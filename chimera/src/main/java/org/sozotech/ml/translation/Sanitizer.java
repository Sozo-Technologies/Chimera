package org.sozotech.ml.translation;

import java.util.List;
import java.util.ArrayList;

public class Sanitizer {

    private final int maxDistance;
    private final List<String> dictionary;

    public Sanitizer(int maxDistance) {
        this.maxDistance = maxDistance;
        this.dictionary = buildDictionary();
    }

    public Sanitizer() {
        this(2);
    }

    public String sanitize(String word) {
        if (word == null || word.isBlank()) return word;

        String upper = word.toUpperCase();

        if (dictionary.contains(upper)) return upper;

        String best = upper;
        int bestDist = Integer.MAX_VALUE;

        for (String entry : dictionary) {
            if (Math.abs(entry.length() - upper.length()) > maxDistance) continue;
            int dist = levenshtein(upper, entry);
            if (dist < bestDist) {
                bestDist = dist;
                best = entry;
            }
        }

        return bestDist <= maxDistance ? best : upper;
    }

    private int levenshtein(String a, String b) {
        int la = a.length();
        int lb = b.length();
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];

        for (int j = 0; j <= lb; j++) prev[j] = j;

        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[lb];
    }

    public void addWord(String word) {
        if (word != null && !word.isBlank()) dictionary.add(word.toUpperCase());
    }

    public void setMaxDistance(int distance) {}

    private List<String> buildDictionary() {
        List<String> d = new ArrayList<>();
        String[] words = {
                "HELLO","WORLD","HELP","PLEASE","THANK","YOU","YES","NO","GOOD","BAD",
                "SORRY","OKAY","NAME","WHAT","WHERE","WHEN","WHO","HOW","THE","AND",
                "ARE","FOR","THIS","THAT","WITH","HAVE","FROM","THEY","WILL","YOUR",
                "CAN","BUT","NOT","ALL","WAS","ONE","HER","HIM","HIS","SHE",
                "LOVE","LIKE","NEED","WANT","KNOW","COME","GIVE","TAKE","MAKE","SEE",
                "TIME","YEAR","PEOPLE","WAY","DAY","MAN","WOMAN","CHILD","WORK","LIFE",
                "HAND","SIGN","LETTER","WORD","LEARN","UNDERSTAND","SPEAK","HEAR","FEEL","THINK",
                "HOME","SCHOOL","FOOD","WATER","HOUSE","FRIEND","FAMILY","MOTHER","FATHER","BROTHER",
                "SISTER","BABY","BOY","GIRL","OLD","NEW","BIG","SMALL","FAST","SLOW",
                "HOT","COLD","OPEN","CLOSE","START","STOP","MORE","LESS","SAME","DIFFERENT"
        };
        for (String w : words) d.add(w);
        return d;
    }

    public DebugSnapshot debug(String raw) {
        String result = sanitize(raw);
        int dist = result.equals(raw.toUpperCase()) ? 0 : levenshtein(raw.toUpperCase(), result);
        return new DebugSnapshot(raw, result, dist, maxDistance);
    }

    public record DebugSnapshot(String input, String output, int distance, int maxAllowed) {
        @Override
        public String toString() {
            return String.format("[Sanitizer] '%s' → '%s' (dist=%d, max=%d)", input, output, distance, maxAllowed);
        }
    }
}