package searchengine.services.indexing.page.abstracts;

import searchengine.model.Page;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * {@link searchengine.services.indexing.page.PageTask} page queue implementation abstract class.
 */
public abstract class PageTaskPageQueue extends PageTaskProxy {
    private final Page stopMessage = new Page();     // Force child thread to finish
    private final BlockingDeque<Page> pageQueue = new LinkedBlockingDeque<>(5);

    protected void clearPages() {
        pageQueue.clear();
    }

    /**
     * Add stop page to this object pages queue to exit child thread.
     * <br>
     * Waits if the queue is full.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    protected void addStopPage() throws InterruptedException {
        pageQueue.putFirst(stopMessage);
    }

    /**
     * Add page to this object pages queue.
     * <br>
     * Waits if the queue is full.
     *
     * @param page Page to add.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    protected void addPage(Page page) throws InterruptedException {
        getPageRepository().save(page);
        pageQueue.put(page);
    }

    /**
     * Takes page from this object pages queue.
     * <br>
     * Waits until the queue has an item.
     *
     * @return Page taken.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    public Page getPage() throws InterruptedException {
        return pageQueue.take();
    }
}
