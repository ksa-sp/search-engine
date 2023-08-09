package searchengine.dto.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Data of a page in the page list of response to the search api request.
 */
@Getter
@RequiredArgsConstructor
public class SearchDataItem {
    private final String site;                  // Site url without path
    private final String siteName;              // Site name from properties
    private final String uri;                   // URI to the page
    private final String title;                 // Title of the page
    private final String snippet;               // Snippet of the page text with lemmas selected
    private final Float relevance;              // <= 1.0
}
