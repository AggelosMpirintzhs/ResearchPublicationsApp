package dto.author;

public record AuthorYearlyStatsByTypeDto(
        Integer year,
        Long totalArticles,
        Long totalJournalArticles,
        Long totalConferenceArticles
) {
}