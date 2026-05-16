package integration;

import db.DatabaseManager;
import dto.author.AuthorSearchResultDto;
import dto.conference.ConferenceSearchResultDto;
import dto.journal.JournalSearchResultDto;
import dto.year.AvailableYearDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.AuthorService;
import service.ConferenceService;
import service.JournalService;
import service.YearService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServiceFlowIntegrationTest {

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
        List<JournalSearchResultDto> journals =
                journalService.searchJournals("data", 5);

        assertFalse(journals.isEmpty(), "Δεν βρέθηκαν journals για το test search.");

        int journalId = journals.get(0).journalId();

        assertNotNull(journalService.getJournalProfile(journalId, null, null));
        assertNotNull(journalService.getJournalRanking(journalId));
        assertNotNull(journalService.getJournalYearlyStats(journalId, null, null));
        assertNotNull(journalService.getJournalArticles(journalId, null, null));

        assertDoesNotThrow(() -> journalService.getJournalProfile(journalId, 2010, 2023));
    }

    @Test
    void conferenceServiceFlowShouldWork() {
        List<ConferenceSearchResultDto> conferences =
                conferenceService.searchConferences("data", 5);

        assertFalse(conferences.isEmpty(), "Δεν βρέθηκαν conferences για το test search.");

        int conferenceId = conferences.get(0).conferenceId();

        assertNotNull(conferenceService.getConferenceProfile(conferenceId, null, null));
        assertNotNull(conferenceService.getConferenceRanking(conferenceId));
        assertNotNull(conferenceService.getConferenceYearlyStats(conferenceId, null, null));
        assertNotNull(conferenceService.getConferenceArticles(conferenceId, null, null));

        assertDoesNotThrow(() -> conferenceService.getConferenceProfile(conferenceId, 2010, 2023));
    }

    @Test
    void authorServiceFlowShouldWork() {
        List<AuthorSearchResultDto> authors =
                authorService.searchAuthors("a", 5);

        assertFalse(authors.isEmpty(), "Δεν βρέθηκαν authors για το test search.");

        int authorId = authors.get(0).authorId();

        assertNotNull(authorService.getAuthorProfile(authorId, null, null));
        assertNotNull(authorService.getAuthorYearlyStats(authorId, null, null));
        assertNotNull(authorService.getAuthorYearlyStatsByType(authorId, null, null));
        assertNotNull(authorService.getAuthorPublications(authorId, null, null));

        assertDoesNotThrow(() -> authorService.getAuthorProfile(authorId, 2010, 2023));
    }

    @Test
    void yearServiceFlowShouldWork() {
        List<AvailableYearDto> years = yearService.getAvailableYears();

        assertFalse(years.isEmpty(), "Δεν βρέθηκαν διαθέσιμες χρονιές.");

        int year = years.get(0).year();

        assertNotNull(yearService.getYearProfile(year));
        assertNotNull(yearService.getAllYearPublications(year));
        assertNotNull(yearService.getYearJournalPublications(year));
        assertNotNull(yearService.getYearConferencePublications(year));

        assertDoesNotThrow(() -> yearService.getYearPublications(
                year,
                "ALL",
                null,
                null,
                null
        ));
    }
}