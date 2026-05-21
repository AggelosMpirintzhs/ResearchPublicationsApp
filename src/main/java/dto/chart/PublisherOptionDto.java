package dto.chart;

public record PublisherOptionDto(
        int publisherId,
        String publisherName,
        long totalPublications
) {
}