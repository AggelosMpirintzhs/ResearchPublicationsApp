package model.entity;

public record Conference(
        Integer conferenceId,
        String acronym,
        String title,
        Integer icoreId
) {
}