package dto.journal;

public record JournalProfileDto(
        Integer journalId,
        String journalName,
        Integer publisherId,
        String publisherName,

        Integer firstYear,
        Integer lastYear,
        Long activeYears,

        Long totalArticles,
        Long totalAuthorOccurrences,
        Long distinctAuthorsAllTime,

        Double avgArticlesPerYear,
        Double avgAuthorOccurrencesPerYear,
        Double avgAuthorsPerArticle
) {
}