package controllers;

import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import service.ConferenceService;
import service.JournalService;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartsController {

    private static final String HOME_FXML_PATH = "/com/example/project_pvasil/hello-view.fxml";

    private static final String TYPE_JOURNAL = "Journal";
    private static final String TYPE_CONFERENCE = "Conference";

    private static final String METRIC_ARTICLES = "Published articles";
    private static final String METRIC_AUTHOR_ENTRIES = "Total author entries";
    private static final String METRIC_DISTINCT_AUTHORS = "Distinct authors";

    private static final String BAR_TOTAL_ARTICLES = "Total articles";
    private static final String BAR_AVG_ARTICLES_PER_YEAR = "Avg articles / year";
    private static final String BAR_AVG_AUTHOR_ENTRIES_PER_YEAR = "Avg author entries / year";

    private static final String BAR_SINGLE_CATEGORY = " ";

    private static final String[] CHART_COLORS = {
            "#1f5fa8",
            "#f5a623",
            "#4caf50",
            "#36a9c9",
            "#4666d8",
            "#9c46d4",
            "#d13f64",
            "#8c8c8c",
            "#ff9800",
            "#2e7d32"
    };

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SELECTED_VENUES = 10;

    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    private Object selectedSearchVenue;

    private PauseTransition searchDebounce;

    private Task<List<Object>> venueSearchTask;

    @FXML
    private ComboBox<String> venueTypeComboBox;

    @FXML
    private TextField venueSearchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button addSelectedButton;

    @FXML
    private Button removeSelectedButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button loadChartButton;

    @FXML
    private Button backButton;

    @FXML
    private VBox searchResultsContainer;

    @FXML
    private ListView<Object> searchResultsListView;

    @FXML
    private ListView<SelectedVenue> selectedVenuesListView;

    @FXML
    private ComboBox<String> metricComboBox;

    @FXML
    private ComboBox<Integer> fromYearComboBox;

    @FXML
    private ComboBox<Integer> toYearComboBox;

    @FXML
    private VBox lineChartLegendBox;

    @FXML
    private FlowPane lineChartLegendFlow;

    @FXML
    private VBox barChartsLegendBox;

    @FXML
    private FlowPane barChartsLegendFlow;

    @FXML
    private VBox scatterLegendBox;

    @FXML
    private FlowPane scatterLegendFlow;

    @FXML
    private LineChart<Number, Number> comparisonLineChart;

    @FXML
    private NumberAxis yearAxis;

    @FXML
    private NumberAxis valueAxis;

    @FXML
    private BarChart<String, Number> totalArticlesBarChart;

    @FXML
    private CategoryAxis totalArticlesCategoryAxis;

    @FXML
    private NumberAxis totalArticlesValueAxis;

    @FXML
    private BarChart<String, Number> avgArticlesBarChart;

    @FXML
    private CategoryAxis avgArticlesCategoryAxis;

    @FXML
    private NumberAxis avgArticlesValueAxis;

    @FXML
    private BarChart<String, Number> avgAuthorEntriesBarChart;

    @FXML
    private CategoryAxis avgAuthorEntriesCategoryAxis;

    @FXML
    private NumberAxis avgAuthorEntriesValueAxis;

    @FXML
    private ScatterChart<Number, Number> venueScatterChart;

    @FXML
    private NumberAxis scatterArticlesAxis;

    @FXML
    private NumberAxis scatterAuthorsAxis;

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupMetricComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupSearchResultsListView();
        setupSelectedVenuesListView();
        setupChart();
        setupExtraCharts();

        selectedSearchVenue = null;
        clearSearchResults();
        clearChart();
    }

    @FXML
    private void searchVenues() {
        executeVenueSearch(true);
    }

    private void searchVenuesRealtime() {
        executeVenueSearch(false);
    }

    private void executeVenueSearch(boolean showAlerts) {
        String venueType = getSelectedVenueType();
        String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

        if (venueSearchTask != null && venueSearchTask.isRunning()) {
            venueSearchTask.cancel();
        }

        if (venueType == null || venueType.isBlank()) {
            clearSearchResults();

            if (showAlerts) {
                showError("Δεν επιλέχθηκε τύπος", "Πρέπει πρώτα να επιλέξεις Journal ή Conference.");
            }

            return;
        }

        if (searchText.length() < MIN_SEARCH_LENGTH) {
            selectedSearchVenue = null;
            clearSearchResults();

            if (showAlerts && searchText.isBlank()) {
                showError("Λάθος αναζήτηση", "Πρέπει να γράψεις όνομα journal ή conference.");
            } else if (showAlerts) {
                showError(
                        "Λάθος αναζήτηση",
                        "Πρέπει να γράψεις τουλάχιστον 3 χαρακτήρες."
                );
            }

            return;
        }

        String requestedVenueType = venueType;
        String requestedSearchText = searchText;

        selectedSearchVenue = null;
        clearSearchResultsOnly();

        venueSearchTask = new Task<>() {
            @Override
            protected List<Object> call() {
                List<Object> results = new ArrayList<>();

                if (TYPE_JOURNAL.equals(requestedVenueType)) {
                    results.addAll(journalService.searchJournals(requestedSearchText, SEARCH_LIMIT));
                    return results;
                }

                if (TYPE_CONFERENCE.equals(requestedVenueType)) {
                    results.addAll(conferenceService.searchConferences(requestedSearchText, SEARCH_LIMIT));
                    return results;
                }

                return results;
            }
        };

        if (showAlerts) {
            setLoading(true);
        }

        venueSearchTask.setOnSucceeded(event -> {
            if (showAlerts) {
                setLoading(false);
            }

            if (venueSearchTask.isCancelled()) {
                return;
            }

            String currentSearchText = venueSearchField == null ? "" : venueSearchField.getText().trim();
            String currentVenueType = getSelectedVenueType();

            if (!requestedSearchText.equals(currentSearchText)) {
                return;
            }

            if (!requestedVenueType.equals(currentVenueType)) {
                return;
            }

            List<Object> results = venueSearchTask.getValue();
            results = sortVenueResultsByRelevance(results, requestedSearchText);

            renderSearchResults(results);

            if (results.isEmpty() && showAlerts) {
                showInfo(
                        "Δεν βρέθηκαν αποτελέσματα",
                        "Δεν βρέθηκε journal/conference με αυτό το κείμενο αναζήτησης."
                );
            }
        });

        venueSearchTask.setOnFailed(event -> {
            if (showAlerts) {
                setLoading(false);
            }

            Throwable exception = venueSearchTask.getException();

            if (showAlerts) {
                showError("Σφάλμα αναζήτησης", exception == null ? null : exception.getMessage());
            } else {
                clearSearchResults();
            }
        });

        startBackgroundTask(venueSearchTask, "charts-search-task");
    }

    @FXML
    private void addSelectedVenue() {
        addSelectedVenueFromSearch(selectedSearchVenue, true);
    }

    private void addSelectedVenueFromSearch(Object venue, boolean showMessages) {
        if (selectedVenuesListView == null) {
            return;
        }

        if (venue == null) {
            if (showMessages) {
                showError(
                        "Δεν επιλέχθηκε venue",
                        "Πρέπει πρώτα να επιλέξεις ένα αποτέλεσμα από τη λίστα Matching results."
                );
            }

            return;
        }

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            if (showMessages) {
                showError("Δεν επιλέχθηκε τύπος", "Πρέπει πρώτα να επιλέξεις Journal ή Conference.");
            }

            return;
        }

        if (selectedVenuesListView.getItems().size() >= MAX_SELECTED_VENUES) {
            if (showMessages) {
                showError(
                        "Πολλά venues",
                        "Για να παραμένει ευανάγνωστο το chart, μπορείς να επιλέξεις μέχρι "
                                + MAX_SELECTED_VENUES + " venues."
                );
            }

            return;
        }

        SelectedVenue selectedVenue = new SelectedVenue(venueType, venue);

        if (alreadySelected(selectedVenue)) {
            if (showMessages) {
                showInfo("Ήδη επιλεγμένο", "Το συγκεκριμένο venue υπάρχει ήδη στη λίστα.");
            }

            return;
        }

        selectedVenuesListView.getItems().add(selectedVenue);

        if (searchResultsListView != null) {
            searchResultsListView.getSelectionModel().clearSelection();
        }

        selectedSearchVenue = null;
    }

    @FXML
    private void removeSelectedVenue() {
        if (selectedVenuesListView == null) {
            return;
        }

        SelectedVenue selected = selectedVenuesListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            selectedVenuesListView.getItems().remove(selected);
        }
    }

    @FXML
    private void loadChart() {
        if (selectedVenuesListView == null || selectedVenuesListView.getItems().isEmpty()) {
            showError("Δεν υπάρχουν επιλεγμένα venues", "Πρέπει να προσθέσεις τουλάχιστον ένα journal ή conference.");
            return;
        }

        String metric = metricComboBox == null ? null : metricComboBox.getValue();

        if (metric == null || metric.isBlank()) {
            showError("Δεν επιλέχθηκε metric", "Πρέπει να επιλέξεις τι θέλεις να εμφανίζει το line chart.");
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Λάθος χρονιές", exception.getMessage());
            return;
        }

        List<SelectedVenue> selectedVenues = new ArrayList<>(selectedVenuesListView.getItems());

        Task<List<VenueChartSeries>> task = new Task<>() {
            @Override
            protected List<VenueChartSeries> call() {
                List<VenueChartSeries> chartSeries = new ArrayList<>();

                for (SelectedVenue selectedVenue : selectedVenues) {
                    if (TYPE_JOURNAL.equals(selectedVenue.type())) {
                        int journalId = getJournalId(selectedVenue.venue());

                        List<JournalYearlyStatsDto> stats =
                                journalService.getJournalYearlyStats(
                                        journalId,
                                        yearRange.startYear(),
                                        yearRange.endYear()
                                );

                        List<Object> yearlyStats = new ArrayList<>();

                        if (stats != null) {
                            yearlyStats.addAll(stats);
                        }

                        chartSeries.add(
                                new VenueChartSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        yearlyStats
                                )
                        );
                    }

                    if (TYPE_CONFERENCE.equals(selectedVenue.type())) {
                        int conferenceId = getConferenceId(selectedVenue.venue());

                        List<ConferenceYearlyStatsDto> stats =
                                conferenceService.getConferenceYearlyStats(
                                        conferenceId,
                                        yearRange.startYear(),
                                        yearRange.endYear()
                                );

                        List<Object> yearlyStats = new ArrayList<>();

                        if (stats != null) {
                            yearlyStats.addAll(stats);
                        }

                        chartSeries.add(
                                new VenueChartSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        yearlyStats
                                )
                        );
                    }
                }

                return chartSeries;
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<VenueChartSeries> chartData = task.getValue();

            updateLineChart(chartData, metric);
            updateBarChart(chartData);
            updateScatterChart(chartData);
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα φόρτωσης charts", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "charts-load-task");
    }

    @FXML
    private void clear() {
        stopCurrentVenueSearch();

        if (venueSearchField != null) {
            venueSearchField.clear();
        }

        clearSearchResults();

        if (selectedVenuesListView != null) {
            selectedVenuesListView.getItems().clear();
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.getSelectionModel().clearSelection();
        }

        if (toYearComboBox != null) {
            toYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        selectedSearchVenue = null;
        clearChart();
    }

    @FXML
    private void backToHome() {
        stopCurrentVenueSearch();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(HOME_FXML_PATH)
            );

            Node node = backButton != null ? backButton : venueTypeComboBox;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            showError(
                    "Σφάλμα πλοήγησης",
                    "Δεν ήταν δυνατή η επιστροφή στην αρχική σελίδα. Έλεγξε το HOME_FXML_PATH."
            );
        }
    }

    private void setupVenueTypeComboBox() {
        if (venueTypeComboBox == null) {
            return;
        }

        venueTypeComboBox.getItems().setAll(TYPE_JOURNAL, TYPE_CONFERENCE);
        venueTypeComboBox.getSelectionModel().select(TYPE_JOURNAL);

        venueTypeComboBox.setOnAction(event -> {
            stopCurrentVenueSearch();

            selectedSearchVenue = null;
            clearSearchResults();

            String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

            if (searchText.length() >= MIN_SEARCH_LENGTH && searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });
    }

    private void setupMetricComboBox() {
        if (metricComboBox == null) {
            return;
        }

        metricComboBox.getItems().setAll(
                METRIC_ARTICLES,
                METRIC_AUTHOR_ENTRIES,
                METRIC_DISTINCT_AUTHORS
        );

        metricComboBox.getSelectionModel().select(METRIC_ARTICLES);
    }

    private void setupYearComboBoxes() {
        setupYearComboBox(fromYearComboBox);
        setupYearComboBox(toYearComboBox);

        if (fromYearComboBox != null) {
            fromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    refreshToYearOptions(newValue)
            );
        }
    }

    private void setupYearComboBox(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setEditable(false);
        comboBox.getItems().setAll(buildYearList(MIN_YEAR));
    }

    private List<Integer> buildYearList(Integer minimumYear) {
        int currentYear = LocalDate.now().getYear();
        int min = minimumYear == null ? MIN_YEAR : minimumYear;

        List<Integer> years = new ArrayList<>();

        for (int year = currentYear; year >= min; year--) {
            years.add(year);
        }

        return years;
    }

    private void refreshToYearOptions(Integer fromYear) {
        if (toYearComboBox == null) {
            return;
        }

        Integer currentToYear = toYearComboBox.getValue();

        toYearComboBox.getItems().setAll(buildYearList(fromYear));

        if (currentToYear != null && fromYear != null && currentToYear < fromYear) {
            toYearComboBox.getSelectionModel().clearSelection();
            return;
        }

        if (currentToYear != null && toYearComboBox.getItems().contains(currentToYear)) {
            toYearComboBox.getSelectionModel().select(currentToYear);
        }
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchVenuesRealtime());

        if (venueSearchField == null) {
            return;
        }

        venueSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        /*
         * To Enter menei san extra shortcut,
         * alla den einai pleon aparaitito.
         */
        venueSearchField.setOnAction(event -> searchVenues());
    }

    private void setupSearchResultsListView() {
        if (searchResultsListView == null) {
            return;
        }

        searchResultsListView.setPlaceholder(new Label("Search results will appear here."));

        searchResultsListView.setCellFactory(listView -> {
            ListCell<Object> cell = new ListCell<>() {
                @Override
                protected void updateItem(Object venue, boolean empty) {
                    super.updateItem(venue, empty);

                    if (empty || venue == null) {
                        setText(null);
                    } else {
                        setText(getRawVenueDisplayName(venue));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedSearchVenue = cell.getItem();
                    addSelectedVenueFromSearch(cell.getItem(), false);
                }
            });

            return cell;
        });

        searchResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> selectedSearchVenue = selected
        );
    }

    private void setupSelectedVenuesListView() {
        if (selectedVenuesListView == null) {
            return;
        }

        selectedVenuesListView.setPlaceholder(new Label("Selected venues will appear here."));

        selectedVenuesListView.setCellFactory(listView -> {
            ListCell<SelectedVenue> cell = new ListCell<>() {
                @Override
                protected void updateItem(SelectedVenue selectedVenue, boolean empty) {
                    super.updateItem(selectedVenue, empty);

                    if (empty || selectedVenue == null) {
                        setText(null);
                    } else {
                        setText(getVenueDisplayName(selectedVenue));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !cell.isEmpty()
                        && cell.getItem() != null) {

                    selectedVenuesListView.getItems().remove(cell.getItem());
                }
            });

            return cell;
        });
    }

    private void setupChart() {
        if (comparisonLineChart != null) {
            comparisonLineChart.setAnimated(false);
            comparisonLineChart.setCreateSymbols(true);
            comparisonLineChart.setLegendVisible(false);
        }

        if (yearAxis != null) {
            yearAxis.setAutoRanging(false);
            yearAxis.setForceZeroInRange(false);
            yearAxis.setTickLabelRotation(0);
        }

        if (valueAxis != null) {
            valueAxis.setAutoRanging(false);
            valueAxis.setForceZeroInRange(true);
        }
    }

    private void setupExtraCharts() {
        setupSingleBarChart(
                totalArticlesBarChart,
                totalArticlesCategoryAxis,
                totalArticlesValueAxis,
                BAR_TOTAL_ARTICLES
        );

        setupSingleBarChart(
                avgArticlesBarChart,
                avgArticlesCategoryAxis,
                avgArticlesValueAxis,
                BAR_AVG_ARTICLES_PER_YEAR
        );

        setupSingleBarChart(
                avgAuthorEntriesBarChart,
                avgAuthorEntriesCategoryAxis,
                avgAuthorEntriesValueAxis,
                BAR_AVG_AUTHOR_ENTRIES_PER_YEAR
        );

        if (venueScatterChart != null) {
            venueScatterChart.setAnimated(false);
            venueScatterChart.setLegendVisible(false);
        }

        if (scatterArticlesAxis != null) {
            scatterArticlesAxis.setAutoRanging(false);
            scatterArticlesAxis.setForceZeroInRange(true);
        }

        if (scatterAuthorsAxis != null) {
            scatterAuthorsAxis.setAutoRanging(false);
            scatterAuthorsAxis.setForceZeroInRange(true);
        }
    }

    private void setupSingleBarChart(
            BarChart<String, Number> chart,
            CategoryAxis categoryAxis,
            NumberAxis valueAxis,
            String valueLabel
    ) {
        if (chart != null) {
            chart.setAnimated(false);
            chart.setLegendVisible(false);
            chart.setCategoryGap(18);
            chart.setBarGap(4);
        }

        if (categoryAxis != null) {
            categoryAxis.setLabel("");
            categoryAxis.setTickLabelsVisible(false);
            categoryAxis.setTickMarkVisible(false);
        }

        if (valueAxis != null) {
            valueAxis.setLabel(valueLabel);
            valueAxis.setAutoRanging(false);
            valueAxis.setForceZeroInRange(true);
        }
    }

    private void setSearchResultsVisible(boolean visible) {
        if (searchResultsContainer != null) {
            searchResultsContainer.setVisible(visible);
            searchResultsContainer.setManaged(visible);
        }
    }

    private void renderSearchResults(List<Object> results) {
        if (searchResultsListView == null) {
            return;
        }

        searchResultsListView.getItems().clear();
        searchResultsListView.getSelectionModel().clearSelection();
        selectedSearchVenue = null;

        setSearchResultsVisible(true);

        if (results == null || results.isEmpty()) {
            searchResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        searchResultsListView.setPlaceholder(new Label("No matching results"));
        searchResultsListView.getItems().setAll(results);
    }

    private void clearSearchResults() {
        clearSearchResultsOnly();
        setSearchResultsVisible(false);
    }

    private void clearSearchResultsOnly() {
        if (searchResultsListView != null) {
            searchResultsListView.getItems().clear();
            searchResultsListView.getSelectionModel().clearSelection();
            searchResultsListView.setPlaceholder(new Label("Search results will appear here."));
        }

        selectedSearchVenue = null;
    }

    private void stopCurrentVenueSearch() {
        if (searchDebounce != null) {
            searchDebounce.stop();
        }

        if (venueSearchTask != null && venueSearchTask.isRunning()) {
            venueSearchTask.cancel();
        }
    }

    private List<Object> sortVenueResultsByRelevance(List<Object> results, String query) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = normalizeSearchText(query);
        List<Object> sortedResults = new ArrayList<>(results);

        sortedResults.sort((first, second) -> {
            int firstScore = venueRelevanceScore(first, normalizedQuery);
            int secondScore = venueRelevanceScore(second, normalizedQuery);

            if (firstScore != secondScore) {
                return Integer.compare(firstScore, secondScore);
            }

            String firstTitle = normalizeSearchText(getVenueTitleForSearch(first));
            String secondTitle = normalizeSearchText(getVenueTitleForSearch(second));

            if (firstTitle.length() != secondTitle.length()) {
                return Integer.compare(firstTitle.length(), secondTitle.length());
            }

            return firstTitle.compareTo(secondTitle);
        });

        return sortedResults;
    }

    private int venueRelevanceScore(Object venue, String query) {
        String title = normalizeSearchText(getVenueTitleForSearch(venue));
        String acronym = normalizeSearchText(getVenueAcronymForSearch(venue));
        String displayName = normalizeSearchText(getRawVenueDisplayName(venue));

        if (title.equals(query) || acronym.equals(query) || displayName.equals(query)) {
            return 0;
        }

        if (title.startsWith(query) || acronym.startsWith(query) || displayName.startsWith(query)) {
            return 1;
        }

        if (title.contains(query) || acronym.contains(query) || displayName.contains(query)) {
            return 2;
        }

        if (containsAllQueryWords(title + " " + acronym + " " + displayName, query)) {
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

    private String getVenueTitleForSearch(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            return journal.journalName();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.conferenceTitle();
        }

        return "";
    }

    private String getVenueAcronymForSearch(Object venue) {
        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.acronym();
        }

        return "";
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

    private void updateLineChart(List<VenueChartSeries> allSeries, String metric) {
        clearChart();

        if (allSeries == null || allSeries.isEmpty()) {
            updateCustomLegend(null);
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        double maxValue = 0;
        int seriesIndex = 0;

        for (VenueChartSeries venueSeries : allSeries) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(shortenSeriesName(venueSeries.name()));

            for (Object stat : venueSeries.yearlyStats()) {
                Integer year = extractYear(stat);
                Long value = extractMetricValue(stat, metric);

                if (year == null || value == null) {
                    continue;
                }

                minYear = minYear == null ? year : Math.min(minYear, year);
                maxYear = maxYear == null ? year : Math.max(maxYear, year);
                maxValue = Math.max(maxValue, value);

                series.getData().add(new XYChart.Data<>(year, value));
            }

            if (!series.getData().isEmpty() && comparisonLineChart != null) {
                comparisonLineChart.getData().add(series);

                final int colorIndex = seriesIndex;
                runAfterChartRender(() -> applyLineSeriesColor(series, colorIndex));
            }

            seriesIndex++;
        }

        configureYearAxis(yearAxis, minYear, maxYear);
        configureValueAxis(valueAxis, maxValue);

        if (valueAxis != null) {
            valueAxis.setLabel(metric);
        }

        updateCustomLegend(allSeries);
    }

    private void updateBarChart(List<VenueChartSeries> allSeries) {
        updateSingleMetricBarChart(
                totalArticlesBarChart,
                totalArticlesValueAxis,
                allSeries,
                BAR_TOTAL_ARTICLES
        );

        updateSingleMetricBarChart(
                avgArticlesBarChart,
                avgArticlesValueAxis,
                allSeries,
                BAR_AVG_ARTICLES_PER_YEAR
        );

        updateSingleMetricBarChart(
                avgAuthorEntriesBarChart,
                avgAuthorEntriesValueAxis,
                allSeries,
                BAR_AVG_AUTHOR_ENTRIES_PER_YEAR
        );
    }

    private void updateSingleMetricBarChart(
            BarChart<String, Number> chart,
            NumberAxis valueAxis,
            List<VenueChartSeries> allSeries,
            String metric
    ) {
        if (chart == null) {
            return;
        }

        chart.getData().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            configureValueAxis(valueAxis, 10);
            return;
        }

        double maxValue = 0;
        int seriesIndex = 0;

        for (VenueChartSeries venueSeries : allSeries) {
            VenueAggregate aggregate = calculateVenueAggregate(venueSeries);

            String venueName = shortenSeriesName(venueSeries.name());
            double value = getBarMetricValue(aggregate, metric);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(venueName);

            XYChart.Data<String, Number> dataPoint =
                    new XYChart.Data<>(BAR_SINGLE_CATEGORY, value);

            series.getData().add(dataPoint);
            chart.getData().add(series);

            final int colorIndex = seriesIndex;
            runAfterChartRender(() -> applyBarColor(dataPoint, colorIndex));

            maxValue = Math.max(maxValue, value);
            seriesIndex++;
        }

        if (valueAxis != null) {
            valueAxis.setLabel(metric);
        }

        configureValueAxis(valueAxis, maxValue);
    }

    private double getBarMetricValue(VenueAggregate aggregate, String metric) {
        if (aggregate == null) {
            return 0;
        }

        if (BAR_TOTAL_ARTICLES.equals(metric)) {
            return aggregate.totalArticles();
        }

        if (BAR_AVG_ARTICLES_PER_YEAR.equals(metric)) {
            return aggregate.avgArticlesPerYear();
        }

        if (BAR_AVG_AUTHOR_ENTRIES_PER_YEAR.equals(metric)) {
            return aggregate.avgAuthorEntriesPerYear();
        }

        return 0;
    }

    private void updateScatterChart(List<VenueChartSeries> allSeries) {
        if (venueScatterChart == null) {
            return;
        }

        venueScatterChart.getData().clear();
        venueScatterChart.setLegendVisible(false);

        if (allSeries == null || allSeries.isEmpty()) {
            configureValueAxis(scatterArticlesAxis, 10);
            configureValueAxis(scatterAuthorsAxis, 10);
            return;
        }

        double maxArticles = 0;
        double maxAvgAuthors = 0;
        int seriesIndex = 0;

        for (VenueChartSeries venueSeries : allSeries) {
            VenueAggregate aggregate = calculateVenueAggregate(venueSeries);

            maxArticles = Math.max(maxArticles, aggregate.totalArticles());
            maxAvgAuthors = Math.max(maxAvgAuthors, aggregate.avgAuthorEntriesPerYear());

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(shortenSeriesName(venueSeries.name()));

            XYChart.Data<Number, Number> dataPoint =
                    new XYChart.Data<>(
                            aggregate.totalArticles(),
                            aggregate.avgAuthorEntriesPerYear()
                    );

            series.getData().add(dataPoint);
            venueScatterChart.getData().add(series);

            final int colorIndex = seriesIndex;
            runAfterChartRender(() -> applyScatterColor(dataPoint, colorIndex));

            seriesIndex++;
        }

        if (scatterArticlesAxis != null) {
            scatterArticlesAxis.setLabel("Total articles");
        }

        if (scatterAuthorsAxis != null) {
            scatterAuthorsAxis.setLabel("Avg author entries / year");
        }

        configureValueAxis(scatterArticlesAxis, maxArticles);
        configureValueAxis(scatterAuthorsAxis, maxAvgAuthors);
    }

    private VenueAggregate calculateVenueAggregate(VenueChartSeries venueSeries) {
        if (venueSeries == null || venueSeries.yearlyStats() == null || venueSeries.yearlyStats().isEmpty()) {
            return new VenueAggregate(0, 0, 0, 0, 0);
        }

        long totalArticles = 0;
        long totalAuthorEntries = 0;
        int activeYears = 0;

        for (Object stat : venueSeries.yearlyStats()) {
            Long articles = extractMetricValue(stat, METRIC_ARTICLES);
            Long authorEntries = extractMetricValue(stat, METRIC_AUTHOR_ENTRIES);

            if (articles == null && authorEntries == null) {
                continue;
            }

            activeYears++;

            if (articles != null) {
                totalArticles += articles;
            }

            if (authorEntries != null) {
                totalAuthorEntries += authorEntries;
            }
        }

        double avgArticlesPerYear = activeYears == 0 ? 0 : (double) totalArticles / activeYears;
        double avgAuthorEntriesPerYear = activeYears == 0 ? 0 : (double) totalAuthorEntries / activeYears;

        return new VenueAggregate(
                totalArticles,
                totalAuthorEntries,
                activeYears,
                avgArticlesPerYear,
                avgAuthorEntriesPerYear
        );
    }

    private void clearChart() {
        if (comparisonLineChart != null) {
            comparisonLineChart.getData().clear();
        }

        clearBarCharts();

        if (venueScatterChart != null) {
            venueScatterChart.getData().clear();
        }

        configureYearAxis(yearAxis, MIN_YEAR, LocalDate.now().getYear());
        configureValueAxis(valueAxis, 10);

        configureValueAxis(totalArticlesValueAxis, 10);
        configureValueAxis(avgArticlesValueAxis, 10);
        configureValueAxis(avgAuthorEntriesValueAxis, 10);

        configureValueAxis(scatterArticlesAxis, 10);
        configureValueAxis(scatterAuthorsAxis, 10);

        updateCustomLegend(null);
    }

    private void clearBarCharts() {
        if (totalArticlesBarChart != null) {
            totalArticlesBarChart.getData().clear();
        }

        if (avgArticlesBarChart != null) {
            avgArticlesBarChart.getData().clear();
        }

        if (avgAuthorEntriesBarChart != null) {
            avgAuthorEntriesBarChart.getData().clear();
        }
    }

    private void configureYearAxis(NumberAxis axis, Integer minYear, Integer maxYear) {
        if (axis == null) {
            return;
        }

        if (minYear == null || maxYear == null) {
            axis.setAutoRanging(true);
            return;
        }

        int lowerYear = minYear;
        int upperYear = maxYear;

        if (lowerYear == upperYear) {
            lowerYear = lowerYear - 1;
            upperYear = upperYear + 1;
        }

        int yearRange = upperYear - lowerYear;
        int tickUnit = calculateYearTickUnit(yearRange);

        axis.setAutoRanging(false);
        axis.setLowerBound(lowerYear);
        axis.setUpperBound(upperYear);
        axis.setTickUnit(tickUnit);
        axis.setMinorTickVisible(false);
    }

    private int calculateYearTickUnit(int yearRange) {
        if (yearRange <= 10) {
            return 1;
        }

        if (yearRange <= 25) {
            return 5;
        }

        if (yearRange <= 60) {
            return 10;
        }

        return 20;
    }

    private void configureValueAxis(NumberAxis axis, double maxValue) {
        if (axis == null) {
            return;
        }

        double upperBound = calculateNiceUpperBound(maxValue);
        double tickUnit = calculateNiceTickUnit(upperBound);

        axis.setAutoRanging(false);
        axis.setLowerBound(0);
        axis.setUpperBound(upperBound);
        axis.setTickUnit(tickUnit);
        axis.setMinorTickVisible(false);
    }

    private double calculateNiceUpperBound(double maxValue) {
        if (maxValue <= 0) {
            return 10;
        }

        double paddedValue = maxValue * 1.08;
        double roughTickUnit = paddedValue / 7.0;
        double tickUnit = calculateNiceRawTickUnit(roughTickUnit);

        return Math.ceil(paddedValue / tickUnit) * tickUnit;
    }

    private double calculateNiceTickUnit(double upperBound) {
        if (upperBound <= 0) {
            return 1;
        }

        double roughTickUnit = upperBound / 7.0;
        return calculateNiceRawTickUnit(roughTickUnit);
    }

    private double calculateNiceRawTickUnit(double value) {
        if (value <= 0) {
            return 1;
        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;

        double niceNormalized;

        if (normalized <= 1) {
            niceNormalized = 1;
        } else if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 2.5) {
            niceNormalized = 2.5;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }

        return niceNormalized * magnitude;
    }

    private String getChartColor(int index) {
        return CHART_COLORS[index % CHART_COLORS.length];
    }

    private void runAfterChartRender(Runnable action) {
        Platform.runLater(() -> Platform.runLater(action));
    }

    private void applyLineSeriesColor(XYChart.Series<Number, Number> series, int colorIndex) {
        if (series == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        Node line = series.getNode();

        if (line != null) {
            line.setStyle(
                    "-fx-stroke: " + color + ";" +
                            "-fx-stroke-width: 2.4px;"
            );
        }

        for (XYChart.Data<Number, Number> data : series.getData()) {
            Node symbol = data.getNode();

            if (symbol != null) {
                symbol.setStyle(
                        "-fx-background-color: " + color + ", white;" +
                                "-fx-background-insets: 0, 2;" +
                                "-fx-background-radius: 7px;" +
                                "-fx-padding: 4px;"
                );
            }
        }
    }

    private void applyBarColor(XYChart.Data<String, Number> dataPoint, int colorIndex) {
        if (dataPoint == null || dataPoint.getNode() == null) {
            return;
        }

        String color = getChartColor(colorIndex);

        dataPoint.getNode().setStyle(
                "-fx-bar-fill: " + color + ";"
        );
    }

    private void applyScatterColor(XYChart.Data<Number, Number> dataPoint, int colorIndex) {
        if (dataPoint == null || dataPoint.getNode() == null) {
            return;
        }

        String color = getChartColor(colorIndex);

        dataPoint.getNode().setStyle(
                "-fx-background-color: " + color + ", white;" +
                        "-fx-background-insets: 0, 2;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 5px;"
        );
    }

    private void updateCustomLegend(List<VenueChartSeries> allSeries) {
        updateLegendFlow(lineChartLegendBox, lineChartLegendFlow, allSeries);
        updateLegendFlow(barChartsLegendBox, barChartsLegendFlow, allSeries);
        updateLegendFlow(scatterLegendBox, scatterLegendFlow, allSeries);
    }

    private void updateLegendFlow(
            VBox legendBox,
            FlowPane legendFlow,
            List<VenueChartSeries> allSeries
    ) {
        if (legendBox == null || legendFlow == null) {
            return;
        }

        legendFlow.getChildren().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            legendBox.setVisible(false);
            legendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (VenueChartSeries venueSeries : allSeries) {
            String color = getChartColor(index);
            String fullName = venueSeries.name();
            String shortName = shortenSeriesName(fullName);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-background-radius: 999px;"
            );

            Label nameLabel = new Label(shortName);
            nameLabel.setStyle(
                    "-fx-text-fill: #0f4c81;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: 700;"
            );

            if (fullName != null && fullName.length() > 45) {
                Tooltip.install(nameLabel, new Tooltip(fullName));
            }

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.95);" +
                            "-fx-border-color: #b9daf7;" +
                            "-fx-border-radius: 999px;" +
                            "-fx-background-radius: 999px;" +
                            "-fx-padding: 5 9 5 9;"
            );

            legendFlow.getChildren().add(legendItem);
            index++;
        }

        legendBox.setVisible(true);
        legendBox.setManaged(true);
    }

    private YearRange getSelectedYearRange() {
        Integer startYear = getComboBoxYearValue(fromYearComboBox);
        Integer endYear = getComboBoxYearValue(toYearComboBox);

        if (startYear != null && endYear != null && endYear < startYear) {
            throw new IllegalArgumentException("Το To year πρέπει να είναι μεγαλύτερο ή ίσο από το From year.");
        }

        return new YearRange(startYear, endYear);
    }

    private Integer getComboBoxYearValue(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return null;
        }

        return comboBox.getValue();
    }

    private String getSelectedVenueType() {
        if (venueTypeComboBox == null) {
            return null;
        }

        return venueTypeComboBox.getValue();
    }

    private boolean alreadySelected(SelectedVenue newSelectedVenue) {
        if (selectedVenuesListView == null) {
            return false;
        }

        for (SelectedVenue existing : selectedVenuesListView.getItems()) {
            if (existing.type().equals(newSelectedVenue.type())
                    && getVenueId(existing.venue()) == getVenueId(newSelectedVenue.venue())) {
                return true;
            }
        }

        return false;
    }

    private int getVenueId(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            return journal.journalId() == null ? -1 : journal.journalId();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            return conference.conferenceId() == null ? -1 : conference.conferenceId();
        }

        return -1;
    }

    private int getJournalId(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalId() != null && journal.journalId() > 0) {
                return journal.journalId();
            }
        }

        throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο journalId.");
    }

    private int getConferenceId(Object venue) {
        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.conferenceId() != null && conference.conferenceId() > 0) {
                return conference.conferenceId();
            }
        }

        throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο conferenceId.");
    }

    private String getVenueDisplayName(SelectedVenue selectedVenue) {
        return selectedVenue.type() + ": " + getRawVenueDisplayName(selectedVenue.venue());
    }

    private String getRawVenueDisplayName(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalName() != null && !journal.journalName().isBlank()) {
                return journal.journalName();
            }

            return "Journal #" + journal.journalId();
        }

        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.acronym() != null && !conference.acronym().isBlank()) {
                if (conference.conferenceTitle() != null && !conference.conferenceTitle().isBlank()) {
                    return conference.acronym() + " - " + conference.conferenceTitle();
                }

                return conference.acronym();
            }

            if (conference.conferenceTitle() != null && !conference.conferenceTitle().isBlank()) {
                return conference.conferenceTitle();
            }

            return "Conference #" + conference.conferenceId();
        }

        return "Unknown venue";
    }

    private String shortenSeriesName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= 45) {
            return name;
        }

        return name.substring(0, 42) + "...";
    }

    private Integer extractYear(Object stat) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            return journalStat.year();
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            return conferenceStat.year();
        }

        return null;
    }

    private Long extractMetricValue(Object stat, String metric) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return journalStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return journalStat.totalAuthorOccurrences();
            }

            if (METRIC_DISTINCT_AUTHORS.equals(metric)) {
                return journalStat.distinctAuthors();
            }
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return conferenceStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return conferenceStat.totalAuthorOccurrences();
            }

            if (METRIC_DISTINCT_AUTHORS.equals(metric)) {
                return conferenceStat.distinctAuthors();
            }
        }

        return null;
    }

    private void setLoading(boolean loading) {
        if (venueTypeComboBox != null) {
            venueTypeComboBox.setDisable(loading);
        }

        if (venueSearchField != null) {
            venueSearchField.setDisable(loading);
        }

        if (searchButton != null) {
            searchButton.setDisable(loading);
        }

        if (addSelectedButton != null) {
            addSelectedButton.setDisable(loading);
        }

        if (removeSelectedButton != null) {
            removeSelectedButton.setDisable(loading);
        }

        if (clearButton != null) {
            clearButton.setDisable(loading);
        }

        if (loadChartButton != null) {
            loadChartButton.setDisable(loading);
        }

        if (metricComboBox != null) {
            metricComboBox.setDisable(loading);
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.setDisable(loading);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(loading);
        }

        if (searchResultsListView != null) {
            searchResultsListView.setDisable(loading);
        }

        if (selectedVenuesListView != null) {
            selectedVenuesListView.setDisable(loading);
        }

        if (comparisonLineChart != null) {
            comparisonLineChart.setDisable(loading);
        }

        if (totalArticlesBarChart != null) {
            totalArticlesBarChart.setDisable(loading);
        }

        if (avgArticlesBarChart != null) {
            avgArticlesBarChart.setDisable(loading);
        }

        if (avgAuthorEntriesBarChart != null) {
            avgAuthorEntriesBarChart.setDisable(loading);
        }

        if (venueScatterChart != null) {
            venueScatterChart.setDisable(loading);
        }
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Άγνωστο σφάλμα." : message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "-" : message);
        alert.showAndWait();
    }

    private record SelectedVenue(
            String type,
            Object venue
    ) {
    }

    private record VenueChartSeries(
            String name,
            String type,
            List<Object> yearlyStats
    ) {
    }

    private record VenueAggregate(
            long totalArticles,
            long totalAuthorEntries,
            int activeYears,
            double avgArticlesPerYear,
            double avgAuthorEntriesPerYear
    ) {
    }

    private record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }
}