package searchengine.services.indexing.index;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import searchengine.model.Page;

import searchengine.services.indexing.index.abstracts.IndexTaskPageHandler;
import searchengine.services.indexing.page.abstracts.PageTaskChildTaskController;
import searchengine.services.indexing.site.abstracts.SiteTaskChildTaskController;

/**
 * Pages indexing thread class.
 * <br>
 * Takes pages from {@link searchengine.services.indexing.page.PageTask} thread pages queue.
 * <br>
 * Parses pages for links and puts the links on {@link searchengine.services.indexing.site.SiteTask} links queue
 * for following downloading by a thread of {@link searchengine.services.indexing.page.PageTask}.
 * <br>
 * Parses pages text for lemmas and saves indexing data into database.
 */
@RequiredArgsConstructor
public class IndexTask extends IndexTaskPageHandler {
    @Getter
    private final SiteTaskChildTaskController siteTask;
    private final PageTaskChildTaskController pageTask;

    @Override
    protected void compute() {
        try {
            while (true) {
                Page page = pageTask.getPage();

                if (isStopMessage(page)) {
                    break;
                }

                processPage(page);
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Whether income page is a special page object to exit this thread.
     *
     * @param page Page to parse.
     *
     * @return true - this thread must stop,
     * <br>false - the page is a regular page.
     */
    private boolean isStopMessage(Page page) {
        return page.getContent() == null;
    }
}
