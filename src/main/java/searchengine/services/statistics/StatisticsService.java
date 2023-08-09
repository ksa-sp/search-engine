package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;

import searchengine.dto.statistics.SiteDetailedStatistics;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import searchengine.services.indexing.IndexingService;

import searchengine.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database statistics and application status service class.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final IndexingService indexingService;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    /**
     * Collects statistics data of database and application current status.
     *
     * @return {@link StatisticsResponse} statistics response object.
     */
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData statistics = response.getStatistics();
        TotalStatistics total = statistics.getTotal();
        List<SiteDetailedStatistics> detailed = statistics.getDetailed();

        Map<String, Site> urlToSite = new HashMap<>();

        for (Site site : siteRepository.findAll()) {
            if (
                    urlToSite.containsKey(site.getUrl())
                    && urlToSite.get(site.getUrl()).getStatus() != IndexingStatus.INDEXED
            ) {
                continue;
            }

            urlToSite.put(site.getUrl(), site);
        }

        for (Site site : urlToSite.values()) {
            SiteDetailedStatistics item;

            detailed.add(item = new SiteDetailedStatistics(
                    site.getUrl(),
                    site.getName(),
                    site.getStatus().toString(),
                    site.getStatusTime().getTime()
            ));

            item.setError(site.getLastError());
            item.setPages((int) pageRepository.countBySiteIdAndCode(site.getId(), 200));
            item.setLemmas((int) lemmaRepository.countBySiteId(site.getId()));
            item.setTasks(indexingService.getSiteIndexingTaskCount(site.getUrl()));

            total.addSites(1);
            total.addPages(item.getPages());
            total.addLemmas(item.getLemmas());
            total.addTasks(item.getTasks());
        }

        total.setIndexing(indexingService.isIndexing());

        return response;
    }
}
