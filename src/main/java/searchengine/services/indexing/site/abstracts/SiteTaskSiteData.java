package searchengine.services.indexing.site.abstracts;

import lombok.Getter;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link searchengine.services.indexing.site.SiteTask} site data related methods.
 */
public abstract class SiteTaskSiteData extends SiteTaskLinkQueue {
    /**
     * Site entity indexing at the moment.
     */
    @Getter
    private Site indexingSite = null;

    /**
     * Previously indexed site entity.
     */
    @Getter
    private Site indexedSite = null;

    /**
     * Refreshes site entity fields of the object with database data.
     *
     * @param findIndexed true - load previously indexed site entity besides indexing one
     *                    <br>
     *                    false - load current indexing site entity only.
     *
     * @return false - indexing site record is not found in database.
     */
    protected boolean findSite(boolean findIndexed) {
        if (indexingSite != null) {
            indexingSite = getSiteRepository().findById(indexingSite.getId()).orElse(null);
        }
        if (indexingSite == null && getRootUri() != null) {
            indexingSite = getSiteRepository().findByUrlAndStatus(
                    getRootUri().toString(),
                    IndexingStatus.INDEXING
            ).orElse(null);
        }

        if (indexingSite == null && getRootUri() != null) {
            indexingSite = getSiteRepository().findByUrlAndStatus(
                    getRootUri().toString(),
                    IndexingStatus.FAILED
            ).orElse(null);
        }

        if (findIndexed) {
            if (indexedSite != null) {
                indexedSite = getSiteRepository().findById(indexedSite.getId()).orElse(null);
            }
            if (indexedSite == null && getRootUri() != null) {
                indexedSite = getSiteRepository().findByUrlAndStatus(
                        getRootUri().toString(),
                        IndexingStatus.INDEXED
                ).orElse(null);
            }
        }

        return indexingSite != null;
    }

    /**
     * Returns indexing site database record id.
     *
     * @return Site id or null if indexing site entity is not loaded.
     */
    public Integer getIndexingSiteId() {
        if (indexingSite != null) {
            return indexingSite.getId();
        }
        return null;
    }

    /**
     * Returns indexed site database record id.
     *
     * @return Site id or null if indexed site entity is not loaded.
     */
    public Integer getIndexedSiteId() {
        if (indexedSite != null) {
            return indexedSite.getId();
        }
        return null;
    }

    /**
     * Returns date and time of the site previous indexing.
     *
     * @return {@link Date} object or null if site has not been indexed before.
     */
    @Override
    public Date getIndexedTime() {
        if (getIndexedSite() != null) {
            return getIndexedSite().getStatusTime();
        }
        return null;
    }

    /**
     * Initialises database data for starting new indexing process.
     *
     * @throws IOException Database modifications failed.
     */
    protected void initSite() throws IOException {
        if (findSite(false)) {            // Remove existing indexing data
            Site.delete(getIndexingSiteId());
            indexingSite = null;
        }

        if (findSite(isUpdate())) {                 // Check for indexing data is removed
            throw new IOException("Can not remove indexing site record from database.");
        }

        if (indexingSite == null) {
            indexingSite = new Site();
            indexingSite.setName(getSiteSettings().getName());
            indexingSite.setUrl(getRootUri().toString());
            indexingSite.setStatus(IndexingStatus.INDEXING);
            indexingSite.setStatusTime(new Date());

            getSiteRepository().save(indexingSite);
        }
    }

