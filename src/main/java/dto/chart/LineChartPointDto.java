package dto.chart;

public record LineChartPointDto(
        String seriesName,
        Integer year,
        Long value
) {
}