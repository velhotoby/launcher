package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class LogExportService {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long MAX_FILE_BYTES = 128L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 512L * 1024 * 1024;
    private static final Set<String> DIAGNOSTIC_STATES = Set.of(
            ".launcher-trusted-sync-v1.json", ".launcher-performance-v1.json",
            ".launcher-keybinds-v1.json", ".launcher-discovered-mods-v1.json"
    );

    record Result(Path archive, int includedFiles, List<String> skipped) {}
    private record Source(Path file, String entry) {}

    static Result export(Path instance, Path launcherHome, Path destinationDirectory,
                         String launcherVersion) throws IOException {
        Path root = instance.toAbsolutePath().normalize();
        Path internal = launcherHome.toAbsolutePath().normalize();
        Path destination = destinationDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(destination)) throw new IOException("A pasta escolhida não existe.");
        Path archive = uniqueArchive(destination);
        List<Source> sources = new ArrayList<>();
        collectDirectory(root.resolve("logs"), "minecraft/logs", sources);
        collectDirectory(root.resolve("crash-reports"), "minecraft/crash-reports", sources);
        collectDirectory(internal.resolve("logs"), "launcher/logs", sources);
        collectRootDiagnostics(root, sources);
        sources.sort(Comparator.comparing(Source::entry));

        List<String> skipped = new ArrayList<>();
        long total = 0;
        int included = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Source source : sources) {
                long size;
                try { size = Files.size(source.file()); }
                catch (IOException error) {
                    skipped.add(source.entry() + " (não foi possível consultar: " + error.getMessage() + ")");
                    continue;
                }
                if (size > MAX_FILE_BYTES || total + size > MAX_TOTAL_BYTES) {
                    skipped.add(source.entry() + " (limite de tamanho)");
                    continue;
                }
                zip.putNextEntry(new ZipEntry(source.entry()));
                try (InputStream input = Files.newInputStream(source.file())) { input.transferTo(zip); }
                zip.closeEntry();
                total += size;
                included += 1;
            }
            included += collectWindowsDiagnostics(zip, skipped);
            String diagnostic = diagnosticReport(root, launcherVersion, included, skipped);
            zip.putNextEntry(new ZipEntry("diagnostico-do-sistema.txt"));
            zip.write(diagnostic.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (IOException error) {
            Files.deleteIfExists(archive);
            throw error;
        }
        return new Result(archive, included, List.copyOf(skipped));
    }

    private static void collectDirectory(Path directory, String prefix, List<Source> sources) throws IOException {
        if (!Files.isDirectory(directory)) return;
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && isLogFile(file.getFileName().toString())) {
                    String relative = directory.relativize(file).toString().replace('\\', '/');
                    sources.add(new Source(file, prefix + "/" + relative));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFileFailed(Path file, IOException error) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void collectRootDiagnostics(Path root, List<Source> sources) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (var entries = Files.list(root)) {
            entries.filter(Files::isRegularFile).forEach(file -> {
                String name = file.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (DIAGNOSTIC_STATES.contains(name)) {
                    sources.add(new Source(file, "minecraft/estado/" + name));
                } else if ((lower.startsWith("hs_err_pid") || lower.startsWith("replay_pid"))
                        && lower.endsWith(".log")) {
                    sources.add(new Source(file, "minecraft/falhas-java/" + name));
                }
            });
        }
    }

    private static boolean isLogFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".log") || lower.endsWith(".log.gz") || lower.endsWith(".txt")
                || lower.endsWith(".json");
    }

    private static Path uniqueArchive(Path directory) {
        String base = "Cobblemon-Legacy-Logs-" + FILE_TIMESTAMP.format(LocalDateTime.now());
        Path candidate = directory.resolve(base + ".zip");
        int suffix = 2;
        while (Files.exists(candidate)) candidate = directory.resolve(base + "-" + suffix++ + ".zip");
        return candidate;
    }

    private static String diagnosticReport(Path root, String launcherVersion, int included,
                                           List<String> skipped) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        long memoryMB = runtime.maxMemory() / (1024 * 1024);
        List<String> mods = new ArrayList<>();
        Path modsDirectory = root.resolve("mods");
        if (Files.isDirectory(modsDirectory)) {
            try (var entries = Files.list(modsDirectory)) {
                entries.filter(Files::isRegularFile)
                        .map(file -> file.getFileName().toString())
                        .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .forEach(mods::add);
            }
        }
        StringBuilder report = new StringBuilder()
                .append("Cobblemon Legacy - pacote de diagnóstico\n")
                .append("Gerado em: ").append(LocalDateTime.now()).append('\n')
                .append("Launcher: ").append(launcherVersion).append('\n')
                .append("Sistema: ").append(System.getProperty("os.name", "desconhecido")).append(' ')
                .append(System.getProperty("os.version", "")).append(" (")
                .append(System.getProperty("os.arch", "")).append(")\n")
                .append("Java do launcher: ").append(System.getProperty("java.version", "desconhecido")).append(" - ")
                .append(System.getProperty("java.vendor", "desconhecido")).append('\n')
                .append("Processadores lógicos: ").append(runtime.availableProcessors()).append('\n')
                .append("Memória máxima visível ao launcher: ").append(memoryMB).append(" MB\n")
                .append("Arquivos incluídos: ").append(included).append('\n')
                .append("Mods instalados: ").append(mods.size()).append("\n\n")
                .append("Lista de mods (nomes dos arquivos):\n");
        for (String mod : mods) report.append("- ").append(mod).append('\n');
        report.append("\nArquivos ignorados:\n");
        if (skipped.isEmpty()) report.append("- Nenhum\n");
        else for (String item : skipped) report.append("- ").append(item).append('\n');
        report.append("\nPrivacidade: credenciais e a sessão Microsoft não são incluídas neste pacote.\n")
                .append("Logs do Minecraft podem conter nickname, mensagens de chat e endereços de servidores.\n");
        return report.toString();
    }

    private static int collectWindowsDiagnostics(ZipOutputStream zip, List<String> skipped) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return 0;
        int included = 0;
        included += writeCommandDiagnostic(zip, skipped, "windows/code-integrity.txt", List.of(
                windowsTool("wevtutil.exe"), "qe", "Microsoft-Windows-CodeIntegrity/Operational",
                "/q:*[System[(Level=2 or Level=3)]]", "/c:200", "/rd:true", "/f:text"));
        included += writeCommandDiagnostic(zip, skipped, "windows/applocker-msi-script.txt", List.of(
                windowsTool("wevtutil.exe"), "qe", "Microsoft-Windows-AppLocker/MSI and Script",
                "/q:*[System[(Level=2 or Level=3)]]", "/c:200", "/rd:true", "/f:text"));
        included += writeCommandDiagnostic(zip, skipped, "windows/app-control-policies.json", List.of(
                windowsTool("CiTool.exe"), "-lp", "-json"));
        return included;
    }

    private static int writeCommandDiagnostic(ZipOutputStream zip, List<String> skipped,
                                               String entry, List<String> command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process running = process;
            CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
                try (InputStream input = running.getInputStream();
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    input.transferTo(buffer);
                    byte[] bytes = buffer.toByteArray();
                    if (bytes.length <= 4 * 1024 * 1024) return bytes;
                    return java.util.Arrays.copyOf(bytes, 4 * 1024 * 1024);
                } catch (IOException error) {
                    return ("Falha ao ler o diagnóstico: " + error.getMessage()).getBytes(StandardCharsets.UTF_8);
                }
            });
            if (!process.waitFor(12, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                skipped.add(entry + " (tempo limite do Windows excedido)");
                return 0;
            }
            byte[] bytes = output.get(3, TimeUnit.SECONDS);
            if (bytes.length == 0 && process.exitValue() != 0) {
                skipped.add(entry + " (comando retornou " + process.exitValue() + ")");
                return 0;
            }
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(bytes);
            zip.closeEntry();
            return 1;
        } catch (Exception error) {
            if (process != null) process.destroyForcibly();
            skipped.add(entry + " (indisponível: " + error.getClass().getSimpleName() + ")");
            return 0;
        }
    }

    private static String windowsTool(String name) {
        Path systemRoot = Path.of(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"));
        Path tool = systemRoot.resolve("System32").resolve(name);
        return Files.isRegularFile(tool) ? tool.toString() : name;
    }

    private LogExportService() {}
}
