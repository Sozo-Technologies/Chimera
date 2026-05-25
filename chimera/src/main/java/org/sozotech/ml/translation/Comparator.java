package org.sozotech.ml.translation;

import org.sozotech.ml.core.NeuralNetwork.PredictionResult;

public class Comparator {

    private float aiThreshold;
    private float markovMinConfidence;
    private final MarkovChain markov;

    public Comparator(float aiThreshold, float markovMinConfidence) {
        this.aiThreshold = aiThreshold;
        this.markovMinConfidence = markovMinConfidence;
        this.markov = new MarkovChain();
    }

    public Comparator() {
        this(0.75f, 0.35f);
    }

    public char compare(PredictionResult ai, char fallback, char prior, float[] aiScores) {
        char candidate = resolve(ai, fallback);
        if (candidate == '?') return '?';

        float[] scores = buildScores(candidate, ai, fallback, aiScores);
        return markov.reweight(prior, scores, markovMinConfidence);
    }

    private char resolve(PredictionResult ai, char fallback) {
        if (ai.letter() == '?' && fallback == '?') return '?';
        if (ai.letter() == '?') return fallback;
        if (fallback == '?') return ai.letter();
        if (ai.confidence() >= aiThreshold) return ai.letter();
        if (ai.letter() == fallback) return ai.letter();
        return fallback;
    }

    private float[] buildScores(char candidate, PredictionResult ai, char fallback, float[] aiScores) {
        float[] scores = new float[26];

        if (aiScores != null && aiScores.length == 26)
            System.arraycopy(aiScores, 0, scores, 0, 26);

        int ci = candidate - 'A';
        if (ci >= 0 && ci < 26) scores[ci] = Math.max(scores[ci], 0.5f);

        if (fallback != '?' && fallback != 0) {
            int fi = fallback - 'A';
            if (fi >= 0 && fi < 26) scores[fi] = Math.max(scores[fi], 0.3f);
        }

        float sum = 0;
        for (float s : scores) sum += s;
        if (sum > 1e-6f)
            for (int i = 0; i < 26; i++) scores[i] /= sum;

        return scores;
    }

    public void setAiThreshold(float threshold) {
        this.aiThreshold = threshold;
    }

    public void setMarkovMinConfidence(float min) {
        this.markovMinConfidence = min;
    }

    public DebugSnapshot debug(PredictionResult ai, char fallback, char prior, float[] aiScores) {
        char candidate = resolve(ai, fallback);
        float[] scores = buildScores(candidate, ai, fallback, aiScores);
        MarkovChain.DebugSnapshot markovDebug = markov.debug(prior, scores);
        return new DebugSnapshot(ai, fallback, candidate, markovDebug, aiThreshold);
    }

    public record DebugSnapshot(PredictionResult ai, char fallback, char preMarkov, MarkovChain.DebugSnapshot markov, float threshold) {
        @Override
        public String toString() {
            return String.format(
                    "[Comparator] AI=%s  Fallback='%s'  PreMarkov='%s'  threshold=%.2f%n%s",
                    ai, fallback, preMarkov, threshold, markov
            );
        }
    }
}