package searchengine.services.indexing.site.abstracts;

import lombok.Getter;
import lombok.Setter;

import org.springframework.transaction.support.TransactionTemplate;

import searchengine.config.SiteSettings;

import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;

import searchengine.services.indexing.IndexingService;

import java.util.concurrent.RecursiveAction;

/**
 * {@link searchengine.services.indexing.site.SiteTask} to other classes proxy methods, armed at shorting code.
 */
public abstract class SiteTaskProxy extends RecursiveAction {
    public abstract IndexingService getService();
    public abstract SiteSettings getSiteSettings();

    /**
     * Returns {@link SiteRepository} object.
     *
     * @return {@link SiteRepository} object.
     */
    public SiteRepository getSiteRepository() {
        return getService().getSiteRepository();
    }

    /**
     * Returns {@link PageRepository} object.
     *
     * @return {@link PageRepository} object.
     */
    public PageRepository getPageRepository() {
        return getService().getPageRepository();
    }

    /**
     * Returns {@link LemmaRepository} object.
     *
     * @return {@link LemmaRepository} object.
     */
    public LemmaRepository getLemmaRepository() {
        return getService().getLemmaRepository();
    }

    /**
     * Returns {@link IndexRepository} object.
     *
     * @return {@link IndexRepository} object.
     */
    public IndexRepository getIndexRepository() {
        return getService().getIndexRepository();
    }

    /**
     * UserAgent http request header value.
     *
     * @return UserAgent http request header value or empty string if the property is not configured.
     */
    public String getUserAgent() {
        return getSiteSettings().getUserAgent();
    }

    /**
     * Referer http request header value.
     *
     * @return Referer http request header value or empty string if the property is not configured.
     */
    public String getReferer() {
        return getSiteSettings().getReferer();
    }

    /**
     * Whether to avoid loading not changed pages.
     *
     * @return true - check pages for changing after previous indexing.
     */
    public boolean isUpdate() {
        return getSiteSettings().isUpdate();
    }

    /**
     * Minimum time interval between http requests to the site.
     *
     * @return Time interval in ms or 1000 if the property is not configured.
     */
    public long getConnectionInterval() {
        return getSiteSettings().getConnectionInterval();
    }

    /**
     * Maximum number of concurrent threads indexing the site.
     *
     * @return Maximum number of concurrent threads indexing the site or 1 if the property is not configured.
     */
    public int getThreadsPerSite() {
        return getSiteSettings().getTasksPerSite();
    }

    /**
     * Whether to ignore robots.txt file rules in the root of the site.
     *
     * @return true - Do not follow rules of robots.txt file in the root of the site.
     */
    public boolean isIgnoreRobotRules() {
        return getSiteSettings().isIgnoreRobotRules();
    }

    // Static methods

    @Setter
    @Getter
    private static TransactionTemplate transactionTemplate = null;
}
