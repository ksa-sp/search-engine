package searchengine.dao;

import org.springframework.data.repository.CrudRepository;

import org.springframework.stereotype.Repository;

import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

/**
 * Page table DAO interface.
 */
@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    /**
     * Counts number of page records of a site.
     *
     * @param siteId Site id.
     *
     * @return Number of records.
     */
    long countBySiteId(Integer siteId);

    /**
     * Counts number of page records with http return code provided.
     *
     * @param code Http code.
     *
     * @return Number of records.
     */
    long countByCode(Integer code);

    /**
     * Counts number of page records of a site with http return code provided.
     *
     * @param siteId Site id.
     * @param code Http code.
     *
     * @return Number of records.
     */
    long countBySiteIdAndCode(Integer siteId, Integer code);

    /**
     * Finds page record of a site with path provided.
     *
     * @param siteId Site id.
     * @param path Absolute path to the page within the site, started with slash.
     *             Root path equals "/".
     *
     * @return Optional object of the record found.
     */
    Optional<Page> findBySiteIdAndPath(Integer siteId, String path);

    /**
     * Returns list of all page records of a site.
     *
     * @param siteId Site id.
     *
     * @return List of the page records.
     */
    List<Page> findAllBySiteId(Integer siteId);

    /**
     * Returns list of all page records with http return code provided.
     *
     * @param code Http code.
     *
     * @return List of the page records.
     */
    List<Page> findAllByCode(Integer code);

    /**
     * Returns list of all page records of a site with http return code provided.
     *
     * @param siteId Site id.
     * @param code Http code.
     *
     * @return List of the page records.
     */
    List<Page> findAllBySiteIdAndCode(Integer siteId, Integer code);
}
