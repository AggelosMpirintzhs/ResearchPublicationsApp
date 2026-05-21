package service.charts;

import dto.chart.PublisherOptionDto;
import dto.chart.PublisherQuartileStatsDto;
import service.JournalService;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PublisherAnalysisService {

    public static final String QUARTILE_Q1 = "Q1";
    public static final String QUARTILE_Q2 = "Q2";
    public static final String QUARTILE_Q3 = "Q3";
    public static final String QUARTILE_Q4 = "Q4";

    public static final List<String> QUARTILES = List.of(
            QUARTILE_Q1,
            QUARTILE_Q2,
            QUARTILE_Q3,
            QUARTILE_Q4
    );

    public static final int DEFAULT_PUBLISHER_SEARCH_LIMIT = 10;

    private final JournalService journalService = new JournalService();

    public List<String> getQuartiles() {
        return QUARTILES;
    }

    public List<PublisherOptionDto> searchPublishers(String searchText, int searchLimit) {
        if (searchText == null || searchText.isBlank()) {
            return new ArrayList<>();
        }

        List<PublisherOptionDto> results = journalService.getPublisherOptions(searchText, searchLimit);
        return sortPublisherResultsByRelevance(results, searchText);
    }

    public boolean alreadySelected(List<PublisherOptionDto> selectedPublishers, PublisherOptionDto publisher) {
        if (publisher == null || selectedPublishers == null || selectedPublishers.isEmpty()) {
            return false;
        }

        for (PublisherOptionDto selected : selectedPublishers) {
            if (selected.publisherId() == publisher.publisherId()) {
                return true;
            }
        }

        return false;
    }

    public List<PublisherSummary> loadPublisherSummaries(List<PublisherOptionDto> selectedPublishers) {
        if (selectedPublishers == null || selectedPublishers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> publisherIds = new ArrayList<>();

        for (PublisherOptionDto publisher : selectedPublishers) {
            if (publisher != null) {
                publisherIds.add(publisher.publisherId());
            }
        }

        List<PublisherQuartileStatsDto> stats =
                journalService.getPublisherQuartilePublicationStatsForPublishers(publisherIds);

        return convertStatsToSummaries(selectedPublishers, stats);
    }

    private List<PublisherOptionDto> sortPublisherResultsByRelevance(
            List<PublisherOptionDto> results,
            String query
    ) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = normalizeSearchText(query);
        List<PublisherOptionDto> sortedResults = new ArrayList<>(results);

        sortedResults.sort((first, second) -> {
            int firstScore = publisherRelevanceScore(first, normalizedQuery);
            int secondScore = publisherRelevanceScore(second, normalizedQuery);

            if (firstScore != secondScore) {
                return Integer.compare(firstScore, secondScore);
            }

            String firstName = normalizeSearchText(first.publisherName());
            String secondName = normalizeSearchText(second.publisherName());

            if (firstName.length() != secondName.length()) {
                return Integer.compare(firstName.length(), secondName.length());
            }

            return firstName.compareTo(secondName);
        });

        return sortedResults;
    }

    private int publisherRelevanceScore(PublisherOptionDto publisher, String query) {
        String name = normalizeSearchText(publisher == null ? "" : publisher.publisherName());

        if (name.equals(query)) {
            return 0;
        }

        if (name.startsWith(query)) {
            return 1;
        }

        if (name.contains(query)) {
            return 2;
        }

        if (containsAllQueryWords(name, query)) {
            return 3;
        }

        return 4;
    }

    private boolean containsAllQueryWords(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        for (String word : query.split(" ")) {
            if (!word.isBlank() && !text.contains(word)) {
                return false;
            }
        }

        return true;
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ");
    }

    private List<PublisherSummary> convertStatsToSummaries(
            List<PublisherOptionDto> selectedPublishers,
            List<PublisherQuartileStatsDto> stats
    ) {
        Map<Integer, PublisherAccumulator> grouped = new LinkedHashMap<>();

        for (PublisherOptionDto publisher : selectedPublishers) {
            if (publisher == null) {
                continue;
            }

            grouped.put(
                    publisher.publisherId(),
                    new PublisherAccumulator(
                            publisher.publisherId(),
                            publisher.publisherName(),
                            publisher.totalPublications()
                    )
            );
        }

        if (stats != null) {
            for (PublisherQuartileStatsDto row : stats) {
                if (row == null) {
                    continue;
                }

                PublisherAccumulator accumulator = grouped.computeIfAbsent(
                        row.publisherId(),
                        publisherId -> new PublisherAccumulator(
                                row.publisherId(),
                                row.publisherName(),
                                row.totalPublications()
                        )
                );

                accumulator.setPublisherName(row.publisherName());
                accumulator.setTotalPublications(row.totalPublications());
                accumulator.add(row.quartile(), row.publicationCount());
            }
        }

        List<PublisherSummary> summaries = new ArrayList<>();

        for (PublisherAccumulator accumulator : grouped.values()) {
            summaries.add(accumulator.toSummary());
        }

        return summaries;
    }

    public String buildPublisherCategory(PublisherSummary summary) {
        if (summary == null) {
            return "-";
        }

        return shortenName(summary.publisherName(), 24) + " #" + summary.publisherId();
    }

    public String buildTotalPublisherKey(PublisherSummary summary) {
        if (summary == null) {
            return "";
        }

        return "publisher#" + summary.publisherId();
    }

    public String formatPublisherForUi(PublisherOptionDto publisher) {
        if (publisher == null) {
            return "";
        }

        return publisher.publisherName() + " (" + publisher.totalPublications() + " publications)";
    }

    public String shortenName(String name, int maxLength) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        return name.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String normalizeQuartile(String quartile) {
        if (quartile == null) {
            return "";
        }

        String normalized = quartile.trim().toUpperCase();

        if (normalized.contains("Q1")) {
            return QUARTILE_Q1;
        }

        if (normalized.contains("Q2")) {
            return QUARTILE_Q2;
        }

        if (normalized.contains("Q3")) {
            return QUARTILE_Q3;
        }

        if (normalized.contains("Q4")) {
            return QUARTILE_Q4;
        }

        return normalized;
    }

    private static class PublisherAccumulator {

        private final int publisherId;
        private String publisherName;
        private long totalPublications;
        private long q1;
        private long q2;
        private long q3;
        private long q4;

        private PublisherAccumulator(
                int publisherId,
                String publisherName,
                long totalPublications
        ) {
            this.publisherId = publisherId;
            this.publisherName = publisherName;
            this.totalPublications = totalPublications;
        }

        private void setPublisherName(String publisherName) {
            if (publisherName != null && !publisherName.isBlank()) {
                this.publisherName = publisherName;
            }
        }

        private void setTotalPublications(long totalPublications) {
            if (totalPublications >= 0) {
                this.totalPublications = totalPublications;
            }
        }

        private void add(String quartile, long count) {
            String normalizedQuartile = normalizeQuartile(quartile);

            if (QUARTILE_Q1.equals(normalizedQuartile)) {
                q1 += count;
            } else if (QUARTILE_Q2.equals(normalizedQuartile)) {
                q2 += count;
            } else if (QUARTILE_Q3.equals(normalizedQuartile)) {
                q3 += count;
            } else if (QUARTILE_Q4.equals(normalizedQuartile)) {
                q4 += count;
            }
        }

        private PublisherSummary toSummary() {
            return new PublisherSummary(
                    publisherId,
                    publisherName,
                    q1,
                    q2,
                    q3,
                    q4,
                    totalPublications
            );
        }
    }

    public record PublisherSummary(
            int publisherId,
            String publisherName,
            long q1,
            long q2,
            long q3,
            long q4,
            long totalPublications
    ) {

        public long countForQuartile(String quartile) {
            if (QUARTILE_Q1.equals(quartile)) {
                return q1;
            }

            if (QUARTILE_Q2.equals(quartile)) {
                return q2;
            }

            if (QUARTILE_Q3.equals(quartile)) {
                return q3;
            }

            if (QUARTILE_Q4.equals(quartile)) {
                return q4;
            }

            return 0;
        }
    }
}