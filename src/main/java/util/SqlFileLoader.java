package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SqlFileLoader {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    // Prevents object creation
    private SqlFileLoader() {
    }

    // Loads SQL file
    public static String load(String path) {
        String normalizedPath = normalizePath(path);
        return CACHE.computeIfAbsent(normalizedPath, SqlFileLoader::readSqlFile);
    }

    // Reads SQL content
    private static String readSqlFile(String normalizedPath) {
        InputStream inputStream = SqlFileLoader.class
                .getClassLoader()
                .getResourceAsStream(normalizedPath);

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "SQL file not found in resources: " + normalizedPath
            );
        }

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                )
        ) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to load SQL file: " + normalizedPath,
                    exception
            );
        }
    }

    // Normalizes file path
    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("SQL file path cannot be empty.");
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    // Clears SQL cache
    public static void clearCache() {
        CACHE.clear();
    }
}