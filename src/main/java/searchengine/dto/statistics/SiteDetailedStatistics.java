package searchengine.dto.statistics;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Site statistics data.
 */
@Data
@RequiredArgsConstructor
public class SiteDetailedStatistics {
    private final String url;               // Site url without path
    private final String name;              // Site name from properties
    private final String status;            // Site indexing status
    private final long statusTime;          // Time of start last indexing process
    private String error = null;            // Indexing error or null if no error occurred
    private int pages = 0;                  // Number of indexed pages in the site
    private int lemmas = 0;                 // Number of lemmas found in the site
    private int tasks = 0;                  // Number of concurrent indexing tasks running
}
