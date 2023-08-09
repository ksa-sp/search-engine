package searchengine.dto.search;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Response class to the search api request.
 */
@Getter
public class SearchResponseOk extends SearchResponse {
    private final boolean result = true;
    private int count = 0;
    private final List<SearchDataItem> data = new ArrayList<>();

    /**
     * Adds new page into the list of found pages of the response.
     *
     * @param searchDataItem Page data to add.
     */
    public void add(SearchDataItem searchDataItem) {
        data.add(searchDataItem);
        count = data.size();
    }
}
