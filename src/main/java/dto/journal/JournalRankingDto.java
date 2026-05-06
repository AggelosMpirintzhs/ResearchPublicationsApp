package dto.journal;

public record JournalRankingDto(
        Integer journalId,
        String journalName,
        Integer publisherId,
        String publisherName,

        Integer rankingPosition,
        String bestQuartile,
        Double sjrIndex,
        Double citeScore,
        Integer hIndex,

        Integer totalDocs,
        Integer totalDocs3y,
        Integer totalRefs,
        Integer totalCites3y,
        Integer citableDocs3y,
        Double citesPerDoc2y,
        Double refsPerDoc,

        Integer bestAreaId,
        String bestSubjectArea
) {
}