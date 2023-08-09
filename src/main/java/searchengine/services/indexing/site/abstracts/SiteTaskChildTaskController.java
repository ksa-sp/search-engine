package searchengine.services.indexing.site.abstracts;

import searchengine.services.indexing.page.PageTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

/**
 * {@link searchengine.services.indexing.site.SiteTask} child threads starter.
 */
public abstract class SiteTaskChildTaskController extends SiteTaskSiteData {
    private final List<ForkJoinTask<Void>> linkTasks = new ArrayList<>();
    private long nextConnectionTime = 0L;
    private int addTaskCount = 0;

    /**
     * Returns number of child indexing tasks running.
     *
     * @return Number of indexing tasks.
     */
    public int getIndexingTaskCount() {
        return linkTasks.size();
    }

    /**
     * Starts child thread of {@link PageTask}.
     *
     * @return true - child thread is successfully started,
     * <br>false - child thread number limit is achieved.
     */
    private boolean startTask() {
        if (linkTasks.size() < getThreadsPerSite()) {
            linkTasks.add(new PageTask(this).fork());
            return true;
        }
        return false;
    }

    /**
     * Stops child threads and waits for them are finished.
     */
    protected void stopTasks() {
        clearLinks();
        linkTasks.forEach(t -> addStopLink());
        linkTasks.forEach(ForkJoinTask::join);

        linkTasks.clear();
        clearLinks();
        clearJobCount();
    }

    /**
     * Delay till connection interval is due.
     * <br>
     * Starts new child thread if the interval is overdue.
     *
     * @return true - shutdown is active,
     * <br>false - continue working
     *
     * @throws InterruptedException Delay is interrupted.
     */
    public boolean connectionDelay() throws InterruptedException {
        if (isShutdown()) {
            return true;
        }

        synchronized (linkTasks) {
            long delay = nextConnectionTime - System.currentTimeMillis();

            if (delay > 0) {
                addTaskCount = 3;
                Thread.sleep(delay);
            } else if (--addTaskCount <= 0) {
                addTaskCount = 3;
                startTask();
            }

            nextConnectionTime = System.currentTimeMillis() + getConnectionInterval();
        }

        return isShutdown();
    }
}
