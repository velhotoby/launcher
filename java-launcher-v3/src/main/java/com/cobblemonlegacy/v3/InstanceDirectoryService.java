package com.cobblemonlegacy.v3;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class InstanceDirectoryService {
    private InstanceDirectoryService() {}

    static Path path(Path userHome) {
        return path(userHome, System.getProperty("os.name", ""), System.getenv("APPDATA"));
    }

    static Path path(Path userHome, String osName, String appData) {
        Path home = userHome.toAbsolutePath().normalize();
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("win")) {
            Path roaming = appData != null && !appData.isBlank()
                    ? Path.of(appData).toAbsolutePath().normalize()
                    : home.resolve("AppData").resolve("Roaming");
            return roaming.resolve(".cobblemon_legacy");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return home.resolve("Library").resolve("Application Support").resolve("cobblemon_legacy");
        }
        return home.resolve(".cobblemon_legacy");
    }

    static boolean isExpectedInstance(Path folder, Path userHome) {
        Path expected = path(userHome);
        return folder.toAbsolutePath().normalize().equals(expected)
                && expected.getParent() != null
                && expected.getParent().getParent() != null;
    }

    static Path ensureDirectory(Path userHome) throws IOException {
        return ensureDirectory(userHome, System.getProperty("os.name", ""), System.getenv("APPDATA"));
    }

    static Path ensureDirectory(Path userHome, String osName, String appData) throws IOException {
        Path folder = path(userHome, osName, appData);
        Files.createDirectories(folder);
        return folder;
    }

    static void open(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) throw new IOException("A pasta do Minecraft não existe: " + folder);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(folder.toFile());
                return;
            }
        } catch (Exception ignored) {
            // Alguns desktops Linux não implementam Desktop.OPEN; tenta os comandos do sistema.
        }

        for (String[] command : fallbackCommands(folder, System.getProperty("os.name", ""))) {
            try {
                Process process = new ProcessBuilder(command)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                if (!process.waitFor(1500, TimeUnit.MILLISECONDS) || process.exitValue() == 0) return;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("A abertura da pasta foi interrompida.", error);
            } catch (IOException ignored) {
                // Tenta o próximo gerenciador de arquivos disponível.
            }
        }
        throw new IOException("Não foi possível abrir a pasta no gerenciador de arquivos: " + folder);
    }

    static String[][] fallbackCommands(Path folder, String osName) {
        String os = osName.toLowerCase(Locale.ROOT);
        return os.startsWith("win")
                ? new String[][]{{"explorer.exe", folder.toString()}}
                : os.contains("mac") || os.contains("darwin")
                    ? new String[][]{{"open", folder.toString()}}
                    : new String[][]{{"xdg-open", folder.toString()}, {"gio", "open", folder.toString()}};
    }
}
