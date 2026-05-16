package dto.chart;

public record ScatterPlotPointDto(
        Integer id,
        String label,
        Double xValue,
        Double yValue
) {
}