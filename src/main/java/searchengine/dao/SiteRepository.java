package searchengine.dao;

import org.springframework.data.repository.CrudRepository;

import org.springframework.stereotype.Repository;

import searchengine.model.IndexingStatus;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

/**
 * Site table DAO interface.
 */
@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    /**
     * Counts number of site records with indexing status provided.
     *
     * @param status Current indexing status of the site.
     *
     * @return Number of records.
     */
    long countByStatus(IndexingStatus status);

    /**
     * Finds site record with address and status provided.
     *
     * @param url Site address with no path.
     * @param status Current indexing status of the site.
     *
     * @return Optional object of the site record found.
     */
    Optional<Site> findByUrlAndStatus(String url, IndexingStatus status);

    /**
     * Finds all sites of the status provided.
     *
     * @param status Indexing status.
     *
     * @return List of the site entities found.
     */
    List<Site> findAllByStatus(IndexingStatus status);

    /**
     * Finds all sites of the address provided.
     *
     * @param url Site address with no path.
     *
     * @return List of the site entities found.
     */
    List<Site> findAllByUrl(String url);
}
