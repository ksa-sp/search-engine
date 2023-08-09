package searchengine.dto.search;

/**
 * Abstract class for responses to search api request.
 * <br>
 * Contains string error constants.
 */
public abstract class SearchResponse {
    public static final String ERROR_NO_QUERY
            = "Задан пустой поисковый запрос";
    public static final String ERROR_SITE_NOT_INDEXED
            = "Сайт не был индексирован";
}
