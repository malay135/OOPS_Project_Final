package i_itsm.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How alike two short texts are, from 0 (nothing in common) to 1 (same words).
 * Words of 1-2 letters are ignored. Used by duplicate detection and suggested resolutions.
 */
public final class TextSimilarity {

    private TextSimilarity() {
    }

    public static Set<String> words(String text) {
        return Arrays.stream((text == null ? "" : text).toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
    }

    /** Shared words divided by all distinct words (Jaccard similarity). */
    public static double similarity(String a, String b) {
        Set<String> x = words(a), y = words(b);
        if (x.isEmpty() || y.isEmpty()) return 0;
        Set<String> all = new HashSet<>(x);
        all.addAll(y);
        Set<String> shared = new HashSet<>(x);
        shared.retainAll(y);
        return (double) shared.size() / all.size();
    }
}
