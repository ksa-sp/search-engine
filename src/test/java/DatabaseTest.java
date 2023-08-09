import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import searchengine.Application;
import searchengine.dao.IndexRepository;
import searchengine.dao.LemmaRepository;
import searchengine.dao.PageRepository;
import searchengine.dao.SiteRepository;
import searchengine.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Application.class
)
@ActiveProfiles("test")     // Load properties from application-test file
@DisplayName("Database Test")
public class DatabaseTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

        @Test
        @DisplayName("Insert Test")
        public void test_01() {
        for (int i = 1; i <= 3; i++) {
            Site site = new Site();
            site.setName(String.valueOf(i));
            site.setUrl("http://www." + "a".repeat(i) + ".net");
            site.setStatus(IndexingStatus.INDEXED);
            site.setStatusTime(new Date());

            Set<Page> pages = new HashSet<>();
            Set<Lemma> lemmas = new HashSet<>();

            for (int j = 1; j <= 3; j++) {
                Page page = new Page();
                page.setContent("");
                page.setPath(String.valueOf(j));

                Lemma lemma = new Lemma();
                lemma.setLemma(String.valueOf(j));
                lemma.setFrequency(j);

                Set<Index> indexes = new HashSet<>();

                for (int k = 1; k <= 3; k++) {
                    Index index = new Index();
                    index.setPageId(page.getId());
                    index.setLemmaId(lemma.getId());
                    index.setRank((float) k);

                    indexes.add(index);
                }

                page.setIndexes(indexes);
                lemma.setIndexes(indexes);

                pages.add(page);
                lemmas.add(lemma);
            }

            site.setPages(pages);
            site.setLemmas(lemmas);

            siteRepository.save(site);
        }

        assertEquals(3, siteRepository.count());
        assertEquals(9, pageRepository.count());
        assertEquals(9, lemmaRepository.count());
        assertEquals(27, indexRepository.count());
    }

    @Test
    @DisplayName("Fetch Test")
    public void test_02() {
        Site site = siteRepository.findByUrlAndStatus("http://www.aa.net", IndexingStatus.INDEXED).orElse(null);

        assertNotNull(site);
        assertEquals("2", site.getName());

        Page page = pageRepository.findBySiteIdAndPath(site.getId(), "1").orElse(null);

        assertNotNull(page);
        assertEquals("1", page.getPath());
        assertEquals(site.getId(), page.getSiteId());

        page = pageRepository.findBySiteIdAndPath(site.getId(), "4").orElse(null);

        assertNull(page);

        int i = 0;
        for (Page p : pageRepository.findAllByCode(1)) i++;

        assertEquals(0, i);
    }

    @Test
    @DisplayName("Update Test")
    public void test_03() {
        for (Page page : pageRepository.findAll()) {
            page.setCode(1);
            pageRepository.save(page);
        }

        assertEquals(9, pageRepository.countByCode(1));

        assertEquals(0, siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertEquals(0, pageRepository.countByCode(-1));

        for (Site site : siteRepository.findAll()) {
            site.setStatus(IndexingStatus.INDEXING);
            siteRepository.save(site);

            for (Page page : pageRepository.findAllBySiteId(site.getId())) {
                page.setCode(-page.getCode());
                pageRepository.save(page);
            }
        }

        assertEquals(3, siteRepository.countByStatus(IndexingStatus.INDEXING));
        assertEquals(9, pageRepository.countByCode(-1));
    }

    @Test
    @DisplayName("NULL Keys Test")
    public void test_04() {
        jdbcTemplate.execute("UPDATE page SET site_id = NULL WHERE path = '3'");
        jdbcTemplate.execute("UPDATE lemma SET site_id = NULL WHERE frequency = 3");
        jdbcTemplate.execute("UPDATE `index` SET page_id = NULL, lemma_id = NULL WHERE `rank` = 3");

        assertEquals(3, pageRepository.countBySiteId(null));
        assertEquals(3, lemmaRepository.countBySiteId(null));
        assertEquals(9, indexRepository.countByPageId(null));

        Index.clean();
        Lemma.delete(null);
        Page.delete(null);

        assertEquals(0, pageRepository.countBySiteId(null));
        assertEquals(0, lemmaRepository.countBySiteId(null));
        assertEquals(0, indexRepository.countByPageId(null));
    }

    @Test
    @DisplayName("Delete Test")
    public void test_05() {
        assertEquals(3, siteRepository.count());
        assertEquals(6, pageRepository.count());
        assertEquals(6, lemmaRepository.count());
        assertEquals(12, indexRepository.count());

        jdbcTemplate.queryForList(
                        "SELECT id FROM page WHERE path <> '1'"
                ).stream()
                .map(x -> (Integer)(x.get("id")))
                .map(x -> pageRepository.findById(x))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(x -> pageRepository.delete(x));

        assertEquals(3, siteRepository.count());
        assertEquals(3, pageRepository.count());
        assertEquals(6, lemmaRepository.count());
        assertEquals(6, indexRepository.count());

        Site site = siteRepository.findByUrlAndStatus("http://www.aaa.net", IndexingStatus.INDEXING).orElse(null);

        assertNotNull(site);

        siteRepository.delete(site);

        assertEquals(2, siteRepository.count());
        assertEquals(2, pageRepository.count());
        assertEquals(4, lemmaRepository.count());
        assertEquals(4, indexRepository.count());

        siteRepository.deleteAll();

        assertEquals(0, siteRepository.count());
        assertEquals(0, pageRepository.count());
        assertEquals(0, lemmaRepository.count());
        assertEquals(0, indexRepository.count());
    }
}
