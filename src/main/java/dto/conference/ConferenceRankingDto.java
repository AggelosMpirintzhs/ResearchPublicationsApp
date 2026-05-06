package dto.conference;

public record ConferenceRankingDto(
        Integer conferenceId,
        String acronym,
        String conferenceTitle,
        Integer icoreId,

        String rankLabel,
        Integer primaryFoRId,
        String primaryFoRName
) {
}