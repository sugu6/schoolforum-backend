package com.example.schoolforum.util;

public final class FileUtil {

    private FileUtil() {
    }

    public static long parseSize(String size) {
        if (size == null || size.isBlank()) {
            return 0;
        }
        size = size.toUpperCase().trim();
        if (size.endsWith("GB")) {
            return Long.parseLong(size.replace("GB", "").trim()) * 1024L * 1024L * 1024L;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "").trim()) * 1024L * 1024L;
        } else if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "").trim()) * 1024L;
        }
        return Long.parseLong(size.trim());
    }
}
