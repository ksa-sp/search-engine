package searchengine.services.indexing.index.abstracts;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;

import org.springframework.transaction.support.TransactionTemplate;

import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;

import searchengine.services.indexing.site.abstracts.SiteTaskChildTaskController;

import java.net.URI;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * {@link searchengine.services.indexing.index.IndexTask} to other objects proxy methods, armed at shorting code.
 */
public abstract class IndexTaskProxy extends RecursiveAction {
    /**
     * Returns parent to this thread site thread object.
     *
     * @return {@link searchengine.services.indexing.site.SiteTask} object.
     */
    public abstract SiteTaskChildTaskController getSiteTask();

    /**
     * Returns {@link LemmaRepository} object.
     *
     * @return {@link LemmaRepository} object.
     */
    public LemmaRepository getLemmaRepository() {
        return getSiteTask().getLemmaRepository();
    }

    /**
     * Returns {@link IndexRepository} object.
     *
     * @return {@link IndexRepository} object.
     */
    public IndexRepository getIndexRepository() {
        return getSiteTask().getIndexRepository();
    }

    /**
     * Returns indexing site database id.
     *
     * @return Site id.
     */
    protected Integer getIndexingSiteId() {
        return getSiteTask().getIndexingSiteId();
    }

    /**
     * Compiles URL from current site root link and absolute path provided.
     *
     * @param path Path to add to site root URI.
     *
     * @return Full link.
     */
    protected String baseUrl(String path) {
        return getSiteTask().baseUrl(path);
    }

    /**
     * Add all links of the list to the link queue.
     *
     * @param uris List of links to add.
     */
    protected void addLink(List<URI> uris) {
        getSiteTask().addLink(uris);
    }

    /**
     * Decrements job counter.
     *
     * @return true - all jobs done.
     */
    protected boolean doneJob() {
        return getSiteTask().doneJob();
    }

    /**
     * Whether shutdown process is activated.
     *
     * @return true - shutdown is active.
     */
    protected boolean isShutdown() {
        return getSiteTask().isShutdown();
    }

    /**
     * Enter string dependant serialization.
     * <br>
     * Waits for the string is free to work on.
     *
     * @param string String to capture working on.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    protected void lockString(String string) throws InterruptedException {
        getSiteTask().lockString(string);
    }

    /**
     * Exit from string dependant serialization.
     * <br>
     * Free the string to work on it by anther thread.
     *
     * @param string String to free.
     */
    protected void unlockString(String string) {
        getSiteTask().unlockString(string);
    }

    /**
     * Returns this application {@link Logger} object.
     *
     * @return {@link Logger} object.
     */
    protected Logger getLogger() {
        return getSiteTask().getLogger();
    }

    // Static methods

    @Setter
    @Getter
    private static TransactionTemplate transactionTemplate = null;
}
