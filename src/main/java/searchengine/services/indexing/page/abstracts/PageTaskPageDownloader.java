package searchengine.services.indexing.page.abstracts;

import org.apache.http.NoHttpResponseException;
import org.apache.http.ParseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import searchengine.dto.indexing.HttpPage;
import searchengine.model.Page;

import java.net.URI;

/**
 * {@link searchengine.services.indexing.page.PageTask} page downloading abstract class.
 */
public abstract class PageTaskPageDownloader extends PageTaskPageQueue {
    /**
     * Downloads page, saves page database record
     * and puts the page on this object pages queue.
     *
     * @param uri URI of the page to download.
     *
     * @return true - continue page downloading loop,
     * false - exit the thread.
     *
     * @throws InterruptedException Connection delay is interrupted.
     */
    protected boolean processLink(URI uri) throws InterruptedException {
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            path += "?" + uri.getQuery();
        }

        if (tryLockString(path) != null) {              // Path is processing by another thread
            doneJob();
            return true;
        }

        try {
            Page page = getPageRepository()
                    .findBySiteIdAndPath(getIndexingSiteId(), path)
                    .orElse(null);

            if (page != null) {                         // Already indexed page
                doneJob();
                return true;
            }

            if (connectionDelay()) {                    // Shutdown is active
                doneJob();
                return false;
            }

            page = new Page();
            page.setSiteId(getIndexingSiteId());
            page.setPath(path);

            Page indexedPage = findIndexedPage(page);

            try {
                HttpPage httpPage = new HttpPage(uri, new String[]
                        {
                                "Accept:text/*,application/xml,application/*+xml",
                                "Referer:" + getReferer(),
                                "User-Agent:" + getUserAgent(),
                                "If-Modified-Since:" + getHttpIndexedTime()
                        }
                );

                switch (httpPage.getCode()) {
                    case 200:
                        page.setCode(httpPage.getCode());
                        page.setContent(httpPage.getBody());
                        break;
                    case 304:
                        if (indexedPage != null) {
                            page.setCode(indexedPage.getCode());
                            page.setContent(indexedPage.getContent());
                            break;
                        }
                    default:
                        page.setCode(httpPage.getCode());
                        page.setContent(uri + "\r\n" + httpPage.getRequest());

                        if (httpPage.getCode() >= 500 && httpPage.getCode() < 600) {
                            shutdown(httpPage.getCode() + " server error");
                        }
                }
            } catch (ParseException e) {            // Not indexed mime type
                page.setCode(Page.NOT_A_PAGE_CODE);
                page.setContent(e.getMessage());
            } catch (NoHttpResponseException
                     | ConnectTimeoutException
                     | HttpHostConnectException e
            ) {                                     // Try to connect again
                System.out.println(e.getMessage());
                addLink(uri);
                doneJob();
                return true;
            } catch (Exception e) {                 // Fatal error
                e.printStackTrace();
                page.setCode(Page.FATAL_ERROR_CODE);
                page.setContent(e.toString());
                shutdown(e.getMessage());
            }

            addPage(page);
            return true;

        } finally {
            unlockString(path);
        }
    }
}
