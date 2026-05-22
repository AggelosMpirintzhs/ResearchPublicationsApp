package controllers;

import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import service.YearService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearControllerTest {

    private YearController yearController;

    @BeforeAll
    static void startJavaFxRuntime() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException exception) {
            latch.countDown();
        }

        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("JavaFX platform did not start.");
        }
    }

    @BeforeEach
    void setUp() {
        yearController = new YearController();
    }

    @Test
    void buildYearDisplayName_returnsUnknownYear_whenYearDtoIsNullOrYearIsNull() throws Exception {
        assertEquals(
                "Unknown year",
                invokePrivate(
                        "buildYearDisplayName",
                        new Class<?>[]{AvailableYearDto.class},
                        (Object) null
                )
        );

        assertEquals(
                "Unknown year",
                invokePrivate(
                        "buildYearDisplayName",
                        new Class<?>[]{AvailableYearDto.class},
                        availableYear(null, 100L, 60L, 40L)
                )
        );
    }

    @Test
    void buildYearDisplayName_returnsYearAsText() throws Exception {
        assertEquals(
                "2020",
                invokePrivate(
                        "buildYearDisplayName",
                        new Class<?>[]{AvailableYearDto.class},
                        availableYear(2020, 100L, 60L, 40L)
                )
        );
    }

    @Test
    void getSelectedPublicationType_returnsAll_whenComboBoxIsNullOrHasNoValue() throws Exception {
        assertEquals(
                YearService.PUBLICATION_TYPE_ALL,
                invokePrivate(
                        "getSelectedPublicationType",
                        new Class<?>[]{}
                )
        );

        runOnFxThread(() -> {
            ComboBox<String> publicationTypeComboBox = new ComboBox<>();
            setPrivateField("publicationTypeComboBox", publicationTypeComboBox);

            assertEquals(
                    YearService.PUBLICATION_TYPE_ALL,
                    invokePrivate(
                            "getSelectedPublicationType",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void getSelectedPublicationType_returnsSelectedValue() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> publicationTypeComboBox = new ComboBox<>();
            publicationTypeComboBox.setValue(YearService.PUBLICATION_TYPE_JOURNAL);

            setPrivateField("publicationTypeComboBox", publicationTypeComboBox);

            assertEquals(
                    YearService.PUBLICATION_TYPE_JOURNAL,
                    invokePrivate(
                            "getSelectedPublicationType",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void setupPublicationTypeComboBox_addsPublicationTypesAndSelectsAll() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> publicationTypeComboBox = new ComboBox<>();
            setPrivateField("publicationTypeComboBox", publicationTypeComboBox);

            invokePrivate(
                    "setupPublicationTypeComboBox",
                    new Class<?>[]{}
            );

            assertEquals(3, publicationTypeComboBox.getItems().size());
            assertTrue(publicationTypeComboBox.getItems().contains(YearService.PUBLICATION_TYPE_ALL));
            assertTrue(publicationTypeComboBox.getItems().contains(YearService.PUBLICATION_TYPE_JOURNAL));
            assertTrue(publicationTypeComboBox.getItems().contains(YearService.PUBLICATION_TYPE_CONFERENCE));
            assertEquals(YearService.PUBLICATION_TYPE_ALL, publicationTypeComboBox.getValue());
        });
    }

    @Test
    void updateSelectedYearLabel_setsNoYearSelected_whenYearDtoIsNull() throws Exception {
        runOnFxThread(() -> {
            Label selectedYearLabel = new Label();
            setPrivateField("selectedYearLabel", selectedYearLabel);

            invokePrivate(
                    "updateSelectedYearLabel",
                    new Class<?>[]{AvailableYearDto.class},
                    (Object) null
            );

            assertEquals("No year selected", selectedYearLabel.getText());
        });
    }

    @Test
    void updateSelectedYearLabel_setsSelectedYearText() throws Exception {
        runOnFxThread(() -> {
            Label selectedYearLabel = new Label();
            setPrivateField("selectedYearLabel", selectedYearLabel);

            invokePrivate(
                    "updateSelectedYearLabel",
                    new Class<?>[]{AvailableYearDto.class},
                    availableYear(2020, 100L, 60L, 40L)
            );

            assertEquals("2020", selectedYearLabel.getText());
        });
    }

    @Test
    void updateFastYearPreview_setsPreviewLabels() throws Exception {
        runOnFxThread(() -> {
            Label yearLabel = new Label();
            Label totalArticlesLabel = new Label();
            Label totalJournalArticlesLabel = new Label();
            Label totalConferenceArticlesLabel = new Label();
            Label distinctJournalsLabel = new Label();
            Label distinctConferencesLabel = new Label();
            Label totalAuthorOccurrencesLabel = new Label();
            Label distinctAuthorsLabel = new Label();
            Label avgAuthorsPerArticleLabel = new Label();

            setPrivateField("yearLabel", yearLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalJournalArticlesLabel", totalJournalArticlesLabel);
            setPrivateField("totalConferenceArticlesLabel", totalConferenceArticlesLabel);
            setPrivateField("distinctJournalsLabel", distinctJournalsLabel);
            setPrivateField("distinctConferencesLabel", distinctConferencesLabel);
            setPrivateField("totalAuthorOccurrencesLabel", totalAuthorOccurrencesLabel);
            setPrivateField("distinctAuthorsLabel", distinctAuthorsLabel);
            setPrivateField("avgAuthorsPerArticleLabel", avgAuthorsPerArticleLabel);

            invokePrivate(
                    "updateFastYearPreview",
                    new Class<?>[]{AvailableYearDto.class},
                    availableYear(2020, 100L, 60L, 40L)
            );

            assertEquals("2020", yearLabel.getText());
            assertEquals("100", totalArticlesLabel.getText());
            assertEquals("60", totalJournalArticlesLabel.getText());
            assertEquals("40", totalConferenceArticlesLabel.getText());
            assertEquals("-", distinctJournalsLabel.getText());
            assertEquals("-", distinctConferencesLabel.getText());
            assertEquals("-", totalAuthorOccurrencesLabel.getText());
            assertEquals("-", distinctAuthorsLabel.getText());
            assertEquals("-", avgAuthorsPerArticleLabel.getText());
        });
    }

    @Test
    void updateFastYearPreview_doesNothing_whenYearDtoIsNull() throws Exception {
        runOnFxThread(() -> {
            Label yearLabel = new Label("unchanged");
            setPrivateField("yearLabel", yearLabel);

            invokePrivate(
                    "updateFastYearPreview",
                    new Class<?>[]{AvailableYearDto.class},
                    (Object) null
            );

            assertEquals("unchanged", yearLabel.getText());
        });
    }

    @Test
    void updateProfileLabels_setsAllProfileLabels() throws Exception {
        runOnFxThread(() -> {
            Label yearLabel = new Label();
            Label totalArticlesLabel = new Label();
            Label totalJournalArticlesLabel = new Label();
            Label totalConferenceArticlesLabel = new Label();
            Label distinctJournalsLabel = new Label();
            Label distinctConferencesLabel = new Label();
            Label totalAuthorOccurrencesLabel = new Label();
            Label distinctAuthorsLabel = new Label();
            Label avgAuthorsPerArticleLabel = new Label();

            setPrivateField("yearLabel", yearLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalJournalArticlesLabel", totalJournalArticlesLabel);
            setPrivateField("totalConferenceArticlesLabel", totalConferenceArticlesLabel);
            setPrivateField("distinctJournalsLabel", distinctJournalsLabel);
            setPrivateField("distinctConferencesLabel", distinctConferencesLabel);
            setPrivateField("totalAuthorOccurrencesLabel", totalAuthorOccurrencesLabel);
            setPrivateField("distinctAuthorsLabel", distinctAuthorsLabel);
            setPrivateField("avgAuthorsPerArticleLabel", avgAuthorsPerArticleLabel);

            YearProfileDto profile =
                    yearProfile(
                            2020,
                            100L,
                            60L,
                            40L,
                            12L,
                            8L,
                            250L,
                            90L,
                            2.50
                    );

            invokePrivate(
                    "updateProfileLabels",
                    new Class<?>[]{YearProfileDto.class},
                    profile
            );

            assertEquals("2020", yearLabel.getText());
            assertEquals("100", totalArticlesLabel.getText());
            assertEquals("60", totalJournalArticlesLabel.getText());
            assertEquals("40", totalConferenceArticlesLabel.getText());
            assertEquals("12", distinctJournalsLabel.getText());
            assertEquals("8", distinctConferencesLabel.getText());
            assertEquals("250", totalAuthorOccurrencesLabel.getText());
            assertEquals("90", distinctAuthorsLabel.getText());
            assertEquals(String.format("%.2f", 2.50), avgAuthorsPerArticleLabel.getText());
        });
    }

    @Test
    void clearResultArea_resetsLabelsAndLoadedCount() throws Exception {
        runOnFxThread(() -> {
            Label yearLabel = new Label("2020");
            Label totalArticlesLabel = new Label("100");
            Label totalJournalArticlesLabel = new Label("60");
            Label totalConferenceArticlesLabel = new Label("40");
            Label distinctJournalsLabel = new Label("12");
            Label distinctConferencesLabel = new Label("8");
            Label totalAuthorOccurrencesLabel = new Label("250");
            Label distinctAuthorsLabel = new Label("90");
            Label avgAuthorsPerArticleLabel = new Label("2.50");
            Label articlesLoadedLabel = new Label();

            setPrivateField("yearLabel", yearLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalJournalArticlesLabel", totalJournalArticlesLabel);
            setPrivateField("totalConferenceArticlesLabel", totalConferenceArticlesLabel);
            setPrivateField("distinctJournalsLabel", distinctJournalsLabel);
            setPrivateField("distinctConferencesLabel", distinctConferencesLabel);
            setPrivateField("totalAuthorOccurrencesLabel", totalAuthorOccurrencesLabel);
            setPrivateField("distinctAuthorsLabel", distinctAuthorsLabel);
            setPrivateField("avgAuthorsPerArticleLabel", avgAuthorsPerArticleLabel);
            setPrivateField("articlesLoadedLabel", articlesLoadedLabel);

            invokePrivate(
                    "clearResultArea",
                    new Class<?>[]{}
            );

            assertEquals("-", yearLabel.getText());
            assertEquals("-", totalArticlesLabel.getText());
            assertEquals("-", totalJournalArticlesLabel.getText());
            assertEquals("-", totalConferenceArticlesLabel.getText());
            assertEquals("-", distinctJournalsLabel.getText());
            assertEquals("-", distinctConferencesLabel.getText());
            assertEquals("-", totalAuthorOccurrencesLabel.getText());
            assertEquals("-", distinctAuthorsLabel.getText());
            assertEquals("-", avgAuthorsPerArticleLabel.getText());
            assertEquals("Articles Loaded: 0 / 0", articlesLoadedLabel.getText());
        });
    }

    @Test
    void buildVenueDisplayName_returnsExpectedVenueNames() throws Exception {
        assertEquals(
                "-",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        (Object) null
                )
        );

        assertEquals(
                "Data Journal",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        yearPublication(
                                2020,
                                "Journal",
                                "Title",
                                "Data Journal",
                                null,
                                null,
                                "Alice",
                                "10-20",
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "VLDB - Very Large Data Bases",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        yearPublication(
                                2020,
                                "Conference",
                                "Title",
                                null,
                                "VLDB",
                                "Very Large Data Bases",
                                "Alice",
                                "10-20",
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "SIGMOD",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        yearPublication(
                                2020,
                                "Conference",
                                "Title",
                                null,
                                "SIGMOD",
                                "",
                                "Alice",
                                "10-20",
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "International Database Conference",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        yearPublication(
                                2020,
                                "Conference",
                                "Title",
                                null,
                                "",
                                "International Database Conference",
                                "Alice",
                                "10-20",
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{YearPublicationDto.class},
                        yearPublication(
                                2020,
                                "Unknown",
                                "Title",
                                null,
                                null,
                                null,
                                "Alice",
                                "10-20",
                                null,
                                null
                        )
                )
        );
    }

    @Test
    void helperFormattingMethodsReturnExpectedValues() throws Exception {
        assertEquals(
                "first",
                invokePrivate(
                        "firstNonBlank",
                        new Class<?>[]{String.class, String.class},
                        "first",
                        "second"
                )
        );

        assertEquals(
                "second",
                invokePrivate(
                        "firstNonBlank",
                        new Class<?>[]{String.class, String.class},
                        "   ",
                        "second"
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "firstNonBlank",
                        new Class<?>[]{String.class, String.class},
                        "   ",
                        null
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "nullToDash",
                        new Class<?>[]{Object.class},
                        (Object) null
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "nullToDash",
                        new Class<?>[]{Object.class},
                        "   "
                )
        );

        assertEquals(
                "abc",
                invokePrivate(
                        "nullToDash",
                        new Class<?>[]{Object.class},
                        "abc"
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "formatDouble",
                        new Class<?>[]{Double.class},
                        (Object) null
                )
        );

        assertEquals(
                String.format("%.2f", 2.345),
                invokePrivate(
                        "formatDouble",
                        new Class<?>[]{Double.class},
                        2.345
                )
        );
    }

    @Test
    void setLabelText_setsDashForNullAndValueForNonNull() throws Exception {
        runOnFxThread(() -> {
            Label label = new Label();

            invokePrivate(
                    "setLabelText",
                    new Class<?>[]{Label.class, Object.class},
                    label,
                    null
            );

            assertEquals("-", label.getText());

            invokePrivate(
                    "setLabelText",
                    new Class<?>[]{Label.class, Object.class},
                    label,
                    123
            );

            assertEquals("123", label.getText());
        });
    }

    @Test
    void updateArticlesLoadedLabel_setsExpectedText() throws Exception {
        runOnFxThread(() -> {
            Label articlesLoadedLabel = new Label();
            setPrivateField("articlesLoadedLabel", articlesLoadedLabel);

            invokePrivate(
                    "updateArticlesLoadedLabel",
                    new Class<?>[]{long.class, long.class},
                    25L,
                    100L
            );

            assertEquals("Articles Loaded: 25 / 100", articlesLoadedLabel.getText());
        });
    }

    @Test
    void setPublicationLoadingText_setsExpectedText() throws Exception {
        runOnFxThread(() -> {
            Label publicationLoadingLabel = new Label();
            setPrivateField("publicationLoadingLabel", publicationLoadingLabel);

            invokePrivate(
                    "setPublicationLoadingText",
                    new Class<?>[]{String.class},
                    "Loading first publications..."
            );

            assertEquals("Loading first publications...", publicationLoadingLabel.getText());
        });
    }

    @Test
    void updatePublicationReportVisibility_usesLoadPublicationsCheckBoxState() throws Exception {
        runOnFxThread(() -> {
            CheckBox loadPublicationsCheckBox = new CheckBox();
            VBox publicationReportPanel = new VBox();

            setPrivateField("loadPublicationsCheckBox", loadPublicationsCheckBox);
            setPrivateField("publicationReportPanel", publicationReportPanel);

            loadPublicationsCheckBox.setSelected(true);

            invokePrivate(
                    "updatePublicationReportVisibility",
                    new Class<?>[]{}
            );

            assertTrue(publicationReportPanel.isVisible());
            assertTrue(publicationReportPanel.isManaged());

            loadPublicationsCheckBox.setSelected(false);

            invokePrivate(
                    "updatePublicationReportVisibility",
                    new Class<?>[]{}
            );

            assertFalse(publicationReportPanel.isVisible());
            assertFalse(publicationReportPanel.isManaged());
        });
    }

    @Test
    void setYearLoading_disablesAndEnablesYearControls() throws Exception {
        runOnFxThread(() -> {
            ComboBox<AvailableYearDto> yearComboBox = new ComboBox<>();
            Button refreshYearsButton = new Button();

            setPrivateField("yearComboBox", yearComboBox);
            setPrivateField("refreshYearsButton", refreshYearsButton);

            invokePrivate(
                    "setYearLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(yearComboBox.isDisabled());
            assertTrue(refreshYearsButton.isDisabled());

            invokePrivate(
                    "setYearLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(yearComboBox.isDisabled());
            assertFalse(refreshYearsButton.isDisabled());
        });
    }

    @Test
    void setLoading_disablesAndEnablesMainControls() throws Exception {
        runOnFxThread(() -> {
            ComboBox<AvailableYearDto> yearComboBox = new ComboBox<>();
            ComboBox<String> publicationTypeComboBox = new ComboBox<>();
            CheckBox loadPublicationsCheckBox = new CheckBox();
            Button loadYearButton = new Button();
            Button refreshYearsButton = new Button();

            setPrivateField("yearComboBox", yearComboBox);
            setPrivateField("publicationTypeComboBox", publicationTypeComboBox);
            setPrivateField("loadPublicationsCheckBox", loadPublicationsCheckBox);
            setPrivateField("loadYearButton", loadYearButton);
            setPrivateField("refreshYearsButton", refreshYearsButton);

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(yearComboBox.isDisabled());
            assertTrue(publicationTypeComboBox.isDisabled());
            assertTrue(loadPublicationsCheckBox.isDisabled());
            assertTrue(loadYearButton.isDisabled());
            assertTrue(refreshYearsButton.isDisabled());

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(yearComboBox.isDisabled());
            assertFalse(publicationTypeComboBox.isDisabled());
            assertFalse(loadPublicationsCheckBox.isDisabled());
            assertFalse(loadYearButton.isDisabled());
            assertFalse(refreshYearsButton.isDisabled());
        });
    }

    private Object invokePrivate(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = YearController.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        try {
            return method.invoke(yearController, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw exception;
        }
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = YearController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(yearController, value);
    }

    private static void runOnFxThread(ThrowingRunnable action) throws Exception {
        callOnFxThread(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T callOnFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        FutureTask<T> futureTask = new FutureTask<>(action);
        Platform.runLater(futureTask);

        try {
            return futureTask.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof Exception checkedException) {
                throw checkedException;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new RuntimeException(cause);
        }
    }

    private AvailableYearDto availableYear(
            Integer year,
            Long totalArticles,
            Long totalJournalArticles,
            Long totalConferenceArticles
    ) {
        AvailableYearDto dto = mock(AvailableYearDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalJournalArticles).when(dto).totalJournalArticles();
        lenient().doReturn(totalConferenceArticles).when(dto).totalConferenceArticles();

        return dto;
    }

    private YearProfileDto yearProfile(
            Integer year,
            Long totalArticles,
            Long totalJournalArticles,
            Long totalConferenceArticles,
            Long distinctJournals,
            Long distinctConferences,
            Long totalAuthorOccurrences,
            Long distinctAuthors,
            Double avgAuthorsPerArticle
    ) {
        YearProfileDto dto = mock(YearProfileDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalJournalArticles).when(dto).totalJournalArticles();
        lenient().doReturn(totalConferenceArticles).when(dto).totalConferenceArticles();
        lenient().doReturn(distinctJournals).when(dto).distinctJournals();
        lenient().doReturn(distinctConferences).when(dto).distinctConferences();
        lenient().doReturn(totalAuthorOccurrences).when(dto).totalAuthorOccurrences();
        lenient().doReturn(distinctAuthors).when(dto).distinctAuthors();
        lenient().doReturn(avgAuthorsPerArticle).when(dto).avgAuthorsPerArticle();

        return dto;
    }

    private YearPublicationDto yearPublication(
            Integer year,
            String articleType,
            String title,
            String journalName,
            String conferenceAcronym,
            String conferenceTitle,
            String authors,
            String pages,
            String ee,
            String url
    ) {
        YearPublicationDto dto = mock(YearPublicationDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(articleType).when(dto).articleType();
        lenient().doReturn(title).when(dto).title();
        lenient().doReturn(journalName).when(dto).journalName();
        lenient().doReturn(conferenceAcronym).when(dto).conferenceAcronym();
        lenient().doReturn(conferenceTitle).when(dto).conferenceTitle();
        lenient().doReturn(authors).when(dto).authors();
        lenient().doReturn(pages).when(dto).pages();
        lenient().doReturn(ee).when(dto).ee();
        lenient().doReturn(url).when(dto).url();

        return dto;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}