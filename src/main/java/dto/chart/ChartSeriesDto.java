package dto.chart;

import java.util.List;

public record ChartSeriesDto(
        String seriesName,
        List<LineChartPointDto> points
) {
}