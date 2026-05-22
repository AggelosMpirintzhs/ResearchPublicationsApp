package controllers;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorControllerTest {

    private AuthorController authorController;

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
        authorController = new AuthorController();
    }

    @Test
    void buildYearList_usesMinimumYearAndReturnsDescendingYears() throws Exception {
        int currentYear = LocalDate.now().getYear();
        int minimumYear = currentYear - 3;

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) invokePrivate(
                "buildYearList",
                new Class<?>[]{Integer.class},
                minimumYear
        );

        assertEquals(4, result.size());
        assertEquals(currentYear, result.get(0));
        assertEquals(currentYear - 1, result.get(1));
        assertEquals(currentYear - 2, result.get(2));
        assertEquals(minimumYear, result.get(3));
    }

    @Test
    void buildYearList_usesDefaultMinYear_whenMinimumYearIsNull() throws Exception {
        int currentYear = LocalDate.now().getYear();

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) invokePrivate(
                "buildYearList",
                new Class<?>[]{Integer.class},
                (Object) null
        );

        assertFalse(result.isEmpty());
        assertEquals(currentYear, result.get(0));
        assertEquals(1900, result.get(result.size() - 1));
    }

    @Test
    void getAuthorDisplayName_returnsExpectedNames() throws Exception {
        AuthorSearchResultDto namedAuthor =
                authorSearchResult(1, "Maria Papadopoulou");

        AuthorSearchResultDto authorWithoutName =
                authorSearchResult(2, "");

        assertEquals(
                "Unknown author",
                invokePrivate(
                        "getAuthorDisplayName",
                        new Class<?>[]{AuthorSearchResultDto.class},
                        (Object) null
                )
        );

        assertEquals(
                "Maria Papadopoulou",
                invokePrivate(
                        "getAuthorDisplayName",
                        new Class<?>[]{AuthorSearchResultDto.class},
                        namedAuthor
                )
        );

        assertEquals(
                "Author #2",
                invokePrivate(
                        "getAuthorDisplayName",
                        new Class<?>[]{AuthorSearchResultDto.class},
                        authorWithoutName
                )
        );
    }

    @Test
    void buildVenueDisplayName_returnsExpectedVenueNames() throws Exception {
        assertEquals(
                "-",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        (Object) null
                )
        );

        assertEquals(
                "Data Journal",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        publication(
                                "Journal",
                                "Title",
                                "Data Journal",
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "VLDB - Very Large Data Bases",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        publication(
                                "Conference",
                                "Title",
                                null,
                                "VLDB",
                                "Very Large Data Bases",
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "SIGMOD",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        publication(
                                "Conference",
                                "Title",
                                null,
                                "SIGMOD",
                                "",
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "International Database Conference",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        publication(
                                "Conference",
                                "Title",
                                null,
                                "",
                                "International Database Conference",
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals(
                "-",
                invokePrivate(
                        "buildVenueDisplayName",
                        new Class<?>[]{AuthorPublicationDto.class},
                        publication(
                                "Unknown",
                                "Title",
                                null,
                                null,
                                null,
                                null,
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

        assertNull(
                invokePrivate(
                        "firstNonBlank",
                        new Class<?>[]{String.class, String.class},
                        "   ",
                        null
                )
        );

        assertEquals(
                0L,
                invokePrivate(
                        "nullToZero",
                        new Class<?>[]{Long.class},
                        (Object) null
                )
        );

        assertEquals(
                7L,
                invokePrivate(
                        "nullToZero",
                        new Class<?>[]{Long.class},
                        7L
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
    void updatePublicationsLoadedLabel_setsExpectedText() throws Exception {
        runOnFxThread(() -> {
            Label label = new Label();
            setPrivateField("publicationsLoadedLabel", label);

            invokePrivate(
                    "updatePublicationsLoadedLabel",
                    new Class<?>[]{long.class, long.class},
                    25L,
                    100L
            );

            assertEquals("Articles Loaded: 25 / 100", label.getText());
        });
    }

    @Test
    void setSelectedAuthor_updatesSelectedAuthorLabel() throws Exception {
        runOnFxThread(() -> {
            Label label = new Label();
            setPrivateField("selectedAuthorLabel", label);

            invokePrivate(
                    "setSelectedAuthor",
                    new Class<?>[]{AuthorSearchResultDto.class},
                    (Object) null
            );

            assertEquals("No author selected", label.getText());

            invokePrivate(
                    "setSelectedAuthor",
                    new Class<?>[]{AuthorSearchResultDto.class},
                    authorSearchResult(5, "John Smith")
            );

            assertEquals("John Smith", label.getText());
        });
    }

    @Test
    void setupLineChart_configuresChartAndAxes() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

            invokePrivate(
                    "setupLineChart",
                    new Class<?>[]{LineChart.class, NumberAxis.class, NumberAxis.class},
                    chart,
                    xAxis,
                    yAxis
            );

            assertFalse(chart.getAnimated());
            assertFalse(chart.getCreateSymbols());
            assertTrue(chart.isLegendVisible());

            assertFalse(xAxis.isAutoRanging());
            assertFalse(xAxis.isForceZeroInRange());
            assertFalse(xAxis.isMinorTickVisible());

            assertFalse(yAxis.isAutoRanging());
            assertTrue(yAxis.isForceZeroInRange());
            assertFalse(yAxis.isMinorTickVisible());
        });
    }

    @Test
    void configureYearAxis_expandsSingleYearRange() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureYearAxis",
                    new Class<?>[]{NumberAxis.class, Integer.class, Integer.class},
                    axis,
                    2020,
                    2020
            );

            assertFalse(axis.isAutoRanging());
            assertEquals(2019.0, axis.getLowerBound());
            assertEquals(2021.0, axis.getUpperBound());
            assertEquals(1.0, axis.getTickUnit());
        });
    }

    @Test
    void configureYearAxis_usesAutoRanging_whenYearsAreNull() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureYearAxis",
                    new Class<?>[]{NumberAxis.class, Integer.class, Integer.class},
                    axis,
                    null,
                    null
            );

            assertTrue(axis.isAutoRanging());
        });
    }

    @Test
    void configureValueAxis_setsNiceBounds() throws Exception {
        runOnFxThread(() -> {
            NumberAxis axis = new NumberAxis();

            invokePrivate(
                    "configureValueAxis",
                    new Class<?>[]{NumberAxis.class, double.class},
                    axis,
                    47.0
            );

            assertFalse(axis.isAutoRanging());
            assertEquals(0.0, axis.getLowerBound());
            assertTrue(axis.getUpperBound() >= 47.0);
            assertTrue(axis.getTickUnit() > 0);
        });
    }

    @Test
    void updateProfileLabels_setsAllProfileLabels() throws Exception {
        runOnFxThread(() -> {
            Label firstYearLabel = new Label();
            Label lastYearLabel = new Label();
            Label activeYearsLabel = new Label();
            Label totalArticlesLabel = new Label();
            Label totalJournalArticlesLabel = new Label();
            Label totalConferenceArticlesLabel = new Label();
            Label distinctJournalsLabel = new Label();
            Label distinctConferencesLabel = new Label();
            Label avgArticlesPerYearLabel = new Label();

            setPrivateField("firstYearLabel", firstYearLabel);
            setPrivateField("lastYearLabel", lastYearLabel);
            setPrivateField("activeYearsLabel", activeYearsLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalJournalArticlesLabel", totalJournalArticlesLabel);
            setPrivateField("totalConferenceArticlesLabel", totalConferenceArticlesLabel);
            setPrivateField("distinctJournalsLabel", distinctJournalsLabel);
            setPrivateField("distinctConferencesLabel", distinctConferencesLabel);
            setPrivateField("avgArticlesPerYearLabel", avgArticlesPerYearLabel);

            AuthorProfileDto profile =
                    authorProfile(
                            2010,
                            2020,
                            11L,
                            100L,
                            60L,
                            40L,
                            8L,
                            5L,
                            9.091
                    );

            invokePrivate(
                    "updateProfileLabels",
                    new Class<?>[]{AuthorProfileDto.class},
                    profile
            );

            assertEquals("2010", firstYearLabel.getText());
            assertEquals("2020", lastYearLabel.getText());
            assertEquals("11", activeYearsLabel.getText());
            assertEquals("100", totalArticlesLabel.getText());
            assertEquals("60", totalJournalArticlesLabel.getText());
            assertEquals("40", totalConferenceArticlesLabel.getText());
            assertEquals("8", distinctJournalsLabel.getText());
            assertEquals("5", distinctConferencesLabel.getText());
            assertEquals(String.format("%.2f", 9.091), avgArticlesPerYearLabel.getText());
        });
    }

    @Test
    void updateTotalArticlesChart_buildsSeriesAndSkipsRowsWithoutYear() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

            setPrivateField("totalArticlesLineChart", chart);
            setPrivateField("totalArticlesYearAxis", xAxis);
            setPrivateField("totalArticlesCountAxis", yAxis);

            List<AuthorYearlyStatsDto> yearlyStats =
                    List.of(
                            authorYearlyStats(2020, 10L),
                            authorYearlyStats(null, 99L),
                            authorYearlyStats(2021, null)
                    );

            invokePrivate(
                    "updateTotalArticlesChart",
                    new Class<?>[]{List.class},
                    yearlyStats
            );

            assertEquals(1, chart.getData().size());
            assertEquals("Total articles", chart.getData().get(0).getName());
            assertEquals(2, chart.getData().get(0).getData().size());

            assertEquals(2020, chart.getData().get(0).getData().get(0).getXValue().intValue());
            assertEquals(10L, chart.getData().get(0).getData().get(0).getYValue().longValue());

            assertEquals(2021, chart.getData().get(0).getData().get(1).getXValue().intValue());
            assertEquals(0L, chart.getData().get(0).getData().get(1).getYValue().longValue());

            assertFalse(xAxis.isAutoRanging());
            assertEquals(2020.0, xAxis.getLowerBound());
            assertEquals(2021.0, xAxis.getUpperBound());
            assertTrue(yAxis.getUpperBound() >= 10.0);
        });
    }

    @Test
    void updatePublicationTypeChart_buildsJournalAndConferenceSeries() throws Exception {
        runOnFxThread(() -> {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

            setPrivateField("publicationTypeLineChart", chart);
            setPrivateField("publicationTypeYearAxis", xAxis);
            setPrivateField("publicationTypeCountAxis", yAxis);

            List<AuthorYearlyStatsByTypeDto> yearlyStats =
                    List.of(
                            authorYearlyStatsByType(2020, 5L, 7L),
                            authorYearlyStatsByType(null, 99L, 99L),
                            authorYearlyStatsByType(2021, null, 3L)
                    );

            invokePrivate(
                    "updatePublicationTypeChart",
                    new Class<?>[]{List.class},
                    yearlyStats
            );

            assertEquals(2, chart.getData().size());

            assertEquals("Journal articles", chart.getData().get(0).getName());
            assertEquals("Conference articles", chart.getData().get(1).getName());

            assertEquals(2, chart.getData().get(0).getData().size());
            assertEquals(2, chart.getData().get(1).getData().size());

            assertEquals(2020, chart.getData().get(0).getData().get(0).getXValue().intValue());
            assertEquals(5L, chart.getData().get(0).getData().get(0).getYValue().longValue());

            assertEquals(2021, chart.getData().get(0).getData().get(1).getXValue().intValue());
            assertEquals(0L, chart.getData().get(0).getData().get(1).getYValue().longValue());

            assertEquals(2021, chart.getData().get(1).getData().get(1).getXValue().intValue());
            assertEquals(3L, chart.getData().get(1).getData().get(1).getYValue().longValue());

            assertFalse(xAxis.isAutoRanging());
            assertTrue(yAxis.getUpperBound() >= 7.0);
        });
    }

    @Test
    void getSelectedYearRange_returnsSelectedYears() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();

            fromYearComboBox.setValue(2010);
            toYearComboBox.setValue(2020);

            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);

            Object result = invokePrivate(
                    "getSelectedYearRange",
                    new Class<?>[]{}
            );

            assertEquals(2010, invokeNoArgMethod(result, "startYear"));
            assertEquals(2020, invokeNoArgMethod(result, "endYear"));
        });
    }

    @Test
    void getSelectedYearRange_throwsException_whenEndYearIsBeforeStartYear() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();

            fromYearComboBox.setValue(2020);
            toYearComboBox.setValue(2010);

            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> invokePrivate(
                            "getSelectedYearRange",
                            new Class<?>[]{}
                    )
            );
        });
    }

    @Test
    void refreshToYearOptions_keepsCurrentToYear_whenItIsStillValid() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            toYearComboBox.getItems().setAll(2024, 2023, 2022, 2021, 2020);
            toYearComboBox.setValue(2022);

            setPrivateField("toYearComboBox", toYearComboBox);

            invokePrivate(
                    "refreshToYearOptions",
                    new Class<?>[]{Integer.class},
                    2020
            );

            assertEquals(2022, toYearComboBox.getValue());
            assertTrue(toYearComboBox.getItems().contains(2020));
            assertFalse(toYearComboBox.getItems().contains(2019));
        });
    }

    @Test
    void refreshToYearOptions_removesInvalidToYearFromAvailableOptions_whenCurrentToYearIsBeforeFromYear() throws Exception {
        runOnFxThread(() -> {
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            toYearComboBox.getItems().setAll(2024, 2023, 2022, 2021, 2020);
            toYearComboBox.setValue(2020);

            setPrivateField("toYearComboBox", toYearComboBox);

            invokePrivate(
                    "refreshToYearOptions",
                    new Class<?>[]{Integer.class},
                    2022
            );

            assertFalse(toYearComboBox.getItems().contains(2020));
            assertTrue(toYearComboBox.getItems().contains(2022));
            assertTrue(toYearComboBox.getItems().contains(2023));
            assertTrue(toYearComboBox.getItems().contains(2024));
        });
    }

    @Test
    void visibilityMethodsUpdateVisibleAndManagedFlags() throws Exception {
        runOnFxThread(() -> {
            VBox authorResultsContainer = new VBox();
            VBox publicationReportPanel = new VBox();

            setPrivateField("authorResultsContainer", authorResultsContainer);
            setPrivateField("publicationReportPanel", publicationReportPanel);

            invokePrivate(
                    "setAuthorResultsVisible",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(authorResultsContainer.isVisible());
            assertTrue(authorResultsContainer.isManaged());

            invokePrivate(
                    "setAuthorResultsVisible",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(authorResultsContainer.isVisible());
            assertFalse(authorResultsContainer.isManaged());

            invokePrivate(
                    "setPublicationReportVisible",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(publicationReportPanel.isVisible());
            assertTrue(publicationReportPanel.isManaged());

            invokePrivate(
                    "setPublicationReportVisible",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(publicationReportPanel.isVisible());
            assertFalse(publicationReportPanel.isManaged());
        });
    }

    @Test
    void setLoading_disablesAndEnablesControls() throws Exception {
        runOnFxThread(() -> {
            TextField authorSearchField = new TextField();
            ListView<AuthorSearchResultDto> authorResultsListView = new ListView<>();
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            CheckBox loadPublicationsCheckBox = new CheckBox();
            Button loadProfileButton = new Button();
            Button clearButton = new Button();

            setPrivateField("authorSearchField", authorSearchField);
            setPrivateField("authorResultsListView", authorResultsListView);
            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);
            setPrivateField("loadPublicationsCheckBox", loadPublicationsCheckBox);
            setPrivateField("loadProfileButton", loadProfileButton);
            setPrivateField("clearButton", clearButton);

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(authorSearchField.isDisabled());
            assertTrue(authorResultsListView.isDisabled());
            assertTrue(fromYearComboBox.isDisabled());
            assertTrue(toYearComboBox.isDisabled());
            assertTrue(loadPublicationsCheckBox.isDisabled());
            assertTrue(loadProfileButton.isDisabled());
            assertTrue(clearButton.isDisabled());

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(authorSearchField.isDisabled());
            assertFalse(authorResultsListView.isDisabled());
            assertFalse(fromYearComboBox.isDisabled());
            assertFalse(toYearComboBox.isDisabled());
            assertFalse(loadPublicationsCheckBox.isDisabled());
            assertFalse(loadProfileButton.isDisabled());
            assertFalse(clearButton.isDisabled());
        });
    }

    private Object invokePrivate(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = AuthorController.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        try {
            return method.invoke(authorController, arguments);
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

    private Object invokeNoArgMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);

        try {
            return method.invoke(target);
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
        Field field = AuthorController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(authorController, value);
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

    private AuthorSearchResultDto authorSearchResult(
            Integer authorId,
            String authorName
    ) {
        AuthorSearchResultDto dto = mock(AuthorSearchResultDto.class);

        lenient().doReturn(authorId).when(dto).authorId();
        lenient().doReturn(authorName).when(dto).authorName();

        return dto;
    }

    private AuthorPublicationDto publication(
            String articleType,
            String title,
            String journalName,
            String conferenceAcronym,
            String conferenceTitle,
            String authors,
            String ee,
            String url
    ) {
        AuthorPublicationDto dto = mock(AuthorPublicationDto.class);

        lenient().doReturn(articleType).when(dto).articleType();
        lenient().doReturn(title).when(dto).title();
        lenient().doReturn(journalName).when(dto).journalName();
        lenient().doReturn(conferenceAcronym).when(dto).conferenceAcronym();
        lenient().doReturn(conferenceTitle).when(dto).conferenceTitle();
        lenient().doReturn(authors).when(dto).authors();
        lenient().doReturn(ee).when(dto).ee();
        lenient().doReturn(url).when(dto).url();

        return dto;
    }

    private AuthorProfileDto authorProfile(
            Integer firstYear,
            Integer lastYear,
            Long activeYears,
            Long totalArticles,
            Long totalJournalArticles,
            Long totalConferenceArticles,
            Long distinctJournals,
            Long distinctConferences,
            Double avgArticlesPerYear
    ) {
        AuthorProfileDto dto = mock(AuthorProfileDto.class);

        lenient().doReturn(firstYear).when(dto).firstYear();
        lenient().doReturn(lastYear).when(dto).lastYear();
        lenient().doReturn(activeYears).when(dto).activeYears();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalJournalArticles).when(dto).totalJournalArticles();
        lenient().doReturn(totalConferenceArticles).when(dto).totalConferenceArticles();
        lenient().doReturn(distinctJournals).when(dto).distinctJournals();
        lenient().doReturn(distinctConferences).when(dto).distinctConferences();
        lenient().doReturn(avgArticlesPerYear).when(dto).avgArticlesPerYear();

        return dto;
    }

    private AuthorYearlyStatsDto authorYearlyStats(
            Integer year,
            Long totalArticles
    ) {
        AuthorYearlyStatsDto dto = mock(AuthorYearlyStatsDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(totalArticles).when(dto).totalArticles();

        return dto;
    }

    private AuthorYearlyStatsByTypeDto authorYearlyStatsByType(
            Integer year,
            Long totalJournalArticles,
            Long totalConferenceArticles
    ) {
        AuthorYearlyStatsByTypeDto dto = mock(AuthorYearlyStatsByTypeDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(totalJournalArticles).when(dto).totalJournalArticles();
        lenient().doReturn(totalConferenceArticles).when(dto).totalConferenceArticles();

        return dto;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}