    /**
     * Finalization of site indexing process.
     */
    protected void doneSite() {
        if (!findSite(true)) {                          // Broken database
            return;
        }

        if (getShutdownError() != null) {
            getIndexingSite().setLastError(getShutdownError());
        }

        if (isShutdown()) {
            getIndexingSite().setStatus(IndexingStatus.FAILED);
            getSiteRepository().save(getIndexingSite());
            return;
        }

        // Update lemmas data

        updateLemmas();

        // One-page indexing

        donePage();

        if (!findSite(true)) {          // There was one page indexing
            return;
        }

        // Set indexing site result status

        getIndexingSite().setStatus(
                getPageRepository().countBySiteIdAndCode(getIndexingSiteId(), Page.FATAL_ERROR_CODE) > 0
                        ? IndexingStatus.FAILED
                        : IndexingStatus.INDEXED
        );

        getTransactionTemplate().execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        if (getIndexedSite() != null
                                && getIndexingSite().getStatus() == IndexingStatus.INDEXED
                        ) {
                            Site.delete(getIndexedSiteId());
                        }
                        getSiteRepository().save(getIndexingSite());
                    }
                }
        );
    }

    /**
     * Finalises one page indexing.
     */
    private void donePage() {
        if (getLinkLimitCount() >= 0 && getIndexedSite() != null) {
            for (Page page : getPageRepository().findAllBySiteId(getIndexingSiteId())) {
                Map<Integer, Lemma> deleteLemmas = new HashMap<>();
                Map<Integer, Lemma> updateLemmas = new HashMap<>();
                List<Index> updateIndexes = new ArrayList<>();

                findPageLemmas(page).forEach(indexingLemma -> {
                    Lemma indexedLemma = getLemmaRepository()
                            .findBySiteIdAndLemma(getIndexedSiteId(), indexingLemma.getLemma())
                            .orElse(null);

                    if (indexedLemma != null) {
                        int id = indexedLemma.getId();

                        indexedLemma.setFrequency(indexedLemma.getFrequency() + indexingLemma.getFrequency());
                        updateLemmas.put(id, indexedLemma);

                        getIndexRepository().findAllByLemmaId(indexingLemma.getId()).forEach(index -> {
                            index.setLemmaId(id);
                            updateIndexes.add(index);
                        });

                        deleteLemmas.put(indexingLemma.getId(), indexingLemma);
                    } else {
                        indexingLemma.setSiteId(getIndexedSiteId());
                        updateLemmas.put(indexingLemma.getId(), indexingLemma);
                    }
                });

                Page indexedPage = findIndexedPage(page, true);

                if (indexedPage != null) {
                    findPageLemmas(indexedPage).forEach(lemma -> {
                        int id = lemma.getId();

                        if (updateLemmas.containsKey(id)) {
                            lemma = updateLemmas.get(id);
                        }
                        lemma.setFrequency(lemma.getFrequency() - 1);

                        if (lemma.getFrequency() > 0) {
                            updateLemmas.put(id, lemma);
                        } else {
                            updateLemmas.remove(id);
                            deleteLemmas.put(id, lemma);
                        }
                    });
                }

                page.setSiteId(getIndexedSiteId());

                getTransactionTemplate().execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                updateLemmas.values().forEach(lemma -> getLemmaRepository().save(lemma));
                                updateIndexes.forEach(index -> getIndexRepository().save(index));
                                deleteLemmas.values().forEach(lemma -> getLemmaRepository().delete(lemma));

                                if (indexedPage != null) {
                                    getPageRepository().delete(indexedPage);
                                }

                                getPageRepository().save(page);
                            }
                        }
                );
            }

            Site.delete(getIndexingSiteId());
        }
    }

    /**
     * Finds all lemmas of the page provided.
     *
     * @param page Page to find lemmas of.
     *
     * @return List of lemmas.
     */
    private List<Lemma> findPageLemmas(Page page) {
        return getIndexRepository()
                .findAllByPageId(page.getId())
                .stream()
                .map(Index::getLemmaId)
                .distinct()
                .map(lemmaId -> getLemmaRepository().findById(lemmaId).orElse(null))
                .filter(Objects::nonNull)
                .filter(lemma -> Objects.equals(lemma.getSiteId(), page.getSiteId()))
                .collect(Collectors.toList());
    }

    /**
     * Finds page in indexed site, equal to the page provided.
     *
     * @param page Page to find in indexed site.
     * @param anyCode false - returns page of non error code only.
     *
     * @return Page found or null if nothing found.
     */
    public Page findIndexedPage(Page page, boolean anyCode) {
        Page indexedPage = null;

        if (getIndexedSite() != null && page != null) {
            indexedPage = getPageRepository()
                    .findBySiteIdAndPath(getIndexedSiteId(), page.getPath())
                    .orElse(null);

            if (indexedPage != null
                    && !anyCode
                    && indexedPage.getCode() != 200
                    && indexedPage.getCode() != Page.NOT_A_PAGE_CODE
            ) {
                indexedPage = null;
            }
        }

        return indexedPage;
    }

    /**
     * Calculates frequency value of every lemma record has the value equal to zero.
     * <br>
     * Removes excess lemma records.
     */
    private void updateLemmas() {
        List<Lemma> updateLemmas = new ArrayList<>();
        List<Lemma> deleteLemmas = new ArrayList<>();

        // Calculate lemmas data

        getLemmaRepository().findAllBySiteIdAndFrequency(getIndexingSiteId(), 0).forEach(lemma -> {
            int count = (int) getIndexRepository().countByLemmaId(lemma.getId());

            if (count > 0) {
                lemma.setFrequency(count);
                updateLemmas.add(lemma);
            } else {
                deleteLemmas.add(lemma);
            }
        });

        // Update lemmas

        for (int i = 0; i < updateLemmas.size(); i += 1000) {
            List<Lemma> lemmas = updateLemmas.subList(i, Math.min(updateLemmas.size(), i + 1000));

            getTransactionTemplate().execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            lemmas.forEach(lemma -> getLemmaRepository().save(lemma));
                        }
                    }
            );
        }

        // Remove excess lemmas

        for (int i = 0; i < deleteLemmas.size(); i += 1000) {
            List<Lemma> lemmas = deleteLemmas.subList(i, Math.min(deleteLemmas.size(), i + 1000));

            getTransactionTemplate().execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            lemmas.forEach(lemma -> getLemmaRepository().delete(lemma));
                        }
                    }
            );
        }
    }
}
