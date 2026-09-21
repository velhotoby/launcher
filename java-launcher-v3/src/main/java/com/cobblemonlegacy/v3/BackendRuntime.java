package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class BackendRuntime {
    private static final String BACKEND_VERSION = "3.4.27-r1";
    private static final String NODE_VERSION = "v24.15.0";
    private final Path launcherRoot = Path.of(System.getProperty("user.home"), ".cobblemon_legacy_launcher");

    record Prepared(Path node, Path backend) {}

    Prepared prepare(Consumer<String> status) throws Exception {
        status.accept("Preparando o núcleo multiplataforma...");
        Path backend = extractBackend();
        Path node = findNode();
        if (node == null) node = installNode(status);
        return new Prepared(node, backend);
    }

    private Path extractBackend() throws Exception {
        Path target = launcherRoot.resolve("v3").resolve("backend").normalize();
        Path marker = target.resolve(".backend-version");
        if (Files.isRegularFile(marker) && BACKEND_VERSION.equals(Files.readString(marker).trim())
                && Files.isRegularFile(target.resolve("v3-backend.js"))) return target.resolve("v3-backend.js");

        if (Files.exists(target)) deleteTree(target);
        Files.createDirectories(target);
        Path source = Path.of(BackendRuntime.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(source)) {
            Path resources = source.resolve("backend");
            if (!Files.isDirectory(resources)) throw new IOException("Recursos internos do launcher não foram encontrados.");
            Files.walkFileTree(resources, new CopyTree(resources, target));
        } else {
            try (JarFile jar = new JarFile(source.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().startsWith("backend/") || entry.isDirectory()) continue;
                    String relative = entry.getName().substring("backend/".length());
                    if (relative.isBlank()) continue;
                    Path output = target.resolve(relative).normalize();
                    if (!output.startsWith(target)) throw new IOException("Recurso interno com caminho inseguro.");
                    Files.createDirectories(output.getParent());
                    try (InputStream input = jar.getInputStream(entry)) { Files.copy(input, output); }
                }
            }
        }
        Files.writeString(marker, BACKEND_VERSION);
        Path entry = target.resolve("v3-backend.js");
        if (!Files.isRegularFile(entry)) throw new IOException("Núcleo v3 não foi empacotado no JAR.");
        return entry;
    }

    private Path findNode() {
        String override = System.getenv("COBBLEMON_NODE");
        if (override != null && !override.isBlank()) {
            Path candidate = Path.of(override).toAbsolutePath().normalize();
            if (works(candidate.toString())) return candidate;
        }
        Path installed = installedNodePath();
        if (Files.isRegularFile(installed) && works(installed.toString())) return installed;
        if (works("node")) return Path.of("node");
        return null;
    }

    private boolean works(String executable) {
        try {
            Process process = new ProcessBuilder(executable, "--version").redirectErrorStream(true).start();
            return process.waitFor() == 0;
        } catch (Exception ignored) { return false; }
    }

    private Path installNode(Consumer<String> status) throws Exception {
        String os = os();
        String arch = arch();
        if (!os.equals("windows") && !os.equals("linux")) {
            throw new IOException("Esta versão automática suporta Node no Linux e Windows.");
        }
        status.accept("Baixando o núcleo compatível com " + (os.equals("windows") ? "Windows" : "Linux") + "...");
        Path runtimeRoot = launcherRoot.resolve("runtime").resolve("node-" + NODE_VERSION + "-" + os + "-" + arch);
        if (Files.exists(runtimeRoot)) deleteTree(runtimeRoot);
        Files.createDirectories(runtimeRoot);
        String platform = os.equals("windows") ? "win-" + arch : "linux-" + arch;
        String extension = os.equals("windows") ? ".zip" : ".tar.xz";
        String baseName = "node-" + NODE_VERSION + "-" + platform;
        URI uri = URI.create("https://nodejs.org/dist/" + NODE_VERSION + "/" + baseName + extension);
        Path archive = Files.createTempFile(launcherRoot, "node-runtime-", extension);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "Cobblemon-Legacy-Launcher/3.4.27").build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(archive));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Falha ao baixar Node.js: HTTP " + response.statusCode());
            }
            if (os.equals("windows")) unzip(archive, runtimeRoot);
            else {
                Process tar = new ProcessBuilder("tar", "-xJf", archive.toString(), "-C", runtimeRoot.toString())
                        .redirectErrorStream(true).start();
                if (tar.waitFor() != 0) throw new IOException("Não foi possível extrair o Node.js. Instale o pacote xz/tar.");
            }
        } finally { Files.deleteIfExists(archive); }
        Path node = installedNodePath();
        if (!Files.isRegularFile(node)) throw new IOException("O executável do Node.js não foi encontrado após a instalação.");
        if (!os.equals("windows")) node.toFile().setExecutable(true, true);
        return node;
    }

    private Path installedNodePath() {
        String os = os();
        String arch = arch();
        Path root = launcherRoot.resolve("runtime").resolve("node-" + NODE_VERSION + "-" + os + "-" + arch);
        String platform = os.equals("windows") ? "win-" + arch : "linux-" + arch;
        Path distribution = root.resolve("node-" + NODE_VERSION + "-" + platform);
        return os.equals("windows") ? distribution.resolve("node.exe") : distribution.resolve("bin").resolve("node");
    }

    private static void unzip(Path archive, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination)) throw new IOException("Arquivo Node.js contém caminho inseguro.");
                if (entry.isDirectory()) Files.createDirectories(output);
                else { Files.createDirectories(output.getParent()); Files.copy(zip, output); }
                zip.closeEntry();
            }
        }
    }

    private static String os() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.contains("win")) return "windows";
        if (name.contains("linux")) return "linux";
        return "other";
    }

    private static String arch() {
        String value = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return value.contains("aarch64") || value.contains("arm64") ? "arm64" : "x64";
    }

    private static void deleteTree(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class CopyTree extends SimpleFileVisitor<Path> {
        private final Path source;
        private final Path destination;
        private CopyTree(Path source, Path destination) { this.source = source; this.destination = destination; }
        @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Files.createDirectories(destination.resolve(source.relativize(dir))); return FileVisitResult.CONTINUE;
        }
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, destination.resolve(source.relativize(file))); return FileVisitResult.CONTINUE;
        }
    }
}
