package dto.journal;

public record JournalYearlyStatsDto(
        Integer journalId,
        String journalName,
        Integer year,

        Long totalArticles,
        Long totalAuthorOccurrences,
        Long distinctAuthors,
        Double avgAuthorsPerArticle
) {
}