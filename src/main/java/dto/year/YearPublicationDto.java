package dto.year;

import java.time.LocalDate;

public record YearPublicationDto(
        Integer articleId,
        String articleKey,
        String title,
        Integer year,
        String articleType,

        Integer journalId,
        String journalName,
        Integer sourceId,
        String volume,
        String number,

        Integer conferenceId,
        String conferenceAcronym,
        String conferenceTitle,
        Integer icoreId,

        String pages,
        String ee,
        String url,
        LocalDate mdate,

        Long authorCount,
        String authors
) {
}