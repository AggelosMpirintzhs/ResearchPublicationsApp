package dto.chart;

public record ScatterPlotRequestDto(
        String xMetric,
        String yMetric
) {
}