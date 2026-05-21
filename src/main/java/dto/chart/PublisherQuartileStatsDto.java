package dto.chart;

public record PublisherQuartileStatsDto(
        int publisherId,
        String publisherName,
        String quartile,
        long publicationCount,
        long totalPublications
) {
}