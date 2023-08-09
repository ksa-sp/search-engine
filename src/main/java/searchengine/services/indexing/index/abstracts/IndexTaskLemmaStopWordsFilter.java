package searchengine.services.indexing.index.abstracts;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * {@link searchengine.services.indexing.index.IndexTask} stop word lemma filter loader abstract class.
 */
public abstract class IndexTaskLemmaStopWordsFilter extends IndexTaskPageLinksParser {
    private static CharArraySet stopWords = null;

    /**
     * Returns stop words set to use in lemmatization process.
     * <br>
     * The returned set contains of apache lucene default stop words sets
     * and stop words of application resources stop_words.txt file.
     *
     * @return Stop words set.
     */
    public static CharArraySet getStopWords() {
        if (stopWords == null) {
            stopWords = StopFilter.makeStopSet(
                    getResourceStopWords(),
                    true
            );
            stopWords.addAll(EnglishAnalyzer.getDefaultStopSet());
            stopWords.addAll(RussianAnalyzer.getDefaultStopSet());
        }
        return stopWords;
    }

    /**
     * Returns array of stop words of application resources stop_words.txt file.
     *
     * @return Stop words as array of strings.
     */
    private static String[] getResourceStopWords() {
        String fileString = getResourceFile("stop_words.txt");

        if (fileString !=  null) {
            return fileString.split("\\s+");
        }

        return new String[] {};
    }

    /**
     * Loads content of application resources text file.
     *
     * @param fileName File name to load.
     *
     * @return Content of the file or null in the case of error.
     */
    public static String getResourceFile(String fileName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        try (
                InputStream inputStream = classLoader.getResourceAsStream(fileName)
        ) {
            if (inputStream != null) {
                try (
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(inputStreamReader)
                ) {
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException ignored) {}

        return null;
    }
}
