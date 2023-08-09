package searchengine.dto.statistics;

import lombok.Getter;

/**
 * Response class to statistics api request.
 * <br>
 * {@link StatisticsData} is sent in the response.
 */
@Getter
public class StatisticsResponse {
    private final boolean result = true;
    private final StatisticsData statistics = new StatisticsData();
}
