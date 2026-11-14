package com.zcshou.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtils {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private VersionUtils() {
    }

    public static String extractVersion(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
    }

    public static int compareVersions(String left, String right) {
        String leftVersion = extractVersion(left);
        String rightVersion = extractVersion(right);
        if (leftVersion == null || rightVersion == null) {
            throw new IllegalArgumentException("Invalid version");
        }
        String[] leftParts = leftVersion.split("\\.");
        String[] rightParts = rightVersion.split("\\.");
        for (int i = 0; i < 3; i++) {
            int diff = Integer.parseInt(leftParts[i]) - Integer.parseInt(rightParts[i]);
            if (diff != 0) {
                return diff > 0 ? 1 : -1;
            }
        }
        return 0;
    }

    public static boolean isNewerVersion(String latest, String current) {
        if (extractVersion(latest) == null || extractVersion(current) == null) {
            return false;
        }
        return compareVersions(latest, current) > 0;
    }
}
