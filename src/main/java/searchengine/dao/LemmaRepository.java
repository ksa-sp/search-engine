package searchengine.dao;

import org.springframework.data.repository.CrudRepository;

import org.springframework.stereotype.Repository;

import searchengine.model.Lemma;

import java.util.Optional;
import java.util.List;

/**
 * Lemma table DAO interface.
 */
@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    /**
     * Counts number of lemma records of a site.
     *
     * @param siteId Site id.
     *
     * @return Number of records.
     */
    long countBySiteId(Integer siteId);

    /**
     * Returns list of lemma records belong to a site.
     *
     * @param siteId Site Id.
     *
     * @return List of the lemma records.
     */
    List<Lemma> findAllBySiteId(Integer siteId);

    /**
     * Returns list of equal lemma records belong to different sites.
     *
     * @param lemma Lemma.
     *
     * @return List of the lemma records.
     */
    List<Lemma> findAllByLemma(String lemma);

    /**
     * Finds lemma record of a site with the frequency value provided.
     *
     * @param siteId Site id.
     * @param frequency Frequency value.
     *
     * @return List of the lemma records.
     */
    List<Lemma> findAllBySiteIdAndFrequency(Integer siteId, Integer frequency);

    /**
     * Finds lemma record of a site.
     *
     * @param siteId Site id.
     * @param lemma Lemma.
     *
     * @return Optional object of the record found.
     */
    Optional<Lemma> findBySiteIdAndLemma(Integer siteId, String lemma);
}
