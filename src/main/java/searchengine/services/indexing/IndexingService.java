package searchengine.services.indexing;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import searchengine.config.ApplicationSettings;
import searchengine.config.SiteSettings;

import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;

import searchengine.dto.indexing.*;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import searchengine.services.indexing.site.SiteTask;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * Indexing process control service.
 */
@Service
@RequiredArgsConstructor
public class IndexingService implements AutoCloseable {
    public static final String USER_SHUTDOWN_ERROR = "Индексация остановлена пользователем";

    private final ApplicationSettings applicationSettings;

    @Getter
    private final SiteRepository siteRepository;
    @Getter
    private final PageRepository pageRepository;
    @Getter
    private final LemmaRepository lemmaRepository;
    @Getter
    private final IndexRepository indexRepository;

    private ForkJoinPool taskPool;

    /**
     * Site root url to site task map.
     */
    private Map<String, SiteTask> taskList = new HashMap<>();

    /**
     * Initializes and cleans the database.
     */
    @EventListener(ApplicationReadyEvent.class)
    private void initDatabase() {
        Page.setOnDeleteCascade();
        Lemma.setOnDeleteCascade();
        Index.setOnDeleteCascade();

        Index.clean();
        Lemma.delete(null);
        Page.delete(null);
    }

    /**
     * Indexing process status.
     *
     * @return true if the process is active.
     */
    public boolean isIndexing() {
        return isIndexing(null);
    }

    /**
     * Site indexing process status.
     *
     * @param siteUrl Site link to check indexing status of or null to return true on any site is indexing.
     *
     * @return true if the process is active.
     */
    private synchronized boolean isIndexing(String siteUrl) {
        if (taskPool == null) {
            taskPool = new ForkJoinPool(applicationSettings.countIndexingThreads());
        }

        if (siteUrl == null) {
            for (SiteTask siteTask : taskList.values()) {
                if (!siteTask.isDone()) {
                    return true;
                }
            }

            taskList.clear();
        } else {
            try {
                String root = SiteTask.link2root(siteUrl).toString();

                if (taskList.containsKey(root) && !taskList.get(root).isDone()) {
                    return true;
                }

                taskList.remove(root);
            } catch (URISyntaxException ignored) {}
        }

        return false;
    }

    /**
     * Returns number of indexing tasks running for the site of the root URL provided.
     *
     * @param url Root URL of the site.
     *
     * @return Number of indexing tasks.
     */
    public int getSiteIndexingTaskCount(String url) {
        SiteTask task = taskList.get(url);

        if (task != null) {
            return task.getIndexingTaskCount();
        }

        return 0;
    }

    /**
     * Waits for site task to finish.
     *
     * @param siteUrl Site link to wait for indexing is done or null to wait for every indexing process is done.
     */
    private synchronized void join(String siteUrl) {
        if (siteUrl == null) {
            taskList.values().forEach(SiteTask::join);
            taskList.clear();
        } else {
            try {
                String root = SiteTask.link2root(siteUrl).toString();

                if (taskList.containsKey(root)) {
                    taskList.get(root).join();
                    taskList.remove(root);
                }
            } catch (URISyntaxException ignored) {}
        }
    }

    /**
     * Activates user coursed site indexing shutdown.
     *
     * @param siteUrl Site link to shut down indexing process or null to shut down all indexing processes.
     */
    private void shutdown(String siteUrl) {
        if (siteUrl == null) {
            taskList.values().forEach(t -> t.shutdown(USER_SHUTDOWN_ERROR));
        } else {
            try {
                String root = SiteTask.link2root(siteUrl).toString();

                if (taskList.containsKey(root)) {
                    taskList.get(root).shutdown(USER_SHUTDOWN_ERROR);
                }
            } catch (URISyntaxException ignored) {}
        }
    }

    /**
     * Shutdown indexing process and wait it for finish.
     *
     * @throws InterruptedException Thread was interrupted.
     */
    @Override
    public void close() throws InterruptedException {
        close(null);
    }

