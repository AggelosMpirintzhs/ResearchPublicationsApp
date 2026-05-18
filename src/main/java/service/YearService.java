package service;

import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
import repository.YearRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class YearService {

    public static final String PUBLICATION_TYPE_ALL = "ALL";
    public static final String PUBLICATION_TYPE_JOURNAL = "JOURNAL";
    public static final String PUBLICATION_TYPE_CONFERENCE = "CONFERENCE";

    private static final String DEFAULT_PUBLICATION_TYPE = PUBLICATION_TYPE_ALL;

    private static final int DEFAULT_FILTER_ID = 0;
    private static final int DEFAULT_LAST_ARTICLE_ID = 0;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_BATCH_SIZE = 5000;

    private static final Set<String> ALLOWED_PUBLICATION_TYPES = Set.of(
            PUBLICATION_TYPE_ALL,
            PUBLICATION_TYPE_JOURNAL,
            PUBLICATION_TYPE_CONFERENCE
    );

    private final YearRepository yearRepository;

    public YearService() {
        this.yearRepository = new YearRepository();
    }

    public YearService(YearRepository yearRepository) {
        this.yearRepository = yearRepository;
    }

    public List<AvailableYearDto> getAvailableYears() {
        return yearRepository.findAvailableYears();
    }

    public YearPageData loadYearPageData(
            int year,
            String publicationType
    ) {
        validateYear(year);

        String safePublicationType = normalizePublicationType(publicationType);

        Optional<YearProfileDto> profile = yearRepository.findYearProfile(year);

        YearProfileDto profileDto = profile.orElse(null);

        long expectedPublicationCount =
                getExpectedPublicationCount(profileDto, safePublicationType);

        return new YearPageData(
                year,
                safePublicationType,
                profileDto,
                expectedPublicationCount
        );
    }

    public Optional<YearProfileDto> getYearProfile(int year) {
        validateYear(year);

        return yearRepository.findYearProfile(year);
    }

    public long getExpectedPublicationCount(
            AvailableYearDto yearDto,
            String publicationType
    ) {
        if (yearDto == null) {
            return 0L;
        }

        String safePublicationType = normalizePublicationType(publicationType);

        if (PUBLICATION_TYPE_JOURNAL.equals(safePublicationType)) {
            return nullToZero(yearDto.totalJournalArticles());
        }

        if (PUBLICATION_TYPE_CONFERENCE.equals(safePublicationType)) {
            return nullToZero(yearDto.totalConferenceArticles());
        }

        return nullToZero(yearDto.totalArticles());
    }

    public long getExpectedPublicationCount(
            YearProfileDto profile,
            String publicationType
    ) {
        if (profile == null) {
            return 0L;
        }

        String safePublicationType = normalizePublicationType(publicationType);

        if (PUBLICATION_TYPE_JOURNAL.equals(safePublicationType)) {
            return nullToZero(profile.totalJournalArticles());
        }

        if (PUBLICATION_TYPE_CONFERENCE.equals(safePublicationType)) {
            return nullToZero(profile.totalConferenceArticles());
        }

        return nullToZero(profile.totalArticles());
    }

    public void loadYearPublicationsInBatches(
            int year,
            String publicationType,
            Integer journalId,
            Integer conferenceId,
            Integer authorId,
            Integer lastArticleId,
            Integer initialBatchSize,
            Integer regularBatchSize,
            Consumer<List<YearPublicationDto>> onBatchLoaded,
            BooleanSupplier shouldContinue
    ) {
        validateYear(year);

        if (onBatchLoaded == null) {
            throw new IllegalArgumentException("Το onBatchLoaded δεν μπορεί να είναι null.");
        }

        if (shouldContinue == null) {
            throw new IllegalArgumentException("Το shouldContinue δεν μπορεί να είναι null.");
        }

        String safePublicationType = normalizePublicationType(publicationType);

        int safeJournalId = normalizeFilterId(journalId, "journalId");
        int safeConferenceId = normalizeFilterId(conferenceId, "conferenceId");
        int safeAuthorId = normalizeFilterId(authorId, "authorId");

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeInitialBatchSize = normalizeBatchSize(initialBatchSize);
        int safeRegularBatchSize = normalizeBatchSize(regularBatchSize);

        validateFilterCombination(
                safePublicationType,
                safeJournalId,
                safeConferenceId
        );

        boolean firstBatch = true;

        while (shouldContinue.getAsBoolean()) {
            int currentBatchSize = firstBatch
                    ? safeInitialBatchSize
                    : safeRegularBatchSize;

            List<YearPublicationDto> batch =
                    yearRepository.findYearPublicationsBatch(
                            year,
                            safePublicationType,
                            safeJournalId,
                            safeConferenceId,
                            safeAuthorId,
                            safeLastArticleId,
                            currentBatchSize
                    );

            if (batch.isEmpty()) {
                break;
            }

            onBatchLoaded.accept(batch);

            YearPublicationDto lastPublication = batch.get(batch.size() - 1);

            if (lastPublication.articleId() == null) {
                break;
            }

            safeLastArticleId = lastPublication.articleId();
            firstBatch = false;
        }
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

    public List<YearPublicationDto> getYearPublicationsBatch(
            int year,
            String publicationType,
            Integer journalId,
            Integer conferenceId,
            Integer authorId,
            Integer lastArticleId,
            Integer batchSize
    ) {
        validateYear(year);

        String safePublicationType = normalizePublicationType(publicationType);

        int safeJournalId = normalizeFilterId(journalId, "journalId");
        int safeConferenceId = normalizeFilterId(conferenceId, "conferenceId");
        int safeAuthorId = normalizeFilterId(authorId, "authorId");

        int safeLastArticleId = normalizeLastArticleId(lastArticleId);
        int safeBatchSize = normalizeBatchSize(batchSize);

        validateFilterCombination(
                safePublicationType,
                safeJournalId,
                safeConferenceId
        );

        return yearRepository.findYearPublicationsBatch(
                year,
                safePublicationType,
                safeJournalId,
                safeConferenceId,
                safeAuthorId,
                safeLastArticleId,
                safeBatchSize
        );
    }

    public List<YearPublicationDto> getAllYearPublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_ALL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearJournalPublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_JOURNAL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    public List<YearPublicationDto> getYearConferencePublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_CONFERENCE,
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
                PUBLICATION_TYPE_JOURNAL,
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
                PUBLICATION_TYPE_CONFERENCE,
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
                PUBLICATION_TYPE_ALL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                authorId
        );
    }

    public List<YearPublicationDto> getAllYearPublicationsBatch(
            int year,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_ALL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                lastArticleId,
                batchSize
        );
    }

    public List<YearPublicationDto> getYearJournalPublicationsBatch(
            int year,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_JOURNAL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                lastArticleId,
                batchSize
        );
    }

    public List<YearPublicationDto> getYearConferencePublicationsBatch(
            int year,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_CONFERENCE,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                lastArticleId,
                batchSize
        );
    }

    public List<YearPublicationDto> getYearPublicationsByJournalBatch(
            int year,
            int journalId,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_JOURNAL,
                journalId,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                lastArticleId,
                batchSize
        );
    }

    public List<YearPublicationDto> getYearPublicationsByConferenceBatch(
            int year,
            int conferenceId,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_CONFERENCE,
                DEFAULT_FILTER_ID,
                conferenceId,
                DEFAULT_FILTER_ID,
                lastArticleId,
                batchSize
        );
    }

    public List<YearPublicationDto> getYearPublicationsByAuthorBatch(
            int year,
            int authorId,
            Integer lastArticleId,
            Integer batchSize
    ) {
        return getYearPublicationsBatch(
                year,
                PUBLICATION_TYPE_ALL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                authorId,
                lastArticleId,
                batchSize
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

    private int normalizeLastArticleId(Integer lastArticleId) {
        if (lastArticleId == null) {
            return DEFAULT_LAST_ARTICLE_ID;
        }

        if (lastArticleId < 0) {
            throw new IllegalArgumentException("Το lastArticleId δεν μπορεί να είναι αρνητικό.");
        }

        return lastArticleId;
    }

    private int normalizeBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
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

        if (journalId > 0 && publicationType.equals(PUBLICATION_TYPE_CONFERENCE)) {
            throw new IllegalArgumentException(
                    "Δεν μπορείς να έχεις publicationType = CONFERENCE και journalId φίλτρο."
            );
        }

        if (conferenceId > 0 && publicationType.equals(PUBLICATION_TYPE_JOURNAL)) {
            throw new IllegalArgumentException(
                    "Δεν μπορείς να έχεις publicationType = JOURNAL και conferenceId φίλτρο."
            );
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    public record YearPageData(
            int year,
            String publicationType,
            YearProfileDto profile,
            long expectedPublicationCount
    ) {
        public boolean hasProfile() {
            return profile != null;
        }
    }
}