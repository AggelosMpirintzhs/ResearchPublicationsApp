package dto.journal;

public record JournalSearchResultDto(
        Integer journalId,
        String journalName,
        Integer publisherId,
        String publisherName
) {
}