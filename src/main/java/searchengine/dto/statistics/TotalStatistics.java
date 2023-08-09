package searchengine.dto.statistics;

import lombok.Data;

/**
 * Application statistics data.
 * <br>
 * All counts of the object initiated with zero.
 */
@Data
public class TotalStatistics {
    private int sites = 0;                  // Number of sites in the database
    private int pages = 0;                  // Number of indexed pages in the database
    private int lemmas = 0;                 // Number of lemmas in the database
    private boolean indexing;               // Whether indexing process run
    private int tasks = 0;                  // Number of indexing tasks running

    /**
     * Increments site number with the value provided.
     *
     * @param count Additional number of sites.
     */
    public void addSites(int count) {
        sites += count;
    }

    /**
     * Increments page number with the value provided.
     *
     * @param count Additional number of pages.
     */
    public void addPages(int count) {
        pages += count;
    }

    /**
     * Increments lemma number with the value provided.
     *
     * @param count Additional number of lemmas.
     */
    public void addLemmas(int count) {
        lemmas += count;
    }

    /**
     * Increments indexing thread number with the value provided.
     *
     * @param count Additional number of threads.
     */
    public void addTasks(int count) {
        tasks += count;
    }
}
