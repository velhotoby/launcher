package com.cobblemonlegacy.v3;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class InstanceRestoreService {
    private static final String BACKUP_DIRECTORY = ".launcher-restore-backups";
    private static final DateTimeFormatter BACKUP_NAME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> SETTINGS_TO_BACK_UP = List.of(
            "config", "options.txt", "optionsof.txt", ".launcher-discovered-mods-v1.json"
    );
    private static final List<String> REBUILDABLE_PATHS = List.of(
            ".fabric", ".mixin.out", "assets", "bin", "libraries", "runtime", "versions",
            "dynamic-data-pack-cache", "dynamic-resource-pack-cache", ".launcher-mods-staging-v1",
            ".launcher-keybinds-v1.json", ".launcher-performance-v1.json",
            ".launcher-trusted-sync-v1.json", "resourcepacks/Cobblemon Legacy Compat"
    );

    record Result(Path backup, int backedUp, int removed) {}

    static Result restore(Path instance) throws IOException {
        Path root = instance.toAbsolutePath().normalize();
        if (!".cobblemon_legacy".equals(root.getFileName().toString())) {
            throw new IOException("A pasta selecionada não é uma instância do Cobblemon Legacy.");
        }
        Files.createDirectories(root);
        Path backup = uniqueBackup(root.resolve(BACKUP_DIRECTORY));
        int backedUp = 0;
        int removed = 0;

        for (String relative : SETTINGS_TO_BACK_UP) {
            Path source = safeResolve(root, relative);
            if (!Files.exists(source)) continue;
            Path destination = safeResolve(backup, relative);
            Files.createDirectories(destination.getParent());
            try {
                Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(source, destination);
            }
            backedUp += 1;
        }
        for (String relative : REBUILDABLE_PATHS) {
            Path target = safeResolve(root, relative);
            if (!Files.exists(target)) continue;
            deleteRecursively(target);
            removed += 1;
        }
        if (backedUp == 0) Files.deleteIfExists(backup);
        return new Result(backedUp == 0 ? null : backup, backedUp, removed);
    }

    private static Path uniqueBackup(Path parent) throws IOException {
        Files.createDirectories(parent);
        String base = BACKUP_NAME.format(LocalDateTime.now());
        Path candidate = parent.resolve(base);
        int suffix = 2;
        while (Files.exists(candidate)) candidate = parent.resolve(base + "-" + suffix++);
        Files.createDirectories(candidate);
        return candidate;
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Caminho inseguro durante a restauração.");
        return resolved;
    }

    static void deleteRecursively(Path target) throws IOException {
        if (!Files.exists(target)) return;
        if (!Files.isDirectory(target)) {
            Files.delete(target);
            return;
        }
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private InstanceRestoreService() {}
}
