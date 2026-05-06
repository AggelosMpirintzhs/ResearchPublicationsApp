package dto.conference;

import java.time.LocalDate;

public record ConferenceArticleDto(
        Integer articleId,
        String articleKey,
        String title,
        Integer year,
        String articleType,

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