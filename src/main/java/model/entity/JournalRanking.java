package model.entity;

import java.math.BigDecimal;

public record JournalRanking(
        Integer journalId,
        Integer rankingPosition,
        Integer bestAreaId,
        String bestQuartile,
        BigDecimal sjrIndex,
        BigDecimal citeScore,
        Integer hIndex,
        Integer totalDocs,
        Integer totalDocs3y,
        Integer totalRefs,
        Integer totalCites3y,
        Integer citableDocs3y,
        BigDecimal citesPerDoc2y,
        BigDecimal refsPerDoc
) {
}