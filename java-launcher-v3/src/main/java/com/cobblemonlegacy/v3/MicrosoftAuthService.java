package com.cobblemonlegacy.v3;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftEntitlements;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.data.MsaConstants;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class MicrosoftAuthService {
    private final HttpClient http = MinecraftAuth.createHttpClient("Cobblemon-Legacy-Launcher/3.4.24");
    private final Path authDirectory = Path.of(System.getProperty("user.home"), ".cobblemon_legacy_launcher");
    private final Path stateFile = authDirectory.resolve("microsoft-auth-state.json");
    private final Path accountFile = authDirectory.resolve("microsoft-account.json");

    interface DeviceCodeListener { void ready(String verificationUri, String userCode); }

    record Account(String name, String uuid, String accessToken, String clientToken) {
        Map<String, Object> toJson() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("schema", 2);
            value.put("name", name);
            value.put("uuid", uuid);
            value.put("accessToken", accessToken);
            value.put("clientToken", clientToken);
            value.put("userProperties", Map.of());
            value.put("meta", Map.of("online", true, "type", "msa"));
            return value;
        }
    }

    Path sessionFile() { return accountFile; }

    Account cachedAccount() {
        try { return readAccount(); } catch (Exception ignored) { return null; }
    }

    Account signIn(DeviceCodeListener listener) throws Exception {
        return signIn(listener, () -> false);
    }

    Account signIn(DeviceCodeListener listener, BooleanSupplier cancelled) throws Exception {
        ensureNotCancelled(cancelled);
        Consumer<MsaDeviceCode> callback = code ->
        {
            ensureNotCancelled(cancelled);
            listener.ready(code.getDirectVerificationUri(), code.getUserCode());
        };
        JavaAuthManager manager = JavaAuthManager.create(http).login(DeviceCodeMsaAuthService::new, callback);
        ensureNotCancelled(cancelled);
        return finish(manager, cancelled);
    }

    Account refreshSaved() throws Exception {
        if (!Files.isRegularFile(stateFile)) throw new IOException("A sessão Microsoft precisa ser conectada novamente.");
        JsonObject json = JsonParser.parseString(Files.readString(stateFile, StandardCharsets.UTF_8)).getAsJsonObject();
        JavaAuthManager manager = JavaAuthManager.fromJson(http, json);
        return finish(manager, () -> false);
    }

    boolean probeDeviceCode() throws Exception {
        MsaApplicationConfig config = new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH);
        DeviceCodeMsaAuthService service = new DeviceCodeMsaAuthService(http, config, ignored -> {});
        MsaDeviceCode code = service.requestDeviceCode();
        return code.getVerificationUri() != null && !code.getVerificationUri().isBlank()
                && code.getUserCode() != null && !code.getUserCode().isBlank()
                && code.getDeviceCode() != null && !code.getDeviceCode().isBlank();
    }

    void logout() throws IOException {
        Files.deleteIfExists(accountFile);
        Files.deleteIfExists(stateFile);
    }

    private Account finish(JavaAuthManager manager, BooleanSupplier cancelled) throws Exception {
        manager.getChangeListeners().add(() -> {
            ensureNotCancelled(cancelled);
            try { saveState(manager); } catch (IOException error) { throw new RuntimeException(error); }
        });
        ensureNotCancelled(cancelled);
        MinecraftToken token = manager.getMinecraftToken().getUpToDate();
        ensureNotCancelled(cancelled);
        MinecraftEntitlements entitlements = manager.getMinecraftEntitlements().getUpToDate();
        if (entitlements.getItems().isEmpty()) {
            throw new IOException("Esta conta Microsoft não possui o Minecraft: Java Edition.");
        }
        ensureNotCancelled(cancelled);
        MinecraftProfile profile = manager.getMinecraftProfile().getUpToDate();
        ensureNotCancelled(cancelled);
        Account account = new Account(profile.getName(), profile.getId().toString(), token.getToken(),
                manager.getDeviceId().toString());
        saveState(manager);
        ensureNotCancelled(cancelled);
        saveAccount(account);
        return account;
    }

    private static void ensureNotCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Login Microsoft cancelado.");
        }
    }

    private void saveState(JavaAuthManager manager) throws IOException {
        saveSecure(stateFile, JavaAuthManager.toJson(manager).toString());
    }

    private void saveAccount(Account account) throws IOException {
        saveSecure(accountFile, MiniJson.stringify(account.toJson()));
    }

    private static void saveSecure(Path target, String contents) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, contents, StandardCharsets.UTF_8);
        try { Files.setPosixFilePermissions(temporary, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); }
        catch (UnsupportedOperationException ignored) {}
        try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Account readAccount() throws IOException {
        if (!Files.isRegularFile(accountFile)) throw new IOException("Nenhuma conta Microsoft conectada.");
        Map<String, Object> data;
        try { data = MiniJson.object(Files.readString(accountFile, StandardCharsets.UTF_8)); }
        catch (RuntimeException error) { throw new IOException("Sessão Microsoft inválida.", error); }
        return new Account(required(data, "name"), required(data, "uuid"), required(data, "accessToken"),
                required(data, "clientToken"));
    }

    private static String required(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw new IOException("Sessão Microsoft sem " + key + ".");
        return text;
    }
}
