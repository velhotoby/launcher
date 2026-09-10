package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class GamePreferences {
    private static final String OPTIONS_VERSION = "3955";

    private GamePreferences() {}

    static void forceBrazilianPortuguese(Path instance) throws IOException {
        Files.createDirectories(instance);
        Path options = instance.resolve("options.txt");
        List<String> lines = Files.exists(options)
                ? new ArrayList<>(Files.readAllLines(options, StandardCharsets.UTF_8)) : new ArrayList<>();
        boolean languageFound = false;
        boolean versionFound = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("lang:")) {
                lines.set(i, "lang:pt_br");
                languageFound = true;
            } else if (lines.get(i).startsWith("version:")) {
                lines.set(i, "version:" + OPTIONS_VERSION);
                versionFound = true;
            }
        }
        if (!versionFound) lines.add(0, "version:" + OPTIONS_VERSION);
        if (!languageFound) lines.add("lang:pt_br");
        Path temporary = options.resolveSibling("options.txt.launcher.tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, options, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, options, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
