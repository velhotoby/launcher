package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.jar.JarFile;

final class UpdateService {
    private static final URI LATEST_RELEASE = URI.create(
            "https://api.github.com/repos/velhotoby/launcher/releases/latest");
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    record Release(String version, String pageUrl, String assetName, URI downloadUrl) {}

    Release latest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.9")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new IOException("O repositório de atualizações não está acessível publicamente.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub respondeu HTTP " + response.statusCode());
        }

        Map<String, Object> root = MiniJson.object(response.body());
        String version = cleanVersion(text(root.get("tag_name")));
        String pageUrl = text(root.get("html_url"));
        if (version.isBlank()) throw new IOException("A versão da publicação não foi informada.");

        Object assetsValue = root.get("assets");
        if (!(assetsValue instanceof List<?> assets)) {
            throw new IOException("A publicação não contém arquivos para atualização.");
        }
        for (Object item : assets) {
            if (!(item instanceof Map<?, ?> asset)) continue;
            String name = text(asset.get("name"));
            String url = text(asset.get("browser_download_url"));
            if (name.matches("(?i)Cobblemon-Legacy-Launcher-[0-9][0-9.]*\\.jar") && !url.isBlank()) {
                URI uri = URI.create(url);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())) {
                    throw new IOException("A publicação aponta para um endereço de download não confiável.");
                }
                return new Release(version, pageUrl, name, uri);
            }
        }
        throw new IOException("A publicação mais recente não contém o JAR do launcher.");
    }

    Path download(Release release, IntConsumer progress) throws Exception {
        Path directory = updateDirectory();
        Files.createDirectories(directory);
        String safeName = Path.of(release.assetName()).getFileName().toString();
        if (!safeName.equals(release.assetName()) || !safeName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IOException("Nome de atualização inválido.");
        }
        Path destination = directory.resolve(safeName).normalize();
        if (!destination.startsWith(directory)) throw new IOException("Destino de atualização inválido.");
        Path temporary = Files.createTempFile(directory, ".launcher-update-", ".part");
        try {
            HttpRequest request = HttpRequest.newBuilder(release.downloadUrl())
                    .timeout(Duration.ofMinutes(5))
                    .header("Accept", "application/octet-stream")
                    .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.9")
                    .GET().build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Falha no download: HTTP " + response.statusCode());
            }
            long total = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            long received = 0;
            try (InputStream input = response.body(); var output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (count == 0) continue;
                    output.write(buffer, 0, count);
                    received += count;
                    if (total > 0) progress.accept((int) Math.min(99, received * 100 / total));
                }
            }
            validateJar(temporary, release.version());
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            progress.accept(100);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void launch(Path jar) throws IOException {
        Path javaHome = Path.of(System.getProperty("java.home"));
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path java = javaHome.resolve("bin").resolve(windows ? "java.exe" : "java");
        if (!Files.isRegularFile(java)) throw new IOException("Java não encontrado para reiniciar o launcher.");
        new ProcessBuilder(java.toString(), "-jar", jar.toString())
                .directory(jar.getParent().toFile()).start();
    }

    static boolean isNewer(String candidate, String current) {
        int[] next = versionParts(candidate);
        int[] installed = versionParts(current);
        int length = Math.max(next.length, installed.length);
        for (int i = 0; i < length; i++) {
            int a = i < next.length ? next[i] : 0;
            int b = i < installed.length ? installed[i] : 0;
            if (a != b) return a > b;
        }
        return false;
    }

    private static int[] versionParts(String version) {
        String clean = cleanVersion(version);
        String[] values = clean.split("\\.");
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            String digits = values[i].replaceFirst("[^0-9].*$", "");
            if (digits.isBlank()) return new int[0];
            try { result[i] = Integer.parseInt(digits); }
            catch (NumberFormatException ignored) { return new int[0]; }
        }
        return result;
    }

    private static void validateJar(Path path, String expectedVersion) throws IOException {
        if (Files.size(path) < 100_000) throw new IOException("O arquivo de atualização está incompleto.");
        try (JarFile jar = new JarFile(path.toFile())) {
            if (jar.getEntry("com/cobblemonlegacy/v3/LauncherApp.class") == null) {
                throw new IOException("O arquivo baixado não é o Cobblemon Legacy Launcher.");
            }
            String packaged = jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            if (!cleanVersion(expectedVersion).equals(cleanVersion(packaged))) {
                throw new IOException("A versão interna do JAR não corresponde à publicação.");
            }
        }
    }

    private static Path updateDirectory() {
        try {
            Path source = Path.of(UpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(source) && Files.isWritable(source.getParent())) return source.getParent();
        } catch (Exception ignored) {}
        return Path.of(System.getProperty("user.home"), ".cobblemon_legacy_launcher", "updates")
                .toAbsolutePath().normalize();
    }

    private static String cleanVersion(String value) {
        if (value == null) return "";
        return value.trim().replaceFirst("^[vV]", "");
    }

    private static String text(Object value) { return value instanceof String text ? text : ""; }
}
