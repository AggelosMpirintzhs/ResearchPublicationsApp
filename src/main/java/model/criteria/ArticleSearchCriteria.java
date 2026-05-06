package model.criteria;

import java.math.BigDecimal;

public record ArticleSearchCriteria(
        String titleKeyword,
        String authorKeyword,

        Integer yearFrom,
        Integer yearTo,

        String articleType,

        String journalName,
        String conferenceAcronym,
        String publisherName,

        String bestSubjectArea,
        String bestQuartile,
        BigDecimal minSjrIndex,
        BigDecimal minCiteScore,
        Integer minHIndex,

        String conferenceRankLabel,
        String primaryForCategory,

        Integer limit,
        Integer offset
) {
}