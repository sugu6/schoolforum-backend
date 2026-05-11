package com.example.schoolforum.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownUtil {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");

    private MarkdownUtil() {
    }

    public static String extractFirstImage(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return null;
        }

        Matcher matcher = IMAGE_PATTERN.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }

        return null;
    }
}
