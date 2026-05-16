package service;

import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
import repository.YearRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class YearService {

    private static final String DEFAULT_PUBLICATION_TYPE = "ALL";

    private static final int DEFAULT_FILTER_ID = 0;

    private static final Set<String> ALLOWED_PUBLICATION_TYPES = Set.of(
            "ALL",
            "JOURNAL",
            "CONFERENCE"
    );

    private final YearRepository yearRepository;

    public YearService() {
        this.yearRepository = new YearRepository();
    }

    public List<AvailableYearDto> getAvailableYears() {
        return yearRepository.findAvailableYears();
    }

    public Optional<YearProfileDto> getYearProfile(int year) {
        validateYear(year);

        return yearRepository.findYearProfile(year);
    }

    public List<YearPublicationDto> getYearPublications(
            int year,
            String publicationType,
            Integer journalId,
            Integer conferenceId,
            Integer authorId
    ) {
        validateYear(year);

        String safePublicationType = normalizePublicationType(publicationType);

        int safeJournalId = normalizeFilterId(journalId, "journalId");
        int safeConferenceId = normalizeFilterId(conferenceId, "conferenceId");
        int safeAuthorId = normalizeFilterId(authorId, "authorId");

        validateFilterCombination(
                safePublicationType,
                safeJournalId,
                safeConferenceId
        );

        return yearRepository.findYearPublications(
                year,
                safePublicationType,
                safeJournalId,
                safeConferenceId,
                safeAuthorId
        );
    }

    public List<YearPublicationDto> getAllYearPublications(int year) {
        return getYearPublications(
                year,
                "ALL",
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearJournalPublications(int year) {
        return getYearPublications(
                year,
                "JOURNAL",
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearConferencePublications(int year) {
        return getYearPublications(
                year,
                "CONFERENCE",
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearPublicationsByJournal(
            int year,
            int journalId
    ) {
        return getYearPublications(
                year,
                "JOURNAL",
                journalId,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearPublicationsByConference(
            int year,
            int conferenceId
    ) {
        return getYearPublications(
                year,
                "CONFERENCE",
                DEFAULT_FILTER_ID,
                conferenceId,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearPublicationsByAuthor(
            int year,
            int authorId
    ) {
        return getYearPublications(
                year,
                "ALL",
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                authorId
        );
    }

    private void validateYear(int year) {
        if (year <= 0) {
            throw new IllegalArgumentException("Το year πρέπει να είναι θετικός αριθμός.");
        }
    }

    private String normalizePublicationType(String publicationType) {
        if (publicationType == null || publicationType.isBlank()) {
            return DEFAULT_PUBLICATION_TYPE;
        }

        String normalizedPublicationType = publicationType.trim().toUpperCase();

        if (!ALLOWED_PUBLICATION_TYPES.contains(normalizedPublicationType)) {
            throw new IllegalArgumentException(
                    "Μη έγκυρος τύπος δημοσίευσης. Επιτρεπτές τιμές: ALL, JOURNAL, CONFERENCE."
            );
        }

        return normalizedPublicationType;
    }

    private int normalizeFilterId(Integer id, String fieldName) {
        if (id == null) {
            return DEFAULT_FILTER_ID;
        }

        if (id < 0) {
            throw new IllegalArgumentException("Το " + fieldName + " δεν μπορεί να είναι αρνητικό.");
        }

        return id;
    }

    private void validateFilterCombination(
            String publicationType,
            int journalId,
            int conferenceId
    ) {
        if (journalId > 0 && conferenceId > 0) {
            throw new IllegalArgumentException(
                    "Δεν μπορείς να φιλτράρεις ταυτόχρονα με journalId και conferenceId."
            );
        }

        if (journalId > 0 && publicationType.equals("CONFERENCE")) {
            throw new IllegalArgumentException(
                    "Δεν μπορείς να έχεις publicationType = CONFERENCE και journalId φίλτρο."
            );
        }

        if (conferenceId > 0 && publicationType.equals("JOURNAL")) {
            throw new IllegalArgumentException(
                    "Δεν μπορείς να έχεις publicationType = JOURNAL και conferenceId φίλτρο."
            );
        }
    }
}