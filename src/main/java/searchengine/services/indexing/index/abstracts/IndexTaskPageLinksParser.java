package searchengine.services.indexing.index.abstracts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import searchengine.model.Page;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link searchengine.services.indexing.index.IndexTask} page parsing for links abstract class.
 */
public abstract class IndexTaskPageLinksParser extends IndexTaskProxy {
    /**
     * Parses page to find links.
     *
     * @param page Page entity object.
     *
     * @return The page plain text.
     */
    protected String processLinks(Page page) {
        Document document = Jsoup.parse(
                page.getContent(),
                baseUrl(page.getPath())
        );

        List<URI> list = document.select("a[href]").stream()
                .map(element -> element.attr("abs:href"))
                .map(link -> {
                    try {
                        return new URI(link);
                    } catch (URISyntaxException ignored) {}
                    return null;
                })
                .collect(Collectors.toList());

        addLink(list);

        return document.text();
    }
}
