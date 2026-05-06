package model.entity;

import java.time.LocalDate;

public record Article(
        Integer articleId,
        String ee,
        String articleKey,
        LocalDate mdate,
        String pages,
        String title,
        Integer year,
        Integer typeId,
        String url
) {
}