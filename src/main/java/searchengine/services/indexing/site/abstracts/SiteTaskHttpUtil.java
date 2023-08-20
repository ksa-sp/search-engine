package searchengine.services.indexing.site.abstracts;

import java.net.URI;
import java.net.URISyntaxException;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * {@link searchengine.services.indexing.site.SiteTask} class of http and uri support methods.
 */
public abstract class SiteTaskHttpUtil extends SiteTaskShutdown {
    /**
     * Returns current site root URI.
     *
     * @return Root URI.
     */
    public abstract URI getRootUri();

    /**
     * Returns date and time of the site previous indexing.
     *
     * @return {@link Date} object.
     */
    public abstract Date getIndexedTime();

    /**
     * Compiles URL from current site root link and absolute path provided.
     *
     * @param path Path to add to site root URI.
     *
     * @return Full link.
     */
    public String baseUrl(String path) {
        return getRootUri() + path;
    }

    /**
     * Returns date and time of the site previous indexing.
     *
     * @return Http header formatted date and time or blank string if there was no previous indexing.
     */
    public String getHttpIndexedTime() {
        return httpFormat(getIndexedTime());
    }

    // Static methods

    /**
     * Http headers value of date and time formatter.
     */
    private static DateTimeFormatter httpTimeFormatter
            = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

    /**
     * Converts java Date object to string http header form of date and time.
     *
     * @param date Date to convert.
     *
     * @return Http header date and time or blank string if the parameter is null.
     */
    public static String httpFormat(Date date) {
        if (date != null) {
            return httpTimeFormatter.format(date.toInstant().atZone(ZoneOffset.UTC));
        }
        return "";
    }

    /**
     * Creates URI object based on string link.
     *
     * @param link Link to make URI.
     *
     * @return URI object.
     *
     * @throws URISyntaxException Link is broken.
     */
    public static URI link2uri(String link) throws URISyntaxException {
        URI uri = new URI(link
                .replaceAll("#.*", "")
                .replaceAll("/+$", ""));

        String scheme = uri.getScheme();

        if (scheme == null || !scheme.toLowerCase().matches("^https?$")) {
            throw new URISyntaxException(uri.toString(), "The scheme is not allowed");
        }

        return uri;
    }

    /**
     * Creates URI object based on string link and trim its path part.
     *
     * @param link Link to make URI.
     *
     * @return URI object.
     *
     * @throws URISyntaxException Link is broken.
     */
    public static URI link2root(String link) throws URISyntaxException {
        URI uri = link2uri(link);

        return new URI(
                uri.getScheme(),
                uri.getAuthority(),
                null, null, null
        );
    }
}
