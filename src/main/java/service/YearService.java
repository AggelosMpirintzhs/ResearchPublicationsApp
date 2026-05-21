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

    // Creates year service
    public YearService() {
        this.yearRepository = new YearRepository();
    }

    // Creates year service
    public YearService(YearRepository yearRepository) {
        this.yearRepository = yearRepository;
    }

    // Gets available years
    public List<AvailableYearDto> getAvailableYears() {
        return yearRepository.findAvailableYears();
    }

    // Loads year page
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

    // Gets year profile
    public Optional<YearProfileDto> getYearProfile(int year) {
        validateYear(year);

        return yearRepository.findYearProfile(year);
    }

    // Gets publication count
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

    // Gets publication count
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

    // Loads publication batches
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
            throw new IllegalArgumentException("onBatchLoaded cannot be null.");
        }

        if (shouldContinue == null) {
            throw new IllegalArgumentException("shouldContinue cannot be null.");
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

    // Gets year publications
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

    // Gets publications batch
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

    // Gets all publications
    public List<YearPublicationDto> getAllYearPublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_ALL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    // Gets journal publications
    public List<YearPublicationDto> getYearJournalPublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_JOURNAL,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    // Gets conference publications
    public List<YearPublicationDto> getYearConferencePublications(int year) {
        return getYearPublications(
                year,
                PUBLICATION_TYPE_CONFERENCE,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID,
                DEFAULT_FILTER_ID
        );
    }

    // Gets journal filter
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

    // Gets conference filter
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

    // Gets author filter
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

    // Gets all batch
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

    // Gets journal batch
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

    // Gets conference batch
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

    // Gets journal batch
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

    // Gets conference batch
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

    // Gets author batch
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

    // Validates year value
    private void validateYear(int year) {
        if (year <= 0) {
            throw new IllegalArgumentException("year must be a positive number.");
        }
    }

    // Normalizes publication type
    private String normalizePublicationType(String publicationType) {
        if (publicationType == null || publicationType.isBlank()) {
            return DEFAULT_PUBLICATION_TYPE;
        }

        String normalizedPublicationType = publicationType.trim().toUpperCase();

        if (!ALLOWED_PUBLICATION_TYPES.contains(normalizedPublicationType)) {
            throw new IllegalArgumentException(
                    "Invalid publication type. Allowed values: ALL, JOURNAL, CONFERENCE."
            );
        }

        return normalizedPublicationType;
    }

    // Normalizes filter id
    private int normalizeFilterId(Integer id, String fieldName) {
        if (id == null) {
            return DEFAULT_FILTER_ID;
        }

        if (id < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }

        return id;
    }

    // Normalizes article id
    private int normalizeLastArticleId(Integer lastArticleId) {
        if (lastArticleId == null) {
            return DEFAULT_LAST_ARTICLE_ID;
        }

        if (lastArticleId < 0) {
            throw new IllegalArgumentException("lastArticleId cannot be negative.");
        }

        return lastArticleId;
    }

    // Normalizes batch size
    private int normalizeBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }

        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    // Validates filter combination
    private void validateFilterCombination(
            String publicationType,
            int journalId,
            int conferenceId
    ) {
        if (journalId > 0 && conferenceId > 0) {
            throw new IllegalArgumentException(
                    "Cannot filter by journalId and conferenceId at the same time."
            );
        }

        if (journalId > 0 && publicationType.equals(PUBLICATION_TYPE_CONFERENCE)) {
            throw new IllegalArgumentException(
                    "Cannot use publicationType = CONFERENCE with a journalId filter."
            );
        }

        if (conferenceId > 0 && publicationType.equals(PUBLICATION_TYPE_JOURNAL)) {
            throw new IllegalArgumentException(
                    "Cannot use publicationType = JOURNAL with a conferenceId filter."
            );
        }
    }

    // Converts null value
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    public record YearPageData(
            int year,
            String publicationType,
            YearProfileDto profile,
            long expectedPublicationCount
    ) {
        // Checks profile exists
        public boolean hasProfile() {
            return profile != null;
        }
    }
}