import dto.IndexingResponseTest;
import dto.SearchResponseTest;

import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import searchengine.Application;
import searchengine.config.ApplicationSettings;
import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = Application.class
)

@ActiveProfiles("test")     // Load properties from application-test file
@DisplayName("REST API Test")
public class ApiTest {
    @LocalServerPort
    private int port;
    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private TestRestTemplate testRestTemplate;
    private RestTemplate restTemplate;

    private String localLink;
    private String apiLink;

    @BeforeEach
    public void init() {
        localLink = "http://localhost:" + port;
        apiLink = localLink + "/api/";

        restTemplate = testRestTemplate.getRestTemplate();
        restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(
                        HttpClientBuilder.create().build()      // Apache
                )
        );
    }

    @Test
    @DisplayName("Configuration Check")
    public void test_01() throws MalformedURLException {
        assertTrue(1 <= applicationSettings.getSites().size());
        URL url = new URL(applicationSettings.getSites().get(0).getUrl());
        assertEquals(url.getHost(), "localhost");
        assertEquals(url.getPort(), port);
    }

    @Test
    @DisplayName("Start Indexing")
    public void test_02() {
        ResponseEntity<IndexingResponseTest> response = restTemplate.getForEntity(
                apiLink + "startIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());
    }

    @Test
    @DisplayName("Start Error")
    public void test_03() {
        ResponseEntity<IndexingResponseTest> response = restTemplate.getForEntity(
                apiLink + "startIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isResult());

        assertTrue(0 < siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.INDEXED));
        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.FAILED));
    }

    @Test
    @DisplayName("Stop Indexing")
    public void test_04() {
        ResponseEntity<IndexingResponseTest> response = restTemplate.getForEntity(
                apiLink + "stopIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());
    }

    @Test
    @DisplayName("Stop Error")
    public void test_05() {
        ResponseEntity<IndexingResponseTest> response = restTemplate.getForEntity(
                apiLink + "stopIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isResult());

        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.INDEXED));
        assertTrue(0 < siteRepository.countByStatus(IndexingStatus.FAILED));
    }

    @Test
    @DisplayName("Page Indexing Test")
    public void test_06() throws URISyntaxException {
        ResponseEntity<IndexingResponseTest> response = restTemplate.postForEntity(
                UriComponentsBuilder
                        .fromHttpUrl(apiLink + "indexPage")
                        .queryParam("url", localLink + "/test/test.html")
                        .toUriString(),
                null,
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());

        Site site = siteRepository.findByUrlAndStatus(localLink, IndexingStatus.INDEXED).orElse(null);

        assertNotNull(site);

        Page page = pageRepository.findBySiteIdAndPath(site.getId(), "/test/test.html").orElse(null);

        assertNotNull(page);

        response = restTemplate.postForEntity(
                new URI(apiLink + "indexPage?url=http://fake.site.com/test"),
                "",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isResult());
        assertEquals(IndexingResponse.ERROR_OUTSIDE_PAGE, response.getBody().getError());

        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertTrue(0 < siteRepository.countByStatus(IndexingStatus.INDEXED));
        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.FAILED));
    }

    @Test
    @DisplayName("Sites Indexing Test")
    public void test_07() {
        siteRepository.deleteAll();

        assertEquals(0, siteRepository.count());

        ResponseEntity<IndexingResponseTest> response = restTemplate.getForEntity(
                apiLink + "startIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());

        response = restTemplate.getForEntity(
                apiLink + "waitIndexing",
                IndexingResponseTest.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());

        Site site = siteRepository.findByUrlAndStatus(localLink, IndexingStatus.INDEXED).orElse(null);

        assertNotNull(site);

        Page page = pageRepository.findBySiteIdAndPath(site.getId(), "/test/test.html").orElse(null);

        assertNotNull(page);

        Lemma lemma12 = lemmaRepository.findBySiteIdAndLemma(site.getId(), "двенадца").orElse(null);
        Lemma lemma20 = lemmaRepository.findBySiteIdAndLemma(site.getId(), "twenti").orElse(null);

        assertNotNull(lemma12);
        assertNotNull(lemma20);
        assertEquals(
                12,
                indexRepository.findByPageIdAndLemmaId(page.getId(), lemma12.getId())
                        .orElse(new Index())
                        .getRank()
        );
        assertEquals(
                20,
                indexRepository.findByPageIdAndLemmaId(page.getId(), lemma20.getId())
                        .orElse(new Index())
                        .getRank()
        );

        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertTrue(0 < siteRepository.countByStatus(IndexingStatus.INDEXED));
        assertTrue(0 == siteRepository.countByStatus(IndexingStatus.FAILED));
    }

    @Test
    @DisplayName("Search Test")
    public void test_08() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiLink + "search");
        String url = builder.toUriString();

        ResponseEntity<SearchResponseTest> response = restTemplate.getForEntity(url, SearchResponseTest.class);

        assertEquals(400, response.getStatusCodeValue());


        url = builder.queryParam("query", "     ").build(false).toUriString();

        response = restTemplate.getForEntity(url, SearchResponseTest.class);

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().getResult());
        assertEquals(SearchResponse.ERROR_NO_QUERY, response.getBody().getError());


        url = builder.replaceQueryParam("query", "twenty").toUriString();

        response = restTemplate.getForEntity(url, SearchResponseTest.class);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().getResult());
        assertEquals(1.0f, response.getBody().getData()[0].getRelevance());
        assertTrue(response.getBody().getData()[0].getSnippet().length() > 0);


        url = builder.queryParam("site", "http://some.other.site.com").toUriString();

        response = restTemplate.getForEntity(url, SearchResponseTest.class);

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().getResult());
        assertEquals(SearchResponse.ERROR_SITE_NOT_INDEXED, response.getBody().getError());
    }

    @Test
    @DisplayName("Statistics Test")
    public void test_09() {
        ResponseEntity<StatisticsResponse> response = restTemplate.getForEntity(
                apiLink + "statistics",
                StatisticsResponse.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isResult());

        assertTrue(response.getBody().getStatistics().getTotal().getSites() > 0);
        assertTrue(response.getBody().getStatistics().getTotal().getPages() > 0);
        assertTrue(response.getBody().getStatistics().getTotal().getLemmas() > 0);
        assertFalse(response.getBody().getStatistics().getTotal().isIndexing());

        assertTrue(response.getBody().getStatistics().getDetailed().size() > 0);
        assertTrue(response.getBody().getStatistics().getDetailed().get(0).getUrl().length() > 0);
        assertTrue(response.getBody().getStatistics().getDetailed().get(0).getPages()
                <= response.getBody().getStatistics().getTotal().getPages());
        assertTrue(response.getBody().getStatistics().getDetailed().get(0).getLemmas()
                <= response.getBody().getStatistics().getTotal().getLemmas());
    }
}
