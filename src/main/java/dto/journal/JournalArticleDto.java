package dto.journal;

import java.time.LocalDate;

public record JournalArticleDto(
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

        String pages,
        String ee,
        String url,
        LocalDate mdate,

        Long authorCount,
        String authors
) {
}