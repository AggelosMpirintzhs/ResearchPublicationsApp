package integration;

import db.DatabaseManager;
import dto.author.AuthorSearchResultDto;
import dto.conference.ConferenceSearchResultDto;
import dto.journal.JournalSearchResultDto;
import dto.year.AvailableYearDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.AuthorService;
import service.ConferenceService;
import service.JournalService;
import service.YearService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceFlowIntegrationTest {

    private static JournalService journalService;
    private static ConferenceService conferenceService;
    private static AuthorService authorService;
    private static YearService yearService;

    @BeforeAll
    static void setUp() {
        DatabaseManager.initialize();

        journalService = new JournalService();
        conferenceService = new ConferenceService();
        authorService = new AuthorService();
        yearService = new YearService();
    }

    @AfterAll
    static void tearDown() {
        DatabaseManager.shutdown();
    }

    @Test
    void journalServiceFlowShouldWork() {
        List<JournalSearchResultDto> journals = findJournalsForTest();

        Assumptions.assumeFalse(
                journals.isEmpty(),
                "Δεν βρέθηκαν journals για integration test."
        );

        Integer journalId = journals.get(0).journalId();

        assertNotNull(journalId);

        assertDoesNotThrow(() -> {
            assertNotNull(journalService.getJournalProfile(journalId, null, null));
            assertNotNull(journalService.getJournalRanking(journalId));
            assertNotNull(journalService.getJournalYearlyStats(journalId, null, null));
            assertNotNull(journalService.getJournalArticles(journalId, null, null));
        });

        assertDoesNotThrow(() ->
                assertNotNull(journalService.getJournalProfile(journalId, 2010, 2023))
        );
    }

    @Test
    void conferenceServiceFlowShouldWork() {
        List<ConferenceSearchResultDto> conferences = findConferencesForTest();

        Assumptions.assumeFalse(
                conferences.isEmpty(),
                "Δεν βρέθηκαν conferences για integration test."
        );

        Integer conferenceId = conferences.get(0).conferenceId();

        assertNotNull(conferenceId);

        assertDoesNotThrow(() -> {
            assertNotNull(conferenceService.getConferenceProfile(conferenceId, null, null));
            assertNotNull(conferenceService.getConferenceRanking(conferenceId));
            assertNotNull(conferenceService.getConferenceYearlyStats(conferenceId, null, null));
            assertNotNull(conferenceService.getConferenceArticles(conferenceId, null, null));
        });

        assertDoesNotThrow(() ->
                assertNotNull(conferenceService.getConferenceProfile(conferenceId, 2010, 2023))
        );
    }

    @Test
    void authorServiceFlowShouldWork() {
        List<AuthorSearchResultDto> authors = findAuthorsForTest();

        Assumptions.assumeFalse(
                authors.isEmpty(),
                "Δεν βρέθηκαν authors για integration test."
        );

        Integer authorId = authors.get(0).authorId();

        assertNotNull(authorId);

        assertDoesNotThrow(() -> {
            assertNotNull(authorService.getAuthorProfile(authorId, null, null));
            assertNotNull(authorService.getAuthorYearlyStats(authorId, null, null));
            assertNotNull(authorService.getAuthorYearlyStatsByType(authorId, null, null));

            assertNotNull(authorService.getAuthorPublicationsBatch(
                    authorId,
                    null,
                    null,
                    null,
                    null
            ));
        });

        assertDoesNotThrow(() ->
                assertNotNull(authorService.getAuthorProfile(authorId, 2010, 2023))
        );
    }

    @Test
    void yearServiceFlowShouldWork() {
        List<AvailableYearDto> years = yearService.getAvailableYears();

        Assumptions.assumeFalse(
                years.isEmpty(),
                "Δεν βρέθηκαν διαθέσιμες χρονιές για integration test."
        );

        Integer year = years.get(0).year();

        assertNotNull(year);

        assertDoesNotThrow(() -> {
            assertNotNull(yearService.getYearProfile(year));
            assertNotNull(yearService.getAllYearPublications(year));
            assertNotNull(yearService.getYearJournalPublications(year));
            assertNotNull(yearService.getYearConferencePublications(year));
        });

        assertDoesNotThrow(() ->
                assertNotNull(yearService.getYearPublications(
                        year,
                        "ALL",
                        null,
                        null,
                        null
                ))
        );
    }

    private static List<JournalSearchResultDto> findJournalsForTest() {
        List<JournalSearchResultDto> journals = journalService.searchJournals("data", 5);

        if (!journals.isEmpty()) {
            return journals;
        }

        journals = journalService.searchJournals("journal", 5);

        if (!journals.isEmpty()) {
            return journals;
        }

        return journalService.searchJournals("a", 5);
    }

    private static List<ConferenceSearchResultDto> findConferencesForTest() {
        List<ConferenceSearchResultDto> conferences = conferenceService.searchConferences("data", 5);

        if (!conferences.isEmpty()) {
            return conferences;
        }

        conferences = conferenceService.searchConferences("conference", 5);

        if (!conferences.isEmpty()) {
            return conferences;
        }

        return conferenceService.searchConferences("a", 5);
    }

    private static List<AuthorSearchResultDto> findAuthorsForTest() {
        List<AuthorSearchResultDto> authors = authorService.searchAuthors("a", 5);

        if (!authors.isEmpty()) {
            return authors;
        }

        authors = authorService.searchAuthors("e", 5);

        if (!authors.isEmpty()) {
            return authors;
        }

        return authorService.searchAuthors("i", 5);
    }
}