package dto.chart;

public record CategoryTrendDto(
        String category,
        int year,
        long count
) {
}