package dto.conference;

public record ConferenceYearlyStatsDto(
        Integer conferenceId,
        String acronym,
        String conferenceTitle,
        Integer year,

        Long totalArticles,
        Long totalAuthorOccurrences,
        Long distinctAuthors,
        Double avgAuthorsPerArticle
) {
}