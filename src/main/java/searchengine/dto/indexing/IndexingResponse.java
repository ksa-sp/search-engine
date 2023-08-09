package searchengine.dto.indexing;

/**
 * Abstract class for responses to indexing api requests.
 * <br>
 * Contains string error constants.
 */
public abstract class IndexingResponse {
    public static final String ERROR_INDEXING_ALREADY_RUN
            = "Индексация уже запущена";
    public static final String ERROR_INDEXING_NOT_RUN
            = "Индексация не запущена";
    public static final String ERROR_OUTSIDE_PAGE
            = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";
}
