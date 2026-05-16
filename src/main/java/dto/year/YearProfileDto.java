package dto.year;

public record YearProfileDto(
        Integer year,

        Long totalArticles,
        Long totalJournalArticles,
        Long totalConferenceArticles,

        Long distinctJournals,
        Long distinctConferences,

        Long totalAuthorOccurrences,
        Long distinctAuthors,
        Double avgAuthorsPerArticle
) {
}