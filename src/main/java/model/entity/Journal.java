package model.entity;

public record Journal(
        Integer journalId,
        String journalName,
        Integer publisherId
) {
}