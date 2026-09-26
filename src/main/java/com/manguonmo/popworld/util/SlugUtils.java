package com.manguonmo.popworld.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "san-pham";
        }
        String text = input.trim();
        text = text.replace('đ', 'd').replace('Đ', 'd');
        String nowhitespace = WHITESPACE.matcher(text).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = MULTI_HYPHEN.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "san-pham" : slug;
    }
}
