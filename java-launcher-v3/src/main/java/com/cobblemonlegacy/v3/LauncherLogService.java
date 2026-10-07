package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

final class LauncherLogService {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private final Path directory;
    private final Path current;
    private String lastMessage = "";
    private long lastMessageAt;

    LauncherLogService(Path launcherHome) {
        directory = launcherHome.resolve("logs");
        current = directory.resolve("launcher.log");
    }

    synchronized void log(String type, String message) {
        String safeType = String.valueOf(type == null ? "status" : type).replaceAll("[^a-zA-Z0-9_-]", "");
        String safeMessage = redact(String.valueOf(message == null ? "" : message))
                .replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.isBlank()) return;
        long now = System.currentTimeMillis();
        String key = safeType + "\t" + safeMessage;
        if (key.equals(lastMessage) && now - lastMessageAt < 2_000) return;
        lastMessage = key;
        lastMessageAt = now;
        try {
            Files.createDirectories(directory);
            rotateIfNecessary();
            String line = Instant.ofEpochMilli(now) + " [" + safeType.toUpperCase() + "] " + safeMessage + "\n";
            Files.writeString(current, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Uma falha ao gravar diagnóstico nunca deve impedir o launcher de funcionar.
        }
    }

    Path directory() {
        return directory;
    }

    private void rotateIfNecessary() throws IOException {
        if (!Files.isRegularFile(current) || Files.size(current) < MAX_BYTES) return;
        Path oldest = directory.resolve("launcher.3.log");
        Files.deleteIfExists(oldest);
        Path second = directory.resolve("launcher.2.log");
        Path first = directory.resolve("launcher.1.log");
        if (Files.exists(second)) Files.move(second, oldest, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(first)) Files.move(first, second, StandardCopyOption.REPLACE_EXISTING);
        Files.move(current, first, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String redact(String text) {
        return text
                .replaceAll("(?i)(Bearer\\s+)[a-z0-9._-]+", "$1[oculto]")
                .replaceAll("(?i)((?:access|refresh|client)[_-]?token\\s*[:=]\\s*)[^\\s,;]+", "$1[oculto]");
    }
}
