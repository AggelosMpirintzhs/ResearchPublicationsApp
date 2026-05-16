package dto.author;

public record AuthorProfileDto(
        Integer authorId,
        String authorName,

        Integer firstYear,
        Integer lastYear,
        Long activeYears,

        Long totalArticles,
        Long totalJournalArticles,
        Long totalConferenceArticles,

        Long distinctJournals,
        Long distinctConferences,

        Double avgArticlesPerYear
) {
}