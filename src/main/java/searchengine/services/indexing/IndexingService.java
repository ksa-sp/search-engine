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
        if (taskPool == null) {
            taskPool = new ForkJoinPool(applicationSettings.countIndexingThreads());
        }

        for (SiteTask siteTask : taskList.values()) {
            if (!siteTask.isDone()) {
                return true;
            }
        }

        taskList.clear();

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
     * Waits for all tasks to finish
     */
    private synchronized void join() {
        taskList.values().forEach(SiteTask::join);
        taskList.clear();
    }

    /**
     * Activates user coused indexing shutdown.
     */
    public void shutdown() {
        taskList.values().forEach(t -> t.shutdown(USER_SHUTDOWN_ERROR));
    }

    /**
     * Shutdown indexing process and wait it for finish.
     */
    @Override
    public void close() throws InterruptedException {
        shutdown();
        join();
    }

    /**
     * Start indexing a site.
     *
     * @param siteSettings Site to index.
     *
     * @throws URISyntaxException Site start indexing link is broken.
     */
    private void startSite(SiteSettings siteSettings) throws URISyntaxException {
        startSite(siteSettings, null);
    }

    /**
     * Start indexing a page of a site or all sate.
     *
     * @param siteSettings Site to index.
     * @param url Link to the page to index.
     *            If null - indexing all the site.
     *
     * @throws URISyntaxException Start indexing link is broken.
     */
    private synchronized void startSite(SiteSettings siteSettings, String url) throws URISyntaxException {
        String key = SiteTask.link2root(siteSettings.getUrl()).toString();

        if (!taskList.containsKey(key)) {
            SiteTask siteTask = url == null
                    ? new SiteTask(this, siteSettings)
                    : new SiteTask(this, siteSettings, url);
            taskPool.execute(siteTask);
            taskList.put(key, siteTask);
        }
    }

    /**
     * Waits for indexing process has been initialised.
     *
     * @throws InterruptedException Waiting was interrupted.
     */
    public void waitIndexingStarted() throws InterruptedException {
        for (SiteTask task : taskList.values()) {
            task.waitStarted();
        }
    }

    /**
     * Start indexing API request handler.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is running.
     */
    public IndexingResponse startIndexing() {
        if (isIndexing()) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_ALREADY_RUN);
        }

        applicationSettings.getSites().forEach(x -> {
            try {
                startSite(x);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });

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
        if (isIndexing()) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_ALREADY_RUN);
        }

        for (SiteSettings settings : applicationSettings.getSites()) {
            try {
                startSite(settings, url);
                join();
                return new IndexingResponseOk();
            } catch (URISyntaxException ignored) {}
        }

        return new IndexingResponseError(IndexingResponse.ERROR_OUTSIDE_PAGE);
    }

    /**
     * Stop indexing API request handler.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is stopped.
     */
    public IndexingResponse stopIndexing() {
        if (!isIndexing()) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_NOT_RUN);
        }

        try {
            close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new IndexingResponseOk();
    }

    /**
     * Waits for indexing is stopped.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on error.
     */
    public IndexingResponse waitIndexing() {
        if (!isIndexing()) {
            return new IndexingResponseError(IndexingResponse.ERROR_INDEXING_NOT_RUN);
        }

        join();
        return new IndexingResponseOk();
    }
}
