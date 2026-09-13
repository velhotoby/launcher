package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

final class LocalNicknameStore {
    private static final Pattern VALID_NICKNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private final Path file;

    LocalNicknameStore(Path file) {
        this.file = file;
    }

    String load() throws IOException {
        if (!Files.isRegularFile(file)) return null;
        String nickname = Files.readString(file, StandardCharsets.UTF_8).trim();
        return VALID_NICKNAME.matcher(nickname).matches() ? nickname : null;
    }

    void save(String nickname) throws IOException {
        if (!VALID_NICKNAME.matcher(nickname).matches()) {
            throw new IllegalArgumentException("Nickname local inválido.");
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), ".nickname-", ".tmp");
        try {
            Files.writeString(temporary, nickname + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    void forget() throws IOException {
        Files.deleteIfExists(file);
    }
}
