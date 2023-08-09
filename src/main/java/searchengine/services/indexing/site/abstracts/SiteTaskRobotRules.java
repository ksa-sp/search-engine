package searchengine.services.indexing.site.abstracts;

import com.panforge.robotstxt.RobotsTxt;

import searchengine.dto.indexing.HttpPage;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.net.URI;

/**
 * {@link searchengine.services.indexing.site.SiteTask} class robots.txt rules link filter.
 */
public abstract class SiteTaskRobotRules extends SiteTaskHttpUtil {
    private RobotsTxt robotsTxt = null;
    private List<String> siteMapLinks = null;

    /**
     * Whether the link is allowed by robots rules.
     *
     * @param uri Link to check.
     *
     * @return true - link is allowed.
     */
    protected boolean queryRobots(URI uri) {
        return isIgnoreRobotRules() || robotsTxt == null
            || robotsTxt.query(
                    getUserAgent(),
                    uri.getPath()
            );
    }

    /**
     * Initializes robots rules for the site.
     */
    protected void getRobots() {
        String link = getRootUri() + "/robots.txt";
        clearRobotsRules();

        try {
            HttpPage httpPage = new HttpPage(link, new String[]
                    {
                            "Accept:text/plain",
                            "Referer:" + getReferer(),
                            "User-Agent:" + getUserAgent()
                    }
            );

            if (httpPage.getCode() == 200) {
                robotsTxt = RobotsTxt.read(new ByteArrayInputStream(httpPage.getBodyAsBytes()));
                siteMapLinks = robotsTxt.getSitemaps();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Clears all data of robots filter.
     */
    private void clearRobotsRules() {
        robotsTxt = null;
        siteMapLinks = null;
    }
}
