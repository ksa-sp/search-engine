package searchengine.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Site properties class.
 * <br>
 * If a property is absent, equal global application property of {@link ApplicationSettings} is used.
 */
@Setter
public class SiteSettings {
    /**
     * Application settings to use as default values in the class getters.
     */
    @Setter
    private static ApplicationSettings applicationSettings = null;

    /**
     * Site start indexing link.
     */
    @Getter
    private String url;

    /**
     * Site name for search response.
     */
    @Getter
    private String name;

    // Following properties must not have default values here:

    /**
     * UserAgent http request header value.
     */
    private String userAgent;

    /**
     * Referer http request header value.
     */
    private String referer;

    /**
     * Do not follow rules of robots.txt file in the root of the site.
     */
    private Boolean ignoreRobotRules;

    /**
     * Do not load full page from the site if it has not changed since previous indexing.
     * <br>
     * Similar to a web browser cache.
     */
    private Boolean update;

    /**
     * Minimum time interval between http requests to the site.
     * <br>
     * Value of milliseconds.
     */
    private Integer connectionInterval;

    /**
     * Maximum number of concurrent tasks indexing the site.
     */
    private Integer tasksPerSite;

    /**
     * UserAgent http request header value.
     * <br>
     * Returns site local property or global application property if the local one is absent.
     *
     * @return UserAgent http request header value or empty string if the property is not configured.
     */
    public String getUserAgent() {
        String userAgent = this.userAgent;

        if (userAgent == null) {
            userAgent = applicationSettings.getUserAgent();
        }
        if (userAgent == null) {
            userAgent = "";
        }

        return userAgent;
    }

    /**
     * Referer http request header value.
     * <br>
     * Returns site local property or global application property if the local one is absent.
     *
     * @return Referer http request header value or empty string if the property is not configured.
     */
    public String getReferer() {
        String referer = this.referer;

        if (referer == null) {
            referer = applicationSettings.getReferer();
        }
        if (referer == null) {
            referer = "";
        }

        return referer;
    }

    /**
     * Whether to ignore robots.txt file rules in the root of the site.
     * <br>
     * Returns site local property or global application property if the local property is absent.
     *
     * @return true - Do not follow rules of robots.txt file in the root of the site.
     */
    public boolean isIgnoreRobotRules() {
        Boolean ignoreRobotRules = this.ignoreRobotRules;

        if (ignoreRobotRules == null) {
            ignoreRobotRules = applicationSettings.getIgnoreRobotRules();
        }
        if (ignoreRobotRules == null) {
            ignoreRobotRules = false;
        }

        return ignoreRobotRules;
    }

    /**
     * Whether to avoid loading not changed pages.
     * <br>
     * Similar to a web browser cache.
     * <br>
     * Returns site local property or global application property if the local one is absent.
     *
     * @return true - Do not load full page from the site if it has not changed since previous indexing.
     */
    public boolean isUpdate() {
        Boolean update = this.update;

        if (update == null) {
            update = applicationSettings.getUpdate();
        }
        if (update == null) {
            update = false;
        }

        return update;
    }

    /**
     * Minimum time interval between http requests to the site.
     * <br>
     * Returns site local property or global application property if the local one is absent.
     *
     * @return Time interval in ms or 1000 if the property is not configured.
     */
    public long getConnectionInterval() {
        Integer interval = this.connectionInterval;

        if (interval == null) {
            interval = applicationSettings.getConnectionInterval();
        }
        if (interval == null) {
            interval = 1000;
        }

        return (long) (Math.random() * interval / 2) + interval;
    }

    /**
     * Maximum number of concurrent threads indexing the site.
     * <br>
     * Returns site local property or global application property if the local one is absent.
     *
     * @return Maximum number of concurrent threads indexing the site or 1 if the property is not configured.
     */
    public int getTasksPerSite() {
        Integer tasksPerSite = this.tasksPerSite;

        if (tasksPerSite == null) {
            tasksPerSite = applicationSettings.getTasksPerSite();
        }
        if (tasksPerSite == null) {
            tasksPerSite = 1;
        }

        return tasksPerSite;
    }
}
