package searchengine.services.indexing.index.abstracts;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.classic.ClassicAnalyzer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterIterator;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import org.tartarus.snowball.ext.EnglishStemmer;
import org.tartarus.snowball.ext.RussianStemmer;

import searchengine.dto.indexing.LemmaAttributes;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * {@link searchengine.services.indexing.index.IndexTask} lemmatization methods abstract class.
 */
public abstract class IndexTaskTextLemmasParser extends IndexTaskLemmaStopWordsFilter {
    /**
     * Indexing a page - extracts lemmas from {@link Page} object text.
     * <br>
     * Updates lemma and index tables.
     *
     * @param page Page entity object.
     * @param text Plain text of the page object.
     */
    protected void processLemmas(Page page, String text) {
        Map<String, LemmaAttributes> lemmaStrings = getTextLemmas(text);

        List<Lemma> updateLemmas = new ArrayList<>();
        List<Index> createIndexes = new ArrayList<>();

        try {
            for (String lemmaString : lemmaStrings.keySet()) {
                if (isShutdown()) {
                    return;
                }

                lockString(lemmaString);

                try {
                    Lemma lemma = getLemmaRepository().findBySiteIdAndLemma(
                            getIndexingSiteId(),
                            lemmaString
                    ).orElse(new Lemma());

                    if (lemma.getId() == null) {            // New lemma
                        lemma.setSiteId(page.getSiteId());
                        lemma.setLemma(lemmaString);

                        getLemmaRepository().save(lemma);
                    }

                    Index index = new Index();
                    index.setPageId(page.getId());
                    index.setLemmaId(lemma.getId());
                    index.setRank(lemmaStrings.get(lemmaString).getRank());

                    createIndexes.add(index);

                    if (lemma.getFrequency() != 0) {
                        lemma.setFrequency(0);              // Mark to update the record at finish of site indexing
                        updateLemmas.add(lemma);
                    }
                } finally {
                    unlockString(lemmaString);
                }
            }

            for (int i = 0; i < updateLemmas.size(); i += 1000) {
                if (isShutdown()) {
                    return;
                }

                List<Lemma> lemmas = updateLemmas.subList(i, Math.min(updateLemmas.size(), i + 1000));

                getTransactionTemplate().execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                lemmas.forEach(lemma -> getLemmaRepository().save(lemma));
                            }
                        }
                );
            }

            for (int i = 0; i < createIndexes.size(); i += 1000) {
                if (isShutdown()) {
                    return;
                }

                List<Index> indexes = createIndexes.subList(i, Math.min(createIndexes.size(), i + 1000));

                getTransactionTemplate().execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                indexes.forEach(index -> getIndexRepository().save(index));
                            }
                        }
                );
            }
        } catch (InterruptedException ignored) {}
    }

    // Static methods

    /**
     * Extracts lemmas from text.
     *
     * @param text Text to extract lemmas from.
     *
     * @return Map of lemmas to their attributes.
     */
    public static Map<String, LemmaAttributes> getTextLemmas(String text) {
        Map<String, LemmaAttributes> index = new HashMap<>();
        CharArraySet stopWords = getStopWords();

        try (
                Analyzer analyzer = new ClassicAnalyzer();
                TokenStream tokenStream = analyzer.tokenStream(null, text);
                TokenStream wordStream = new WordDelimiterFilter(
                        tokenStream,
                        WordDelimiterIterator.DEFAULT_WORD_DELIM_TABLE,
                        0,
                        null
                );
                TokenStream stopStream = new StopFilter(wordStream, stopWords);

                TokenStream engStream = new SnowballFilter(stopStream, new EnglishStemmer());
                TokenStream rusStream = new SnowballFilter(engStream, new RussianStemmer());

                // Remove useless numbers

                TokenStream replaceStream01 = new PatternReplaceFilter(
                        rusStream,
                        Pattern.compile("\\d{5,}"),
                        "",
                        true
                );
                TokenStream replaceStream02 = new PatternReplaceFilter(
                        replaceStream01,
                        Pattern.compile("^\\d{1,3}$"),
                        "",
                        true
                );

                // Length limit

                TokenStream stream = new LengthFilter(replaceStream02, 2, 255)
        ) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);

            stream.reset();
            while (stream.incrementToken()) {
                index.compute(term.toString(), (k, v) -> v == null
                        ? new LemmaAttributes(offset)
                        : v.add(offset)
                );
            }
            stream.end();
        } catch (IOException ignored) {}

        return index;
    }
}
