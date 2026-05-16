package dto.author;

public record AuthorPublicationDto(
        Integer articleId,
        String articleKey,
        String title,
        Integer year,
        String articleType,

        Integer journalId,
        String journalName,
        String volume,
        String number,

        Integer conferenceId,
        String conferenceAcronym,
        String conferenceTitle,

        String pages,
        String ee,
        String url,

        Long authorCount,
        String authors
) {
}