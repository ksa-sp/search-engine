package searchengine.services.indexing.page.abstracts;

import searchengine.services.indexing.index.IndexTask;

import java.util.concurrent.ForkJoinTask;

/**
 * {@link searchengine.services.indexing.page.PageTask} child thread starter.
 */
public abstract class PageTaskChildTaskController extends PageTaskPageDownloader {
    private ForkJoinTask<Void> indexingTask = null;

    /**
     * Starts child thread of {@link IndexTask}.
     *
     * @return true - child thread is successfully started,
     * <br>false - child thread is already run.
     */
    protected boolean startTask() {
        if (indexingTask == null) {
            indexingTask = new IndexTask(getSiteTask(), this).fork();
            return true;
        }
        return false;
    }

    /**
     * Stops child thread and waits for it is finished.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    protected void stopTask() throws InterruptedException {
        if (indexingTask != null) {
            clearPages();
            addStopPage();
            indexingTask.join();
        }
        clearPages();
    }
}
