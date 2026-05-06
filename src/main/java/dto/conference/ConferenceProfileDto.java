package dto.conference;

public record ConferenceProfileDto(
        Integer conferenceId,
        String acronym,
        String conferenceTitle,
        Integer icoreId,

        Integer firstYear,
        Integer lastYear,
        Long activeYears,

        Long totalArticles,
        Long totalAuthorOccurrences,
        Long distinctAuthors,

        Double avgArticlesPerYear,
        Double avgAuthorOccurrencesPerYear,
        Double avgAuthorsPerArticle
) {
}