package searchengine.dao;

import org.springframework.data.repository.CrudRepository;

import org.springframework.stereotype.Repository;

import searchengine.model.Index;

import java.util.List;
import java.util.Optional;

/**
 * Index table DAO interface.
 */
@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    /**
     * Counts number of index records of a page.
     *
     * @param pageId Page id.
     *
     * @return Number of records.
     */
    long countByPageId(Integer pageId);

    /**
     * Counts number of index records of a lemma.
     *
     * @param lemmaId Lemma id.
     *
     * @return Number of records.
     */
    long countByLemmaId(Integer lemmaId);

    /**
     * Finds index record of a lemma on a page.
     *
     * @param pageId Page id.
     * @param lemmaId Lemma id.
     *
     * @return Optional object of the record found.
     */
    Optional<Index> findByPageIdAndLemmaId(Integer pageId, Integer lemmaId);

    /**
     * Returns list of index records belong to a page.
     *
     * @param pageId Page id.
     *
     * @return List of the index records.
     */
    List<Index> findAllByPageId(Integer pageId);

    /**
     * Returns list of index records belong to a lemma.
     *
     * @param lemmaId Lemma id.
     *
     * @return List of the index records.
     */
    List<Index> findAllByLemmaId(Integer lemmaId);
}
