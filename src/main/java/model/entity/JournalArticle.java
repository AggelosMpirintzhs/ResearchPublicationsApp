package model.entity;

public record JournalArticle(
        Integer articleId,
        Integer journalId,
        Integer sourceId,
        String volume,
        String number
) {
}