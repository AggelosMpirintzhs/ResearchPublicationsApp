package dto.conference;

public record ConferenceSearchResultDto(
        Integer conferenceId,
        String acronym,
        String conferenceTitle,
        Integer icoreId
) {
}