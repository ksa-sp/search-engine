package searchengine.services.search;

import lombok.RequiredArgsConstructor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.springframework.stereotype.Service;

import searchengine.config.ApplicationSettings;
import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;
import searchengine.dto.indexing.LemmaAttributes;
import searchengine.dto.indexing.LemmaOffset;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseError;
import searchengine.dto.search.SearchResponseOk;
import searchengine.model.*;
import searchengine.services.indexing.index.IndexTask;
import searchengine.services.indexing.site.SiteTask;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Search query process service class.
 */
@Service
@RequiredArgsConstructor
public class SearchService {
    private final ApplicationSettings applicationSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    /**
     * Searches most relevant to the query provided pages in database.
     *
     * @param query Query to search.
     * @param siteUrl Site link to search in or null to search in all available sites.
     * @param offset Index of page in search result list which is the first in response.
     * @param limit Search response list size limit.
     *
     * @return {@link searchengine.dto.search.SearchResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.search.SearchResponseError} object on error.
     */
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) {
            return new SearchResponseError(SearchResponse.ERROR_NO_QUERY);
        }

        // Load sites to search in

        List<Site> sites = new ArrayList<>();

        if (siteUrl != null) {
            try {
                siteRepository.findByUrlAndStatus(
                        SiteTask.link2root(siteUrl).toString(),
                        IndexingStatus.INDEXED
                ).ifPresent(sites::add);
            } catch (URISyntaxException ignored) {}
        } else {                // All sites search
            sites.addAll(siteRepository.findAllByStatus(
                    IndexingStatus.INDEXED
            ));
        }

        if (sites.isEmpty()) {
            return new SearchResponseError(SearchResponse.ERROR_SITE_NOT_INDEXED);
        }

        List<Lemma> dbLemmas = searchLemmas(query, sites);
        Map<Integer, Float> pageRanks = calculatePageRanks(dbLemmas);

        return fillResponse(pageRanks, dbLemmas, offset, limit);
    }

    /**
     * Searches lemmas in database optionally filtered by site.
     *
     * @param query Text to split to lemmas.
     * @param sites Sites list to search in.
     *
     * @return List of lemmas found.
     */
    private List<Lemma> searchLemmas(String query, List<Site> sites) {
        Map<String, LemmaAttributes> queryLemmas = IndexTask.getTextLemmas(query);
        List<Lemma> dbLemmas = new ArrayList<>();

        for (String queryLemma : queryLemmas.keySet()) {
            for (Site site : sites) {
                lemmaRepository.findBySiteIdAndLemma(site.getId(), queryLemma).ifPresent(dbLemmas::add);
            }
        }

        return dbLemmas;
    }

    /**
     * Creates rank to page map according to database index information.
     *
     * @param dbLemmas List of search query lemmas, found in the database.
     *
     * @return Rank to page map.
     */
    private Map<Integer, Float> calculatePageRanks(List<Lemma> dbLemmas) {
        Map<Integer, Float> pageRanks = new HashMap<>();

        for (Lemma lemma : dbLemmas) {
            indexRepository.findAllByLemmaId(lemma.getId()).forEach(
                    index -> {
                        if (index.getPageId() != null) {
                            pageRanks.merge(
                                    index.getPageId(),
                                    (float) Math.log10(Math.min(index.getRank(), 10f)) + dbLemmas.size(),
                                    Float::sum
                            );
                        }
                    }
            );
        }

        return pageRanks;
    }

    /**
     * Creates search response.
     *
     * @param pageRanks Rank to page map used to sort pages according to query relevance.
     * @param dbLemmas List of search query lemmas, found in the database.
     * @param offset Index of page in search result list which is the first in response.
     * @param limit Search response list size limit.
     *
     * @return {@link searchengine.dto.search.SearchResponseOk} object.
     */
    private SearchResponseOk fillResponse(Map<Integer, Float> pageRanks, List<Lemma> dbLemmas, int offset, int limit) {
        SearchResponseOk responseOk = new SearchResponseOk();

        Float maxPageRank = pageRanks.values().stream().reduce(0f, Math::max);

        for (Map.Entry<Integer, Float> entry : pageRanks.entrySet().stream().sorted(
                Collections.reverseOrder(
                        Map.Entry.comparingByValue()
                )
        ).collect(Collectors.toList())) {
            Optional<Page> optional = pageRepository.findById(entry.getKey());

            if (optional.isEmpty() || offset-- > 0) {
                continue;
            }
            if (limit-- <= 0) {
                break;
            }

            Page page = optional.get();
            siteRepository.findById(page.getSiteId()).ifPresent(pageSite -> {
                Document document = Jsoup.parse(
                        page.getContent(),
                        pageSite.getUrl() + page.getPath()
                );

                responseOk.add(new SearchDataItem(
                        pageSite.getUrl(),
                        pageSite.getName(),
                        page.getPath(),
                        document.title(),
                        getSnippet(document.text(), dbLemmas),
                        entry.getValue() / maxPageRank
                ));
            });
        }

        return responseOk;
    }

    /**
     * Makes snippet from the text.
     *
     * @param text Source text.
     * @param lemmas List of lemmas to find in the text.
     *
     * @return Snippet - formatted and limited by length text.
     */
    private String getSnippet(String text, List<Lemma> lemmas) {
        Set<String> sentences = new TreeSet<>(Comparator.comparingInt(String::length).reversed());

        sentences.addAll(
                Arrays.asList(text
                        .replaceAll("[!?.]\\h+", "\n")
                        .split("\\v+")
                )
        );

        for (String sentence : sentences) {
            Map<String, LemmaAttributes> sentenceLemmas = IndexTask.getTextLemmas(sentence);
            SortedMap<Integer, LemmaOffset> sentenceLemmaOffsets = new TreeMap<>(Comparator.reverseOrder());

            // Search for lemmas in the sentence

            boolean found = false;

            for (Lemma lemma : lemmas) {
                LemmaAttributes attributes = sentenceLemmas.get(lemma.getLemma());

                if (attributes == null) {           // The lemma not found in the sentence
                    continue;
                }

                for (LemmaOffset offset : attributes.getOffsetList()) {
                    sentenceLemmaOffsets.put(offset.getStart(), offset);
                    found = true;
                }
            }

            if (!found) {                           // No lemma found in the sentence
                continue;                           // Proceed next sentence
            }

            return formatSnippet(sentence, sentenceLemmaOffsets.values());
        }

        // No lemma found anywhere

        return text.substring(0, applicationSettings.getSnippetSize());
    }

    /**
     * Formats text to make snippet.
     *
     * @param text Text to be formatted.
     * @param offsetList Lemmas offset list. Must be sorted in reverse order by start index.
     *
     * @return Snippet - formatted and limited by length text.
     */
    private String formatSnippet(String text, Collection<LemmaOffset> offsetList) {
        int lengthLimit = applicationSettings.getSnippetSize();

        // First lemma start index
        int minStart = offsetList.stream()
                .map(LemmaOffset::getStart)
                .reduce(text.length(), Math::min);

        // Last lemma end index + 1
        int maxEnd = offsetList.stream()
                .map(LemmaOffset::getEnd)
                .reduce(-1, Math::max);

        // Number of chars, cut off the beginning of the text
        int shift = 0;

        if (minStart >= maxEnd) {                       // No lemma in the text
            return text.substring(0, lengthLimit);
        }

        // Length limit

        while (text.length() > lengthLimit && text.matches(".*\\S\\s+\\S.*")) {
            if (shift < minStart && minStart - shift > text.length() + shift - maxEnd) {
                String string = text.replaceAll("^\\s*\\S+\\s+", "");

                if (shift + text.length() - string.length() <= minStart) {
                    shift += text.length() - string.length();
                    text = string;
                } else {
                    text = text.substring(minStart - shift);
                    shift = minStart;
                }
            } else {
                text = text.replaceAll("\\s+\\S+\\s*$", "");
            }
        }

        // Format text

        for (LemmaOffset offset : offsetList) {
            if (offset.getEnd() - shift <= text.length() && offset.getStart() >= shift) {
                text = new StringBuilder()
                        .append(text, 0, offset.getStart() - shift)
                        .append("<b>")
                        .append(text, offset.getStart() - shift, offset.getEnd() - shift)
                        .append("</b>")
                        .append(text.substring(offset.getEnd() - shift))
                        .toString();
            }
        }

        return text;
    }
}
