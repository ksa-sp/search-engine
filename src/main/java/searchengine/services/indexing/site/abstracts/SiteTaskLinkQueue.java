package searchengine.services.indexing.site.abstracts;

import lombok.Getter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * {@link searchengine.services.indexing.site.SiteTask} class link queue implementation.
 */
public abstract class SiteTaskLinkQueue extends SiteTaskJobCounter {
    private final String stopMessage = "http://0:0";     // Force child thread to finish

    private final BlockingDeque<String> linkQueue = new LinkedBlockingDeque<>();

    // Number of url to process limit, < 0 - all possible
    @Getter
    private int linkLimitCount = 1;

    protected void clearLinks() {
        linkQueue.clear();
    }

    protected void disableLinkLimitCount() {
        linkLimitCount = -1;
    }

    /**
     * Add stop link to this object pages queue to exit child thread.
     * <br>
     * Waits if the queue is full.
     */
    protected void addStopLink() {
        try {
            linkQueue.putFirst(stopMessage);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Add link to the link queue.
     * <br>
     * Link is added to queue if it points into the site.
     *
     * @param link Link to add.
     *
     * @return true - link successfully added to queue.
     */
    private boolean addLink(String link) {
        if (linkLimitCount == 0) {
            return false;
        }
        if (linkLimitCount > 0) {
            linkLimitCount--;
        }

        try {
            URI uri = link2uri(link);
            String url = uri.toString();

            if (!getRootUri().relativize(uri).isAbsolute()
                    && !linkQueue.contains(url)
                    && queryRobots(uri)
            ) {
                startJob();
                try {
                    linkQueue.put(url);
                    return true;
                } catch (InterruptedException e) {
                    doneJob();
                }
            }
        } catch (URISyntaxException ignored) {}

        return false;
    }

    /**
     * Add link to the link queue.
     * <br>
     * Link is added to queue if it points into the site.
     *
     * @param uri Link to add.
     *
     * @return true - link successfully added to queue.
     */
    public boolean addLink(URI uri) {
        if (uri != null) {
            return addLink(uri.toString());
        }
        return false;
    }

    /**
     * Add all links of the list to the link queue.
     *
     * @param uris List of links to add.
     */
    public void addLink(List<URI> uris) {
        uris.forEach(this::addLink);
    }

    /**
     * Takes URI from this object links queue.
     * <br>
     * Waits until the queue has an item.
     *
     * @return URI taken.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    public URI getLink() throws InterruptedException {
        try {
            return new URI(linkQueue.take());
        } catch (URISyntaxException ignored) {
            // URI syntax checked before link is added to queue
        }
        return null;        // Will never here
    }
}
