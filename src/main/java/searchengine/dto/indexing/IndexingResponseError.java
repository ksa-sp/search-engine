package searchengine.dto.indexing;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Error response class to the indexing api requests.
 */
@Getter
@RequiredArgsConstructor
public class IndexingResponseError extends IndexingResponse {
    private final boolean result = false;
    @NonNull
    private final String error;
}
