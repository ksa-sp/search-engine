package searchengine.services.indexing.site;

import lombok.Getter;

import searchengine.config.SiteSettings;
import searchengine.services.indexing.IndexingService;
import searchengine.services.indexing.site.abstracts.SiteTaskChildTaskController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * Site indexing master thread class.
 * <br>
 * Starts first thread of {@link searchengine.services.indexing.page.PageTask}.
 * <br>
 * Puts starting link of the site on this thread links queue
 * for following downloading by a thread of {@link searchengine.services.indexing.page.PageTask}.
 */
public class SiteTask extends SiteTaskChildTaskController {
    @Getter
    private final IndexingService service;
    @Getter
    private final SiteSettings siteSettings;

    @Getter
    private final URI rootUri;
    private final URI startUri;

    private final CountDownLatch startedLatch = new CountDownLatch(1);

    /**
     * Indexing one page constructor.
     *
     * @param service {@link IndexingService} object to get configuration and dao objects from.
     * @param siteSettings site the indexed page belongs to.
     * @param url link to the indexed page.
     * @throws URISyntaxException if the link is broken or outside the site.
     */
    public SiteTask(IndexingService service, SiteSettings siteSettings, String url) throws URISyntaxException {
        // Indexing one page only constructor

        this.service = service;
        this.siteSettings = siteSettings;

        startUri = link2uri(url);
        rootUri = link2root(siteSettings.getUrl());

        if (rootUri.relativize(startUri).isAbsolute()) {
            throw new URISyntaxException(url, "External start link");
        }
    }

    /**
     * Indexing all site constructor.
     *
     * @param service {@link IndexingService} object to get configuration and dao objects from.
     * @param siteSettings site to be indexed configuration.
     * @throws URISyntaxException if start link in the siteSettings is broken.
     */
    public SiteTask(IndexingService service, SiteSettings siteSettings) throws URISyntaxException {
        // Indexing all the site constructor

        this(service, siteSettings, siteSettings.getUrl());

        disableLinkLimitCount();        // Process all links of the site
    }

    @Override
    protected void compute() {
        try {
            if (!addLink(startUri)) {
                return;
            }

            Date startTime = new Date();
            getLogger().info("Start indexing " + getRootUri());

            // Start and wait for working threads

            startSite();

            // Stop working threads

            stopTasks();

            // Finalization

            doneSite();

            getLogger().info("Finish indexing " + getRootUri() + " duration "
                    + Duration.between(startTime.toInstant(), new Date().toInstant())
                    .toString().replaceFirst("^\\D+", "")
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            startedLatch.countDown();
        }
    }

    /**
     * Synchronizes start indexing process till the start of first working thread and waits for the process to finish.
     *
     * @throws InterruptedException Start indexing process was interrupted.
     * @throws IOException Database access error occurred.
     */
    private synchronized void startSite() throws InterruptedException, IOException {
        getRobots();
        initSite();

        // Start first working thread

        connectionDelay();

        startedLatch.countDown();

        // Wait for work is done or shutdown

        try {
            wait();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Waits for indexing process has been initialised.
     *
     * @throws InterruptedException Waiting was interrupted.
     */
    public void waitStarted() throws InterruptedException {
        startedLatch.await();
    }
}
