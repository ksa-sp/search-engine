package searchengine.dto.search;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Error response class to the search api request.
 */
@Getter
@RequiredArgsConstructor
public class SearchResponseError extends SearchResponse {
    private final boolean result = false;
    @NonNull
    private final String error;
}