    /**
     * Shutdown site indexing process and wait it for finish.
     *
     * @param siteUrl Site link to shut down indexing process or null to shut down all indexing processes.
     *
     * @throws InterruptedException Thread was interrupted.
     */
    private void close(String siteUrl) throws InterruptedException {
        shutdown(siteUrl);
        join(siteUrl);
    }

    /**
     * Start indexing a site.
     *
     * @param siteSettings Site to index.
     *
     * @return true - new indexing task started.
     *
     * @throws URISyntaxException Site start indexing link is broken.
     */
    private boolean startSite(SiteSettings siteSettings) throws URISyntaxException {
        return startSite(siteSettings, null);
    }

    /**
     * Start indexing a page of a site or all sate.
     *
     * @param siteSettings Site to index.
     * @param url Link to the page to index.
     *            If null - indexing all the site.
     *
     * @return true - new indexing task started.
     *
     * @throws URISyntaxException Start indexing link is broken.
     */
    private synchronized boolean startSite(SiteSettings siteSettings, String url) throws URISyntaxException {
        String root = SiteTask.link2root(siteSettings.getUrl()).toString();

        if (!taskList.containsKey(root)) {
            SiteTask siteTask = url == null
                    ? new SiteTask(this, siteSettings)
                    : new SiteTask(this, siteSettings, url);
            taskPool.execute(siteTask);
            taskList.put(root, siteTask);

            return true;
        }

        return false;
    }

    /**
     * Waits for indexing process has been initialised.
     *
     * @throws InterruptedException Waiting was interrupted.
     */
    private void waitIndexingStarted() throws InterruptedException {
        for (SiteTask task : taskList.values()) {
            task.waitStarted();
        }
    }

    /**
     * Start indexing API request handler.
     *
     * @param siteUrl Site link to start indexing or null to start indexing all available sites.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is running.
     */
    public IndexingResponse startIndexing(String siteUrl) {
        if (isIndexing(siteUrl)) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_ALREADY_RUN);
        }

        if (applicationSettings.getSites().isEmpty()) {
            return new IndexingResponseError(IndexingResponse.ERROR_EMPTY_CONFIGURATION);
        }

        int started = 0;

        for (SiteSettings siteSettings : applicationSettings.getSites()) {
            try {
                if (siteUrl == null ||
                        SiteTask.link2root(siteUrl).equals(
                                SiteTask.link2root(siteSettings.getUrl())
                        )
                ) {
                    startSite(siteSettings);
                    started++;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (started == 0) {
            return new IndexingResponseError(IndexingResponse.ERROR_UNKNOWN_SITE);
        }

        try {
            waitIndexingStarted();
        } catch (InterruptedException ignored) {}

        return new IndexingResponseOk();
    }

    /**
     * Start indexing page API request handler.
     *
     * @param url Link to the page to index.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is running.
     */
    public IndexingResponse indexPage(String url) {
        if (isIndexing(url)) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_ALREADY_RUN);
        }

        for (SiteSettings settings : applicationSettings.getSites()) {
            try {
                startSite(settings, url);
                join(url);
                return new IndexingResponseOk();
            } catch (URISyntaxException ignored) {}
        }

        return new IndexingResponseError(IndexingResponse.ERROR_OUTSIDE_PAGE);
    }

    /**
     * Stop indexing API request handler.
     *
     * @param siteUrl Site link to stop indexing or null to stop indexing all indexing sites.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is stopped.
     */
    public IndexingResponse stopIndexing(String siteUrl) {
        if (!isIndexing(siteUrl)) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_NOT_RUN);
        }

        try {
            close(siteUrl);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new IndexingResponseOk();
    }

    /**
     * Waits for indexing is stopped.
     *
     * @param siteUrl Site link to wait for indexing is done or null to wait for every indexing process is done.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on error.
     */
    public IndexingResponse waitIndexing(String siteUrl) {
        if (!isIndexing(siteUrl)) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_NOT_RUN);
        }

        join(siteUrl);

        return new IndexingResponseOk();
    }
}
