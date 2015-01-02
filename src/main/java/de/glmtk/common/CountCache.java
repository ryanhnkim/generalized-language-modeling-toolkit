package de.glmtk.common;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import static de.glmtk.common.PatternElem.CNT;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.counting.LengthDistribution;
import de.glmtk.util.StringUtils;

/**
 * Tests for this class can be found in {@link de.glmtk.counting.CountingTest}.
 */
public class CountCache {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(CountCache.class);

    private Map<Pattern, Map<String, Long>> absolute =
            new HashMap<Pattern, Map<String, Long>>();

    private Map<Pattern, Map<String, Counter>> continuation =
            new HashMap<Pattern, Map<String, Counter>>();

    private Map<Pattern, long[]> nGramTimes = new HashMap<Pattern, long[]>();

    private LengthDistribution lengthDistribution;

    public CountCache(
            Path countsDir) throws IOException {
        // Allowing workingDir == null to make
        // {@link Patterns#getUsedPatterns(ParamEstimator, ProbMode)} work.
        if (countsDir == null) {
            return;
        }

        LOGGER.info("Loading counts...");
        LOGGER.debug("Loading absolute counts...");
        loadAbsolute(countsDir);
        LOGGER.debug("Loading continuation counts...");
        loadContinuation(countsDir);
        LOGGER.debug("Loading nGramTimes counts...");
        loadNGramTimes(countsDir);
        LOGGER.debug("Loading Sequence Length Distribution...");
        lengthDistribution =
                new LengthDistribution(
                        countsDir
                                .resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME),
                        false);
    }

    private void loadAbsolute(Path countsDir) throws IOException {
        try (DirectoryStream<Path> files =
                Files.newDirectoryStream(countsDir
                        .resolve(Constants.ABSOLUTE_DIR_NAME))) {
            for (Path file : files) {
                Pattern pattern = Patterns.get(file.getFileName().toString());
                Map<String, Long> counts = new HashMap<String, Long>();
                absolute.put(pattern, counts);

                try (BufferedReader reader =
                        Files.newBufferedReader(file, Constants.CHARSET)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        String sequence =
                                Counter.getSequenceAndCounter(line, counter);
                        counts.put(sequence, counter.getOnePlusCount());
                    }
                }
            }
        }
    }

    private void loadContinuation(Path countsDir) throws IOException {
        try (DirectoryStream<Path> files =
                Files.newDirectoryStream(countsDir
                        .resolve(Constants.CONTINUATION_DIR_NAME))) {
            for (Path file : files) {
                Pattern pattern = Patterns.get(file.getFileName().toString());
                Map<String, Counter> counts = new HashMap<String, Counter>();
                continuation.put(pattern, counts);

                try (BufferedReader reader =
                        Files.newBufferedReader(file, Constants.CHARSET)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        String sequence =
                                Counter.getSequenceAndCounter(line, counter);
                        counts.put(sequence, counter);
                    }
                }
            }
        }
    }

    private void loadNGramTimes(Path countsDir) throws IOException {
        Path nGramTimesFile = countsDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        try (BufferedReader reader =
                Files.newBufferedReader(nGramTimesFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.splitAtChar(line, '\t');
                if (split.size() != 5) {
                    throw new IllegalStateException(String.format(
                            "Illegal nGramTimes file: '%s'.", nGramTimesFile));
                }

                Pattern pattern = Patterns.get(split.get(0));
                long[] counts = new long[4];
                for (int i = 0; i != 4; ++i) {
                    counts[i] = Long.parseLong(split.get(i + 1));
                }
                nGramTimes.put(pattern, counts);
            }
        }
    }

    public long getAbsolute(NGram sequence) {
        Map<String, Long> counts = absolute.get(sequence.getPattern());
        if (counts == null) {
            throw new IllegalStateException(String.format(
                    "No absolute counts learned for pattern: '%s'.",
                    sequence.getPattern()));
        }
        Long count = counts.get(sequence.toString());
        return count == null ? 0 : count;
    }

    public Counter getContinuation(NGram sequence) {
        Map<String, Counter> counts = continuation.get(sequence.getPattern());
        if (counts == null) {
            throw new IllegalStateException(String.format(
                    "No continuation counts learned for pattern: '%s'.",
                    sequence.getPattern()));
        }
        Counter counter = counts.get(sequence.toString());
        return counter == null ? new Counter() : counter;
    }

    public long[] getNGramTimes(Pattern pattern) {
        long[] counts = nGramTimes.get(pattern);
        if (counts == null) {
            throw new IllegalStateException(String.format(
                    "No nGramTimes counts learned for pattern: '%s'.", pattern));
        }
        return counts;
    }

    public long getNumWords() {
        return getAbsolute(SKP_NGRAM);
    }

    public long getVocabSize() {
        return getContinuation(WSKP_NGRAM).getOnePlusCount();
    }

    public SortedSet<String> getWords() {
        return new TreeSet<String>(absolute.get(Patterns.get(CNT)).keySet());
    }

    public LengthDistribution getLengthDistribution() {
        return lengthDistribution;
    }

}
