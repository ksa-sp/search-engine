package searchengine.services.indexing.site.abstracts;

import lombok.Getter;

/**
 * {@link searchengine.services.indexing.site.SiteTask} indexing process shutdown functionality.
 */
public abstract class SiteTaskShutdown extends SiteTaskStringLatch {
    @Getter
    private boolean isShutdown = false;
    @Getter
    private String shutdownError = null;

    /**
     * Activates shutdown process.
     *
     * @param error Error description.
     */
    public synchronized void shutdown(String error) {
        isShutdown = true;
        shutdownError = error;
        notifyAll();
    }
}
