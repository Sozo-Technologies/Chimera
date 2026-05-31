package org.sozotech.ml.translation;

import org.sozotech.ml.core.NeuralNetwork;
import org.sozotech.ml.core.NeuralNetwork.PredictionResult;
import org.sozotech.ml.preprocess.HandData;
import org.sozotech.ml.preprocess.Matrix;
import org.sozotech.ml.preprocess.Normalizer;
import org.sozotech.system.FallbackDetector;

public class TranslationUnit {

    private final NeuralNetwork nn;
    private final FallbackDetector fallback;
    private final Comparator comparator;
    private final LetterHistory history;
    private final Sanitizer sanitizer;
    private final FrameBuffer frameBuffer;

    private boolean debugMode;
    private final StringBuilder sentence;

    public TranslationUnit(int frameCapacity) {
        this.nn = NeuralNetwork.getInstance();
        this.fallback = new FallbackDetector();
        this.comparator = new Comparator();
        this.history = new LetterHistory(32);
        this.sanitizer = new Sanitizer();
        this.frameBuffer = new FrameBuffer(frameCapacity);
        this.debugMode = false;
        this.sentence = new StringBuilder();
    }

    public TranslationUnit() {
        this(20);
    }

    public void feedFrame(HandData hand) {
        if (hand == null || !hand.isPresent()) return;
        frameBuffer.push(hand.landmarks());

        if (!frameBuffer.isFull()) return;

        float[][] resolved = frameBuffer.resolve();
        frameBuffer.clear();

        HandData resolvedHand = new HandData(resolved, hand.world(), hand.visibility());

        PredictionResult aiResult = nn.predictWithConfidence(resolved);
        float[] aiScores = extractScores(resolved);
        char fallbackChar = fallback.detect(resolvedHand);
        char prior = history.last();
        char letter = comparator.compare(aiResult, fallbackChar, prior, aiScores);

        if (debugMode) printDebug(aiResult, fallbackChar, prior, aiScores, letter, resolvedHand);

        if (letter == '?') return;

        history.push(letter);
    }

    public void feedFrame(float[][] rawLandmarks) {
        feedFrame(new HandData(rawLandmarks, rawLandmarks, new float[21]));
    }

    public void commitWord() {
        String raw = history.word();
        if (raw.isBlank()) return;

        String clean = sanitizer.sanitize(raw);

        if (debugMode) System.out.println(sanitizer.debug(raw));

        if (!sentence.isEmpty()) sentence.append(' ');
        sentence.append(clean);
        history.reset();
    }

    public String getSentence() {
        return sentence.toString();
    }

    public String getCurrentWord() {
        return history.word();
    }

    public void clearSentence() {
        sentence.setLength(0);
    }

    public void reset() {
        frameBuffer.clear();
        history.reset();
        sentence.setLength(0);
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setFrameCapacity(int capacity) {
        frameBuffer.setCapacity(capacity);
    }

    public void setAiThreshold(float threshold) {
        comparator.setAiThreshold(threshold);
    }

    public void setMarkovMinConfidence(float min) {
        comparator.setMarkovMinConfidence(min);
    }

    public void addDictionaryWord(String word) {
        sanitizer.addWord(word);
    }

    private float[] extractScores(float[][] resolved) {
        if (!nn.isReady()) return null;
        float[][] normalized = Normalizer.normalize(resolved);
        float[] flat = Normalizer.flattenLandmarks(normalized);
        if (flat.length != 63) return null;
        return nn.getNetwork().forward(flat);
    }

    private void printDebug(PredictionResult ai, char fallbackChar, char prior, float[] aiScores, char letter, HandData hand) {
        System.out.println("─────────────────────────────────────");
        System.out.println(frameBuffer.debug());
        System.out.println(history.debug());
        System.out.println(fallback.debugDetect(hand));
        System.out.println(comparator.debug(ai, fallbackChar, prior, aiScores));
        System.out.printf("[TranslationUnit] output='%s'  word='%s'%n", letter, history.word() + letter);
        System.out.println("─────────────────────────────────────");
    }

    public DebugSnapshot debugSnapshot() {
        return new DebugSnapshot(
                history.word(),
                sentence.toString(),
                frameBuffer.debug(),
                history.debug(),
                debugMode
        );
    }

    public record DebugSnapshot(
            String currentWord,
            String sentence,
            FrameBuffer.DebugSnapshot frameBuffer,
            LetterHistory.DebugSnapshot letterHistory,
            boolean debugMode
    ) {
        @Override
        public String toString() {
            return String.format(
                    "[TranslationUnit]%n  sentence='%s'%n  word='%s'%n  debug=%b%n  %s%n  %s",
                    sentence, currentWord, debugMode, frameBuffer, letterHistory
            );
        }
    }
}