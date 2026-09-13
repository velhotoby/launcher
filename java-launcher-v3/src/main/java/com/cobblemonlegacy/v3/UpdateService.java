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
import java.util.concurrent.TimeUnit;
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
                .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.18")
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
                    .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.18")
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
            destination.toFile().setReadable(true, false);
            progress.accept(100);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void launch(Path jar) throws IOException {
        launch(jar, new String[0]);
    }

    private void launch(Path jar, String... startupArguments) throws IOException {
        Path target = jar.toAbsolutePath().normalize();
        if (!Files.isRegularFile(target)) throw new IOException("A nova versão não foi encontrada.");

        Path javaHome = Path.of(System.getProperty("java.home"));
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path javaw = javaHome.resolve("bin").resolve("javaw.exe");
        Path java = windows && Files.isRegularFile(javaw)
                ? javaw : javaHome.resolve("bin").resolve(windows ? "java.exe" : "java");
        if (!Files.isRegularFile(java)) throw new IOException("Java não encontrado para reiniciar o launcher.");

        var command = new java.util.ArrayList<String>();
        if (!windows) {
            Path setsid = executable("/usr/bin/setsid", "/bin/setsid");
            Path nohup = executable("/usr/bin/nohup", "/bin/nohup");
            if (setsid != null) command.add(setsid.toString());
            else if (nohup != null) command.add(nohup.toString());
        }
        command.add(java.toString());
        command.add("-jar");
        command.add(target.toString());
        command.addAll(List.of(startupArguments));

        Path previous = runningJar();
        if (previous != null && !previous.equals(target) && !hasUpdatedFrom(startupArguments)) {
            command.add("--updated-from");
            command.add(previous.toString());
        }

        Process process = new ProcessBuilder(command)
                .directory(target.getParent().toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            if (process.waitFor(1200, TimeUnit.MILLISECONDS)) {
                throw new IOException("A nova versão encerrou imediatamente (código "
                        + process.exitValue() + ").");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("O reinício do launcher foi interrompido.", error);
        }
    }

    static boolean detachCurrent() {
        Path current = runningJar();
        if (current == null) return false;
        try {
            Path previous = newestPreviousBeside(current);
            if (previous == null) {
                new UpdateService().launch(current, "--launcher-detached");
            } else {
                new UpdateService().launch(current, "--launcher-detached", "--updated-from", previous.toString());
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    static void deletePreviousWhenPossible(Path previousJar) {
        Thread cleanup = new Thread(() -> deletePrevious(previousJar), "launcher-update-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();
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

    private static void deletePrevious(Path previousJar) {
        Path previous = previousJar.toAbsolutePath().normalize();
        Path current = runningJar();
        if (!isSafePreviousJar(previous, current)) return;

        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                if (Files.deleteIfExists(previous)) return;
            } catch (IOException ignored) {
                // No Windows o processo anterior pode manter o JAR bloqueado por alguns segundos.
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (Files.exists(previous)) previous.toFile().deleteOnExit();
    }

    private static boolean isSafePreviousJar(Path previous, Path current) {
        try {
            if (current == null || !Files.isRegularFile(previous) || Files.isSameFile(previous, current)) return false;
            if (!previous.getFileName().toString()
                    .matches("(?i)Cobblemon-Legacy-Launcher-[0-9][0-9.]*\\.jar")) return false;
            String previousVersion = packagedVersion(previous);
            String currentVersion = packagedVersion(current);
            return !previousVersion.isBlank() && isNewer(currentVersion, previousVersion);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static Path newestPreviousBeside(Path current) {
        Path selected = null;
        String selectedVersion = "";
        try (var candidates = Files.newDirectoryStream(current.getParent(),
                "Cobblemon-Legacy-Launcher-*.jar")) {
            for (Path candidate : candidates) {
                Path normalized = candidate.toAbsolutePath().normalize();
                if (!isSafePreviousJar(normalized, current)) continue;
                String version = packagedVersion(normalized);
                if (selected == null || isNewer(version, selectedVersion)) {
                    selected = normalized;
                    selectedVersion = version;
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return selected;
    }

    private static String packagedVersion(Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            if (jar.getEntry("com/cobblemonlegacy/v3/LauncherApp.class") == null
                    || jar.getManifest() == null) return "";
            return cleanVersion(jar.getManifest().getMainAttributes().getValue("Implementation-Version"));
        }
    }

    private static Path runningJar() {
        try {
            Path source = Path.of(UpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().normalize();
            return Files.isRegularFile(source) ? source : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path executable(String... candidates) {
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isExecutable(path)) return path;
        }
        return null;
    }

    private static boolean hasUpdatedFrom(String[] arguments) {
        for (String argument : arguments) {
            if ("--updated-from".equals(argument)) return true;
        }
        return false;
    }

    private static Path updateDirectory() {
        Path source = runningJar();
        if (source != null && Files.isWritable(source.getParent())) return source.getParent();
        return Path.of(System.getProperty("user.home"), ".cobblemon_legacy_launcher", "updates")
                .toAbsolutePath().normalize();
    }

    private static String cleanVersion(String value) {
        if (value == null) return "";
        return value.trim().replaceFirst("^[vV]", "");
    }

    private static String text(Object value) { return value instanceof String text ? text : ""; }
}
