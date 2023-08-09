package searchengine.services.indexing.index.abstracts;

import searchengine.model.Page;

/**
 * {@link searchengine.services.indexing.index.IndexTask} process pages abstract class.
 */
public abstract class IndexTaskPageHandler extends IndexTaskTextLemmasParser {
    /**
     * Processes {@link Page} object to extract links and text lemmas from.
     *
     * @param page {@link Page} entity object.
     */
    protected void processPage(Page page) {
        switch (page.getCode()) {
            case 200:
                processLemmas(page, processLinks(page));
                break;
        }
        doneJob();
    }
}
