package searchengine.dto.indexing;

import lombok.Getter;

/**
 * Response class to the indexing api requests.
 */
@Getter
public class IndexingResponseOk extends IndexingResponse{
    private final boolean result = true;
}
