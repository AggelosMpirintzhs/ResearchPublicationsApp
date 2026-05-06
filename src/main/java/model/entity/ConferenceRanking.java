package model.entity;

public record ConferenceRanking(
        Integer conferenceId,
        String rankLabel,
        Integer primaryForId
) {
}