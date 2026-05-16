package dto.chart;

import java.util.List;

public record SelectedIdsChartRequestDto(
        List<Integer> ids,
        Integer startYear,
        Integer endYear
) {
}