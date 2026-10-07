package com.cobblemonlegacy.windows;

import javax.swing.JOptionPane;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WindowsBootstrap {
    private static final Pattern VERSIONED = Pattern.compile(
            "(?i)^Cobblemon-Legacy-Launcher-([0-9]+(?:\\.[0-9]+)*)\\.jar$");
    private static final String FALLBACK = "Cobblemon-Legacy-Launcher-Windows.jar";

    public static void main(String[] arguments) {
        if (arguments.length == 1 && "--self-test".equals(arguments[0])) {
            Matcher valid = VERSIONED.matcher("Cobblemon-Legacy-Launcher-3.4.32.jar");
            if (!valid.matches()
                    || compareVersions(List.of(3, 4, 32), List.of(3, 4, 31)) <= 0
                    || compareVersions(List.of(3, 4, 32), List.of(3, 4, 32, 0)) != 0) {
                throw new IllegalStateException("Falha no seletor semântico de versões.");
            }
            System.out.println("WINDOWS-BOOTSTRAP OK: versão semântica e fallback validados.");
            return;
        }
        try {
            Path ownJar = Path.of(WindowsBootstrap.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath().normalize();
            Path directory = ownJar.getParent();
            Path launcher = newestLauncher(directory);
            if (launcher == null) {
                throw new IllegalStateException("Nenhuma versão válida do launcher foi encontrada.");
            }
            Path javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
            Path java = Files.isRegularFile(javaw)
                    ? javaw : Path.of(System.getProperty("java.home"), "bin", "java.exe");
            if (!Files.isRegularFile(java)) throw new IllegalStateException("O Java instalado está incompleto.");

            List<String> command = new ArrayList<>();
            command.add(java.toString());
            command.add("-Dfile.encoding=UTF-8");
            command.add("-jar");
            command.add(launcher.toString());
            command.addAll(List.of(arguments));
            new ProcessBuilder(command).directory(directory.toFile()).start();
        } catch (Exception error) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível abrir o Cobblemon Legacy Launcher.\n\n" + rootMessage(error)
                            + "\n\nExecute novamente o instalador oficial.",
                    "Cobblemon Legacy Launcher", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static Path newestLauncher(Path directory) throws Exception {
        record Candidate(Path path, List<Integer> version) {}
        try (var files = Files.list(directory)) {
            Path versioned = files.filter(Files::isRegularFile)
                    .map(path -> {
                        Matcher matcher = VERSIONED.matcher(path.getFileName().toString());
                        if (!matcher.matches()) return null;
                        List<Integer> version = new ArrayList<>();
                        for (String part : matcher.group(1).split("\\.")) version.add(Integer.parseInt(part));
                        return new Candidate(path, version);
                    })
                    .filter(candidate -> candidate != null)
                    .max(Comparator.comparing(Candidate::version, WindowsBootstrap::compareVersions))
                    .map(Candidate::path).orElse(null);
            if (versioned != null) return versioned;
        }
        Path fallback = directory.resolve(FALLBACK);
        return Files.isRegularFile(fallback) ? fallback : null;
    }

    private static int compareVersions(List<Integer> left, List<Integer> right) {
        int length = Math.max(left.size(), right.size());
        for (int index = 0; index < length; index++) {
            int a = index < left.size() ? left.get(index) : 0;
            int b = index < right.size() ? right.get(index) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName().toLowerCase(Locale.ROOT) : message;
    }

    private WindowsBootstrap() {}
}
