package controllers;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalYearlyStatsDto;
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
class VenueControllerTest {

    private VenueController venueController;

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
        venueController = new VenueController();
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
            assertFalse(chart.isLegendVisible());

            assertFalse(xAxis.isAutoRanging());
            assertFalse(xAxis.isForceZeroInRange());
            assertEquals(0.0, xAxis.getTickLabelRotation());

            assertFalse(yAxis.isAutoRanging());
            assertTrue(yAxis.isForceZeroInRange());
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
                    new Class<?>[]{NumberAxis.class, long.class},
                    axis,
                    47L
            );

            assertFalse(axis.isAutoRanging());
            assertEquals(0.0, axis.getLowerBound());
            assertTrue(axis.getUpperBound() >= 47.0);
            assertTrue(axis.getTickUnit() > 0);
        });
    }

    @Test
    void calculateYearTickUnit_returnsExpectedValues() throws Exception {
        assertEquals(
                1,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        10
                )
        );

        assertEquals(
                5,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        25
                )
        );

        assertEquals(
                10,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        60
                )
        );

        assertEquals(
                20,
                invokePrivate(
                        "calculateYearTickUnit",
                        new Class<?>[]{int.class},
                        100
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
                        "defaultLong",
                        new Class<?>[]{Long.class},
                        (Object) null
                )
        );

        assertEquals(
                7L,
                invokePrivate(
                        "defaultLong",
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
    void updateArticlesLoadedLabel_setsExpectedText() throws Exception {
        runOnFxThread(() -> {
            Label label = new Label();
            setPrivateField("articlesLoadedLabel", label);

            invokePrivate(
                    "updateArticlesLoadedLabel",
                    new Class<?>[]{long.class, long.class},
                    25L,
                    100L
            );

            assertEquals("Articles Loaded: 25 / 100", label.getText());
        });
    }

    @Test
    void getSelectedVenueType_returnsComboBoxValue() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            venueTypeComboBox.setValue("Journal");

            setPrivateField("venueTypeComboBox", venueTypeComboBox);

            assertEquals(
                    "Journal",
                    invokePrivate(
                            "getSelectedVenueType",
                            new Class<?>[]{}
                    )
            );
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
    void updateProfileLabels_setsJournalProfileLabels() throws Exception {
        runOnFxThread(() -> {
            Label firstYearLabel = new Label();
            Label lastYearLabel = new Label();
            Label totalArticlesLabel = new Label();
            Label totalAuthorsLabel = new Label();
            Label distinctAuthorsLabel = new Label();
            Label avgAuthorsArticleLabel = new Label();
            Label avgArticlesYearLabel = new Label();
            Label avgAuthorsYearLabel = new Label();

            setPrivateField("firstYearLabel", firstYearLabel);
            setPrivateField("lastYearLabel", lastYearLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalAuthorsLabel", totalAuthorsLabel);
            setPrivateField("distinctAuthorsLabel", distinctAuthorsLabel);
            setPrivateField("avgAuthorsArticleLabel", avgAuthorsArticleLabel);
            setPrivateField("avgArticlesYearLabel", avgArticlesYearLabel);
            setPrivateField("avgAuthorsYearLabel", avgAuthorsYearLabel);

            JournalProfileDto profile =
                    journalProfile(
                            2010,
                            2020,
                            100L,
                            300L,
                            80L,
                            3.0,
                            10.0,
                            30.0
                    );

            invokePrivate(
                    "updateProfileLabels",
                    new Class<?>[]{Object.class},
                    profile
            );

            assertEquals("2010", firstYearLabel.getText());
            assertEquals("2020", lastYearLabel.getText());
            assertEquals("100", totalArticlesLabel.getText());
            assertEquals("300", totalAuthorsLabel.getText());
            assertEquals("80", distinctAuthorsLabel.getText());
            assertEquals(String.format("%.2f", 3.0), avgAuthorsArticleLabel.getText());
            assertEquals(String.format("%.2f", 10.0), avgArticlesYearLabel.getText());
            assertEquals(String.format("%.2f", 30.0), avgAuthorsYearLabel.getText());
        });
    }

    @Test
    void updateProfileLabels_setsConferenceProfileLabels() throws Exception {
        runOnFxThread(() -> {
            Label firstYearLabel = new Label();
            Label lastYearLabel = new Label();
            Label totalArticlesLabel = new Label();
            Label totalAuthorsLabel = new Label();
            Label distinctAuthorsLabel = new Label();
            Label avgAuthorsArticleLabel = new Label();
            Label avgArticlesYearLabel = new Label();
            Label avgAuthorsYearLabel = new Label();

            setPrivateField("firstYearLabel", firstYearLabel);
            setPrivateField("lastYearLabel", lastYearLabel);
            setPrivateField("totalArticlesLabel", totalArticlesLabel);
            setPrivateField("totalAuthorsLabel", totalAuthorsLabel);
            setPrivateField("distinctAuthorsLabel", distinctAuthorsLabel);
            setPrivateField("avgAuthorsArticleLabel", avgAuthorsArticleLabel);
            setPrivateField("avgArticlesYearLabel", avgArticlesYearLabel);
            setPrivateField("avgAuthorsYearLabel", avgAuthorsYearLabel);

            ConferenceProfileDto profile =
                    conferenceProfile(
                            2012,
                            2022,
                            50L,
                            150L,
                            60L,
                            3.0,
                            5.0,
                            15.0
                    );

            invokePrivate(
                    "updateProfileLabels",
                    new Class<?>[]{Object.class},
                    profile
            );

            assertEquals("2012", firstYearLabel.getText());
            assertEquals("2022", lastYearLabel.getText());
            assertEquals("50", totalArticlesLabel.getText());
            assertEquals("150", totalAuthorsLabel.getText());
            assertEquals("60", distinctAuthorsLabel.getText());
            assertEquals(String.format("%.2f", 3.0), avgAuthorsArticleLabel.getText());
            assertEquals(String.format("%.2f", 5.0), avgArticlesYearLabel.getText());
            assertEquals(String.format("%.2f", 15.0), avgAuthorsYearLabel.getText());
        });
    }

    @Test
    void updateRankingPanel_setsDashValues_whenRankingIsNull() throws Exception {
        runOnFxThread(() -> {
            Label rankingLabel = new Label();
            Label categoryLabel = new Label();
            Label rankingMetricsLabel = new Label();

            setPrivateField("rankingLabel", rankingLabel);
            setPrivateField("categoryLabel", categoryLabel);
            setPrivateField("rankingMetricsLabel", rankingMetricsLabel);

            invokePrivate(
                    "updateRankingPanel",
                    new Class<?>[]{Object.class},
                    (Object) null
            );

            assertEquals("-", rankingLabel.getText());
            assertEquals("-", categoryLabel.getText());
            assertEquals("-", rankingMetricsLabel.getText());
        });
    }

    @Test
    void updateRankingPanel_setsJournalRankingLabels() throws Exception {
        runOnFxThread(() -> {
            Label rankingLabel = new Label();
            Label categoryLabel = new Label();
            Label rankingMetricsLabel = new Label();

            setPrivateField("rankingLabel", rankingLabel);
            setPrivateField("categoryLabel", categoryLabel);
            setPrivateField("rankingMetricsLabel", rankingMetricsLabel);

            JournalRankingDto ranking =
                    journalRanking(
                            5,
                            "Q1",
                            "Computer Science",
                            1.25,
                            3.40,
                            90,
                            1000,
                            5000,
                            2.75,
                            40.5
                    );

            invokePrivate(
                    "updateRankingPanel",
                    new Class<?>[]{Object.class},
                    ranking
            );

            String metricsText = rankingMetricsLabel.getText();

            assertEquals("Rank 5 / Q1", rankingLabel.getText());
            assertEquals("Computer Science", categoryLabel.getText());

            assertTrue(metricsText.contains("SJR: " + String.format("%.2f", 1.25)));
            assertTrue(metricsText.contains("CiteScore: " + String.format("%.2f", 3.40)));
            assertTrue(metricsText.contains("H-index: 90"));
            assertTrue(metricsText.contains("Total docs: 1000"));
        });
    }

    @Test
    void updateRankingPanel_setsConferenceRankingLabels() throws Exception {
        runOnFxThread(() -> {
            Label rankingLabel = new Label();
            Label categoryLabel = new Label();
            Label rankingMetricsLabel = new Label();

            setPrivateField("rankingLabel", rankingLabel);
            setPrivateField("categoryLabel", categoryLabel);
            setPrivateField("rankingMetricsLabel", rankingMetricsLabel);

            ConferenceRankingDto ranking =
                    conferenceRanking(
                            "A*",
                            "Artificial Intelligence",
                            123
                    );

            invokePrivate(
                    "updateRankingPanel",
                    new Class<?>[]{Object.class},
                    ranking
            );

            assertEquals("A*", rankingLabel.getText());
            assertEquals("Artificial Intelligence", categoryLabel.getText());
            assertEquals("ICORE ID: 123", rankingMetricsLabel.getText());
        });
    }

    @Test
    void buildJournalRankText_returnsExpectedText() throws Exception {
        assertEquals(
                "Rank 4 / Q2",
                invokePrivate(
                        "buildJournalRankText",
                        new Class<?>[]{JournalRankingDto.class},
                        journalRanking(
                                4,
                                "Q2",
                                "Category",
                                1.0,
                                2.0,
                                10,
                                20,
                                30,
                                1.5,
                                2.5
                        )
                )
        );

        assertEquals(
                "Q1",
                invokePrivate(
                        "buildJournalRankText",
                        new Class<?>[]{JournalRankingDto.class},
                        journalRanking(
                                null,
                                "Q1",
                                "Category",
                                1.0,
                                2.0,
                                10,
                                20,
                                30,
                                1.5,
                                2.5
                        )
                )
        );
    }

    @Test
    void extractYearAndMetricValues_returnExpectedValuesForJournalStats() throws Exception {
        JournalYearlyStatsDto stat =
                journalYearlyStats(2020, 10L, 30L, 8L);

        assertEquals(
                2020,
                invokePrivate(
                        "extractYear",
                        new Class<?>[]{Object.class},
                        stat
                )
        );

        assertEquals(
                10L,
                invokePrivate(
                        "extractTotalArticles",
                        new Class<?>[]{Object.class},
                        stat
                )
        );

        assertEquals(
                30L,
                invokePrivate(
                        "extractTotalAuthorOccurrences",
                        new Class<?>[]{Object.class},
                        stat
                )
        );

        assertEquals(
                8L,
                invokePrivate(
                        "extractDistinctAuthors",
                        new Class<?>[]{Object.class},
                        stat
                )
        );
    }

    @Test
    void extractArticleValues_returnExpectedValuesForJournalArticle() throws Exception {
        JournalArticleDto article =
                journalArticle(
                        2020,
                        "Journal Article",
                        "Alice, Bob",
                        "10-20",
                        "https://ee.example",
                        "https://url.example"
                );

        assertEquals(
                2020,
                invokePrivate(
                        "extractArticleYear",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "Journal Article",
                invokePrivate(
                        "extractArticleTitle",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "Alice, Bob",
                invokePrivate(
                        "extractArticleAuthors",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "10-20",
                invokePrivate(
                        "extractArticlePages",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "https://ee.example",
                invokePrivate(
                        "extractArticleUrlOrEe",
                        new Class<?>[]{Object.class},
                        article
                )
        );
    }

    @Test
    void extractArticleValues_returnExpectedValuesForConferenceArticle() throws Exception {
        ConferenceArticleDto article =
                conferenceArticle(
                        2021,
                        "Conference Article",
                        "Maria, John",
                        "30-40",
                        "",
                        "https://url.example"
                );

        assertEquals(
                2021,
                invokePrivate(
                        "extractArticleYear",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "Conference Article",
                invokePrivate(
                        "extractArticleTitle",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "Maria, John",
                invokePrivate(
                        "extractArticleAuthors",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "30-40",
                invokePrivate(
                        "extractArticlePages",
                        new Class<?>[]{Object.class},
                        article
                )
        );

        assertEquals(
                "https://url.example",
                invokePrivate(
                        "extractArticleUrlOrEe",
                        new Class<?>[]{Object.class},
                        article
                )
        );
    }

    @Test
    void updateYearlyLineCharts_buildsThreeSeriesAndSkipsRowsWithoutYear() throws Exception {
        runOnFxThread(() -> {
            NumberAxis articlesYearAxis = new NumberAxis();
            NumberAxis articlesCountAxis = new NumberAxis();
            LineChart<Number, Number> articlesLineChart =
                    new LineChart<>(articlesYearAxis, articlesCountAxis);

            NumberAxis authorEntriesYearAxis = new NumberAxis();
            NumberAxis authorEntriesCountAxis = new NumberAxis();
            LineChart<Number, Number> authorEntriesLineChart =
                    new LineChart<>(authorEntriesYearAxis, authorEntriesCountAxis);

            NumberAxis distinctAuthorsYearAxis = new NumberAxis();
            NumberAxis distinctAuthorsCountAxis = new NumberAxis();
            LineChart<Number, Number> distinctAuthorsLineChart =
                    new LineChart<>(distinctAuthorsYearAxis, distinctAuthorsCountAxis);

            setPrivateField("articlesLineChart", articlesLineChart);
            setPrivateField("articlesYearAxis", articlesYearAxis);
            setPrivateField("articlesCountAxis", articlesCountAxis);

            setPrivateField("authorEntriesLineChart", authorEntriesLineChart);
            setPrivateField("authorEntriesYearAxis", authorEntriesYearAxis);
            setPrivateField("authorEntriesCountAxis", authorEntriesCountAxis);

            setPrivateField("distinctAuthorsLineChart", distinctAuthorsLineChart);
            setPrivateField("distinctAuthorsYearAxis", distinctAuthorsYearAxis);
            setPrivateField("distinctAuthorsCountAxis", distinctAuthorsCountAxis);

            List<Object> stats =
                    List.of(
                            journalYearlyStats(2020, 10L, 30L, 8L),
                            journalYearlyStats(2021, null, 50L, 12L),
                            journalYearlyStats(null, 99L, 99L, 99L)
                    );

            invokePrivate(
                    "updateYearlyLineCharts",
                    new Class<?>[]{List.class},
                    stats
            );

            assertEquals(1, articlesLineChart.getData().size());
            assertEquals(1, authorEntriesLineChart.getData().size());
            assertEquals(1, distinctAuthorsLineChart.getData().size());

            assertEquals("Articles", articlesLineChart.getData().get(0).getName());
            assertEquals("Author entries", authorEntriesLineChart.getData().get(0).getName());
            assertEquals("Distinct authors", distinctAuthorsLineChart.getData().get(0).getName());

            assertEquals(2, articlesLineChart.getData().get(0).getData().size());
            assertEquals(2, authorEntriesLineChart.getData().get(0).getData().size());
            assertEquals(2, distinctAuthorsLineChart.getData().get(0).getData().size());

            assertEquals(2020, articlesLineChart.getData().get(0).getData().get(0).getXValue().intValue());
            assertEquals(10L, articlesLineChart.getData().get(0).getData().get(0).getYValue().longValue());

            assertEquals(2021, articlesLineChart.getData().get(0).getData().get(1).getXValue().intValue());
            assertEquals(0L, articlesLineChart.getData().get(0).getData().get(1).getYValue().longValue());

            assertFalse(articlesYearAxis.isAutoRanging());
            assertTrue(articlesCountAxis.getUpperBound() >= 10.0);
        });
    }

    @Test
    void visibilityMethodsUpdateVisibleAndManagedFlags() throws Exception {
        runOnFxThread(() -> {
            VBox venueResultsContainer = new VBox();
            VBox articleReportPanel = new VBox();

            setPrivateField("venueResultsContainer", venueResultsContainer);
            setPrivateField("articleReportPanel", articleReportPanel);

            invokePrivate(
                    "setVenueResultsVisible",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(venueResultsContainer.isVisible());
            assertTrue(venueResultsContainer.isManaged());

            invokePrivate(
                    "setVenueResultsVisible",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(venueResultsContainer.isVisible());
            assertFalse(venueResultsContainer.isManaged());

            invokePrivate(
                    "setArticleReportVisible",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(articleReportPanel.isVisible());
            assertTrue(articleReportPanel.isManaged());

            invokePrivate(
                    "setArticleReportVisible",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(articleReportPanel.isVisible());
            assertFalse(articleReportPanel.isManaged());
        });
    }

    @Test
    void setLoading_disablesAndEnablesControls() throws Exception {
        runOnFxThread(() -> {
            ComboBox<String> venueTypeComboBox = new ComboBox<>();
            TextField venueSearchField = new TextField();
            Button searchVenueButton = new Button();
            ComboBox<Integer> fromYearComboBox = new ComboBox<>();
            ComboBox<Integer> toYearComboBox = new ComboBox<>();
            CheckBox loadArticlesCheckBox = new CheckBox();
            Button loadProfileButton = new Button();
            Button clearButton = new Button();
            ListView<Object> venueResultsListView = new ListView<>();

            setPrivateField("venueTypeComboBox", venueTypeComboBox);
            setPrivateField("venueSearchField", venueSearchField);
            setPrivateField("searchVenueButton", searchVenueButton);
            setPrivateField("fromYearComboBox", fromYearComboBox);
            setPrivateField("toYearComboBox", toYearComboBox);
            setPrivateField("loadArticlesCheckBox", loadArticlesCheckBox);
            setPrivateField("loadProfileButton", loadProfileButton);
            setPrivateField("clearButton", clearButton);
            setPrivateField("venueResultsListView", venueResultsListView);

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    true
            );

            assertTrue(venueTypeComboBox.isDisabled());
            assertTrue(venueSearchField.isDisabled());
            assertTrue(searchVenueButton.isDisabled());
            assertTrue(fromYearComboBox.isDisabled());
            assertTrue(toYearComboBox.isDisabled());
            assertTrue(loadArticlesCheckBox.isDisabled());
            assertTrue(loadProfileButton.isDisabled());
            assertTrue(clearButton.isDisabled());
            assertTrue(venueResultsListView.isDisabled());

            invokePrivate(
                    "setLoading",
                    new Class<?>[]{boolean.class},
                    false
            );

            assertFalse(venueTypeComboBox.isDisabled());
            assertFalse(venueSearchField.isDisabled());
            assertFalse(searchVenueButton.isDisabled());
            assertFalse(fromYearComboBox.isDisabled());
            assertFalse(toYearComboBox.isDisabled());
            assertFalse(loadArticlesCheckBox.isDisabled());
            assertFalse(loadProfileButton.isDisabled());
            assertFalse(clearButton.isDisabled());
            assertFalse(venueResultsListView.isDisabled());
        });
    }

    private Object invokePrivate(
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = VenueController.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);

        try {
            return method.invoke(venueController, arguments);
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
        Field field = VenueController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(venueController, value);
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

    private JournalProfileDto journalProfile(
            Integer firstYear,
            Integer lastYear,
            Long totalArticles,
            Long totalAuthorOccurrences,
            Long distinctAuthorsAllTime,
            Double avgAuthorsPerArticle,
            Double avgArticlesPerYear,
            Double avgAuthorOccurrencesPerYear
    ) {
        JournalProfileDto dto = mock(JournalProfileDto.class);

        lenient().doReturn(firstYear).when(dto).firstYear();
        lenient().doReturn(lastYear).when(dto).lastYear();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalAuthorOccurrences).when(dto).totalAuthorOccurrences();
        lenient().doReturn(distinctAuthorsAllTime).when(dto).distinctAuthorsAllTime();
        lenient().doReturn(avgAuthorsPerArticle).when(dto).avgAuthorsPerArticle();
        lenient().doReturn(avgArticlesPerYear).when(dto).avgArticlesPerYear();
        lenient().doReturn(avgAuthorOccurrencesPerYear).when(dto).avgAuthorOccurrencesPerYear();

        return dto;
    }

    private ConferenceProfileDto conferenceProfile(
            Integer firstYear,
            Integer lastYear,
            Long totalArticles,
            Long totalAuthorOccurrences,
            Long distinctAuthors,
            Double avgAuthorsPerArticle,
            Double avgArticlesPerYear,
            Double avgAuthorOccurrencesPerYear
    ) {
        ConferenceProfileDto dto = mock(ConferenceProfileDto.class);

        lenient().doReturn(firstYear).when(dto).firstYear();
        lenient().doReturn(lastYear).when(dto).lastYear();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalAuthorOccurrences).when(dto).totalAuthorOccurrences();
        lenient().doReturn(distinctAuthors).when(dto).distinctAuthors();
        lenient().doReturn(avgAuthorsPerArticle).when(dto).avgAuthorsPerArticle();
        lenient().doReturn(avgArticlesPerYear).when(dto).avgArticlesPerYear();
        lenient().doReturn(avgAuthorOccurrencesPerYear).when(dto).avgAuthorOccurrencesPerYear();

        return dto;
    }

    private JournalRankingDto journalRanking(
            Integer rankingPosition,
            String bestQuartile,
            String bestSubjectArea,
            Double sjrIndex,
            Double citeScore,
            Integer hIndex,
            Integer totalDocs,
            Integer totalRefs,
            Double citesPerDoc2y,
            Double refsPerDoc
    ) {
        JournalRankingDto dto = mock(JournalRankingDto.class);

        lenient().doReturn(rankingPosition).when(dto).rankingPosition();
        lenient().doReturn(bestQuartile).when(dto).bestQuartile();
        lenient().doReturn(bestSubjectArea).when(dto).bestSubjectArea();
        lenient().doReturn(sjrIndex).when(dto).sjrIndex();
        lenient().doReturn(citeScore).when(dto).citeScore();
        lenient().doReturn(hIndex).when(dto).hIndex();
        lenient().doReturn(totalDocs).when(dto).totalDocs();
        lenient().doReturn(totalRefs).when(dto).totalRefs();
        lenient().doReturn(citesPerDoc2y).when(dto).citesPerDoc2y();
        lenient().doReturn(refsPerDoc).when(dto).refsPerDoc();

        return dto;
    }

    private ConferenceRankingDto conferenceRanking(
            String rankLabel,
            String primaryFoRName,
            Integer icoreId
    ) {
        ConferenceRankingDto dto = mock(ConferenceRankingDto.class);

        lenient().doReturn(rankLabel).when(dto).rankLabel();
        lenient().doReturn(primaryFoRName).when(dto).primaryFoRName();
        lenient().doReturn(icoreId).when(dto).icoreId();

        return dto;
    }

    private JournalYearlyStatsDto journalYearlyStats(
            Integer year,
            Long totalArticles,
            Long totalAuthorOccurrences,
            Long distinctAuthors
    ) {
        JournalYearlyStatsDto dto = mock(JournalYearlyStatsDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(totalArticles).when(dto).totalArticles();
        lenient().doReturn(totalAuthorOccurrences).when(dto).totalAuthorOccurrences();
        lenient().doReturn(distinctAuthors).when(dto).distinctAuthors();

        return dto;
    }

    private JournalArticleDto journalArticle(
            Integer year,
            String title,
            String authors,
            String pages,
            String ee,
            String url
    ) {
        JournalArticleDto dto = mock(JournalArticleDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(title).when(dto).title();
        lenient().doReturn(authors).when(dto).authors();
        lenient().doReturn(pages).when(dto).pages();
        lenient().doReturn(ee).when(dto).ee();
        lenient().doReturn(url).when(dto).url();

        return dto;
    }

    private ConferenceArticleDto conferenceArticle(
            Integer year,
            String title,
            String authors,
            String pages,
            String ee,
            String url
    ) {
        ConferenceArticleDto dto = mock(ConferenceArticleDto.class);

        lenient().doReturn(year).when(dto).year();
        lenient().doReturn(title).when(dto).title();
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