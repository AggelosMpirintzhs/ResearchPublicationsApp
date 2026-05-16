package dto.author;

public record AuthorYearlyStatsDto(
        Integer year,
        Long totalArticles
) {
}