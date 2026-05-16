package dto.year;

public record AvailableYearDto(
        Integer year,
        Long totalArticles,
        Long totalJournalArticles,
        Long totalConferenceArticles
) {
}