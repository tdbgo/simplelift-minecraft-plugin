package com.playcity.simplelift.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Converts the legacy Lift material syntax (for example, {@code *BUTTON}) into a safe regex. */
public final class MaterialGlob {

    private MaterialGlob() {
    }

    public static Pattern compile(String glob) {
        if (glob == null || glob.trim().isEmpty()) {
            throw new IllegalArgumentException("Material pattern cannot be blank");
        }

        String normalized = glob.trim().toUpperCase(Locale.ROOT);
        StringBuilder regex = new StringBuilder("^");
        int literalStart = 0;
        for (int index = 0; index < normalized.length(); index++) {
            if (normalized.charAt(index) != '*') {
                continue;
            }
            if (index > literalStart) {
                regex.append(Pattern.quote(normalized.substring(literalStart, index)));
            }
            regex.append(".*");
            literalStart = index + 1;
        }
        if (literalStart < normalized.length()) {
            regex.append(Pattern.quote(normalized.substring(literalStart)));
        }
        return Pattern.compile(regex.append('$').toString());
    }

    public static boolean matches(String glob, String materialName) {
        return compile(glob).matcher(materialName.toUpperCase(Locale.ROOT)).matches();
    }
}
