package searchengine.config;

import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import searchengine.dao.SiteRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.indexing.index.IndexTask;
import searchengine.services.indexing.page.PageTask;
import searchengine.services.indexing.site.SiteTask;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Global application properties class.
 * <br>
 * The properties are applied to every site with no equal local properties of {@link SiteSettings}.
 */
@Data
@Component
@ConfigurationProperties(prefix = "application-settings")
public class ApplicationSettings {
    /**
     * UserAgent http request header value.
     */
    private String userAgent;

    /**
     * Referer http request header value.
     */
    private String referer;

    /**
     * Do not follow rules of robots.txt file in the root of site.
     */
    private Boolean ignoreRobotRules = false;

    /**
     * Do not load full page from the site if it has not changed since previous indexing.
     * <br>
     * Similar to a web browser cache.
     */
    private Boolean update = false;

    /**
     * Minimum time interval between http requests to a site.
     * <br>
     * Value of milliseconds.
     */
    private Integer connectionInterval = 1000;

    /**
     * Maximum number of concurrent tasks indexing a site.
     */
    private Integer tasksPerSite = 1;

    /**
     * Maximum search response snippet length.
     */
    private Integer snippetSize = 160;

    /**
     * List of every site local properties.
     */
    private List<SiteSettings> sites;

    // Derivative properties

    /**
     * Maximum number of concurrent threads indexing all sites.
     *
     * @return Maximum number of concurrent threads indexing all sites.
     */
    public int countIndexingThreads() {
        int threadCount = 0;

        for (SiteSettings site : sites) {
            threadCount += site.getTasksPerSite() * 2 + 1;
        }

        return threadCount;
    }

    // Application initialization

    @Autowired
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private final TransactionTemplate transactionTemplate;
    @Autowired
    private final SiteRepository siteRepository;

    /**
     * Initialises static fields of different classes.
     */
    @EventListener(ApplicationStartedEvent.class)
    private void initStaticFields() {
        SiteSettings.setApplicationSettings(this);

        Site.setJdbcTemplate(jdbcTemplate);
        Page.setJdbcTemplate(jdbcTemplate);
        Lemma.setJdbcTemplate(jdbcTemplate);
        Index.setJdbcTemplate(jdbcTemplate);

        SiteTask.setTransactionTemplate(transactionTemplate);
        PageTask.setTransactionTemplate(transactionTemplate);
        IndexTask.setTransactionTemplate(transactionTemplate);
    }

    /**
     * Sets database site names to the names of application settings.
     */
    @EventListener(ApplicationReadyEvent.class)
    private void syncSiteNames() {
        sites.forEach(siteSettings -> {
            if (siteSettings.getName() != null && !siteSettings.getName().isBlank()) {
                try {
                    siteRepository.findAllByUrl(
                            SiteTask.link2root(siteSettings.getUrl()).toString()
                    ).forEach(site -> {
                        if (!siteSettings.getName().equals(site.getName())) {
                            site.setName(siteSettings.getName());
                            siteRepository.save(site);
                        }
                    });
                } catch (URISyntaxException ignored) {}
            }
        });
    }
}
