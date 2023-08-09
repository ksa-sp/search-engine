package searchengine.dto.statistics;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Cumulative statistics data.
 * <br>
 * The data is sent in response to the statistics api request.
 * See {@link StatisticsResponse}
 */
@Getter
public class StatisticsData {
    private final TotalStatistics total = new TotalStatistics();                    // Application statistics
    private final List<SiteDetailedStatistics> detailed = new ArrayList<>();        // Site data list
}
