package searchengine.services.indexing.site.abstracts;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link searchengine.services.indexing.site.SiteTask} class job counter implementation.
 */
public abstract class SiteTaskJobCounter extends SiteTaskRobotRules {
    private final AtomicInteger jobCount = new AtomicInteger();

    protected void clearJobCount() {
        jobCount.set(0);
    }

    /**
     * Increments job counter.
     *
     * @return Value of the counter after increment.
     */
    public int startJob() {
        return jobCount.incrementAndGet();
    }

    /**
     * Decrements job counter.
     *
     * @return true - all jobs done.
     */
    public synchronized boolean doneJob() {
        boolean done = jobCount.decrementAndGet() <= 0;

        if (done) {
            notifyAll();
        }

        return done;
    }
}
