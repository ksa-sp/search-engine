package searchengine.services.indexing.page;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import searchengine.services.indexing.page.abstracts.PageTaskChildTaskController;
import searchengine.services.indexing.site.abstracts.SiteTaskChildTaskController;

import java.net.URI;

/**
 * Pages downloading thread class.
 * <br>
 * Starts child thread of {@link searchengine.services.indexing.index.IndexTask}.
 * <br>
 * Takes links from {@link searchengine.services.indexing.site.SiteTask} thread links queue.
 * <br>
 * Downloads, saves into database and puts pages on this thread pages queue
 * for following parsing by a thread of {@link searchengine.services.indexing.index.IndexTask}.
 */
@RequiredArgsConstructor
public class PageTask extends PageTaskChildTaskController {
    @Getter
    private final SiteTaskChildTaskController siteTask;

    @Override
    protected void compute() {
        try {
            startTask();

            try {
                while (true) {
                    URI uri = siteTask.getLink();

                    if (isStopMessage(uri) || !processLink(uri)) {
                        break;
                    }
                }
            } catch (InterruptedException ignored) {}

            stopTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Whether income link is a special link to exit this thread.
     *
     * @param uri Link to parse.
     *
     * @return true - this thread must stop,
     * <br>false - the link is a regular link.
     */
    private boolean isStopMessage(URI uri) {
        return uri.getPort() == 0 && "0".equals(uri.getHost());
    }
}
