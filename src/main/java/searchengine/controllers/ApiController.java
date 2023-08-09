package searchengine.controllers;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.services.search.SearchService;
import searchengine.services.indexing.IndexingService;
import searchengine.services.statistics.StatisticsService;

/**
 * REST API Controller.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    /**
     * Request for current statistics data.
     *
     * @return {@link StatisticsResponse} object.
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /**
     * Request to start indexing all the sites configured.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is running.
     */
    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    /**
     * Request to index a page.
     *
     * @param url Link to the page to index.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is running.
     */
    @PostMapping(value = "/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexPage(url));
    }

    /**
     * Request to stop indexing process.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on either error
     * <br>or the indexing process is stopped.
     */
    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    /**
     * Request to wait for indexing process is stopped.
     * <br>
     * Response to the request is sent immediately if indexing is stopped or
     * delayed until indexing process become finished.
     *
     * @return {@link searchengine.dto.indexing.IndexingResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.indexing.IndexingResponseError} object on error.
     */
    @GetMapping("/waitIndexing")
    public ResponseEntity<IndexingResponse> waitIndexing() {
        return ResponseEntity.ok(indexingService.waitIndexing());
    }

    /**
     * Request to search for list of page links most relevant to the query provided.
     *
     * @param query Query to search for.
     * @param site Site link to search in. Search in all sites configured if the parameter is absent.
     * @param offset Index of page in pages list found, list returned must start with. Default is 0.
     * @param limit Maximum number of pages in the list returned. Default is 20.
     *
     * @return {@link searchengine.dto.search.SearchResponseOk} object in the case of success
     * <br>or {@link searchengine.dto.search.SearchResponseError} object on error.
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "20") Integer limit
            ) {
        return ResponseEntity.ok(searchService.search(query, site, offset, limit));
    }
}
