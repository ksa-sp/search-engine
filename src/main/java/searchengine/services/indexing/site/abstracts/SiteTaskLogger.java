package searchengine.services.indexing.site.abstracts;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import searchengine.Application;

/**
 * {@link searchengine.services.indexing.site.SiteTask} logging provider.
 */
public abstract class SiteTaskLogger extends SiteTaskProxy {
    @Getter
    private final Logger logger = LoggerFactory.getLogger(Application.class);
}
