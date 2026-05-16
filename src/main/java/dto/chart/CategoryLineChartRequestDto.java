package dto.chart;

public record CategoryLineChartRequestDto(
        Integer categoryId,
        Integer startYear,
        Integer endYear
) {
}