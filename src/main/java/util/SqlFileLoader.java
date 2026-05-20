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

    private SqlFileLoader() {
        // Utility class. Δεν θέλουμε instances.
    }

    public static String load(String path) {
        String normalizedPath = normalizePath(path);
        return CACHE.computeIfAbsent(normalizedPath, SqlFileLoader::readSqlFile);
    }

    private static String readSqlFile(String normalizedPath) {
        InputStream inputStream = SqlFileLoader.class
                .getClassLoader()
                .getResourceAsStream(normalizedPath);

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "Δεν βρέθηκε το SQL αρχείο στα resources: " + normalizedPath
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
                    "Αποτυχία φόρτωσης SQL αρχείου: " + normalizedPath,
                    exception
            );
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Το path του SQL αρχείου δεν μπορεί να είναι κενό.");
        }

        return path.startsWith("/") ? path.substring(1) : path;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}