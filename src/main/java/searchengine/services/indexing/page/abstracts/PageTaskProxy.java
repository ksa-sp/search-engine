package searchengine.services.indexing.page.abstracts;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionTemplate;

import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;

import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.indexing.site.abstracts.SiteTaskChildTaskController;

import java.net.URI;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RecursiveAction;

/**
 * {@link searchengine.services.indexing.page.PageTask} to other objects proxy methods, armed at shorting code.
 */
public abstract class PageTaskProxy extends RecursiveAction {
    /**
     * Returns parent to this thread site thread object.
     *
     * @return {@link searchengine.services.indexing.site.SiteTask} object.
     */
    public abstract SiteTaskChildTaskController getSiteTask();

    /**
     * Returns {@link PageRepository} object.
     *
     * @return {@link PageRepository} object.
     */
    public PageRepository getPageRepository() {
        return getSiteTask().getPageRepository();
    }

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
     * UserAgent http request header value.
     *
     * @return UserAgent http request header value or empty string if the property is not configured.
     */
    public String getUserAgent() {
        return getSiteTask().getUserAgent();
    }

    /**
     * Referer http request header value.
     *
     * @return Referer http request header value or empty string if the property is not configured.
     */
    public String getReferer() {
        return getSiteTask().getReferer();
    }

    /**
     * Returns indexing site entity object.
     *
     * @return Site entity object.
     */
    protected Site getIndexingSite() {
        return getSiteTask().getIndexingSite();
    }

    /**
     * Returns indexed site entity object.
     *
     * @return Site entity object.
     */
    protected Site getIndexedSite() {
        return getSiteTask().getIndexedSite();
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
     * Returns indexed site database id.
     *
     * @return Site id.
     */
    public Integer getIndexedSiteId() {
        return getSiteTask().getIndexedSiteId();
    }

    /**
     * Finds page in indexed site, equal to the page provided.
     * <br>
     * Returns page of non error code only.
     *
     * @param page Page to find in indexed site.
     *
     * @return Page found or null if nothing found.
     */
    public Page findIndexedPage(Page page) {
        return getSiteTask().findIndexedPage(page, false);
    }

    /**
     * Returns date and time of the site previous indexing.
     *
     * @return Http header formatted date and time or blank string if there was no previous indexing.
     */
    public String getHttpIndexedTime() {
        return getSiteTask().getHttpIndexedTime();
    }

    /**
     * Add link to the link queue.
     *
     * @param uri Link to add.
     */
    protected void addLink(URI uri) {
        getSiteTask().addLink(uri);
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
     * Delay till connection interval is finished.
     *
     * @return true - shutdown is active,<br>false - continue working
     *
     * @throws InterruptedException Delay is interrupted.
     */
    protected boolean connectionDelay() throws InterruptedException {
        return getSiteTask().connectionDelay();
    }

    /**
     * Activates shutdown process.
     *
     * @param error Error description.
     */
    protected void shutdown(String error) {
        getSiteTask().shutdown(error);
    }

    /**
     * Try to enter string dependant serialization.
     * <br>
     * Returns immediately.
     *
     * @param string String to capture working on.
     *
     * @return Latch object to wait for the string is free,
     * <br>or null if the string is successfully captured.
     */
    protected CountDownLatch tryLockString(String string) {
        return getSiteTask().tryLockString(string);
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
