package controllers.charts;

import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ScatterPlotsController {

    private static final String TYPE_JOURNAL = "Journal";
    private static final String TYPE_CONFERENCE = "Conference";

    private static final String METRIC_ARTICLES = "Published articles";
    private static final String METRIC_AUTHOR_ENTRIES = "Total author entries";

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SELECTED_VENUES = 10;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;
    private static final int DEFAULT_RANKING_LIMIT = 150;

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

    private static final String[] RANKING_METRICS = {
            "Total Docs",
            "Total Docs 3y",
            "Total Refs",
            "Total Cites 3y",
            "Citable Docs 3y",
            "Cites / Doc 2y",
            "Refs / Doc",
            "SJR",
            "Cite Score",
            "H index"
    };

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    private Object selectedSearchVenue;
    private PauseTransition searchDebounce;
    private Task<List<Object>> venueSearchTask;

    private String activeVenueKey;

    private final List<ChartHighlightHandle> chartHighlightHandles = new ArrayList<>();
    private final List<LegendHighlightHandle> legendHighlightHandles = new ArrayList<>();

    @FXML
    private ComboBox<String> scatterVenueTypeComboBox;

    @FXML
    private TextField scatterVenueSearchField;

    @FXML
    private Button scatterAddSelectedButton;

    @FXML
    private Button scatterRemoveSelectedButton;

    @FXML
    private Button scatterClearSelectedButton;

    @FXML
    private Button scatterLoadVenueButton;

    @FXML
    private VBox scatterSearchResultsContainer;

    @FXML
    private ListView<Object> scatterSearchResultsListView;

    @FXML
    private ListView<SelectedVenue> scatterSelectedVenuesListView;

    @FXML
    private ComboBox<Integer> scatterFromYearComboBox;

    @FXML
    private ComboBox<Integer> scatterToYearComboBox;

    @FXML
    private Label venueScatterStatusLabel;

    @FXML
    private ScatterChart<Number, Number> venueScatterChart;

    @FXML
    private NumberAxis scatterArticlesAxis;

    @FXML
    private NumberAxis scatterAuthorsAxis;

    @FXML
    private VBox venueScatterLegendBox;

    @FXML
    private FlowPane venueScatterLegendFlow;

    @FXML
    private ComboBox<String> rankingXMetricComboBox;

    @FXML
    private ComboBox<String> rankingYMetricComboBox;

    @FXML
    private ComboBox<Integer> rankingLimitComboBox;

    @FXML
    private TextField rankingJournalFilterField;

    @FXML
    private Button loadRankingScatterButton;

    @FXML
    private Button clearRankingScatterButton;

    @FXML
    private Label rankingStatusLabel;

    @FXML
    private ScatterChart<Number, Number> journalRankingScatterChart;

    @FXML
    private NumberAxis rankingXAxis;

    @FXML
    private NumberAxis rankingYAxis;

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupSearchResultsListView();
        setupSelectedVenuesListView();
        setupVenueScatterChart();

        setupRankingControls();
        setupRankingScatterChart();

        selectedSearchVenue = null;
        clearSearchResults();
        clearVenueScatterOnly();
        clearRankingScatterOnly();

        setVenueStatus("Choose journals/conferences and load venue scatter.");
        setRankingStatus("Choose ranking metrics and load journal ranking scatter.");
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
        String searchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();

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
                showError("Λάθος αναζήτηση", "Πρέπει να γράψεις τουλάχιστον 3 χαρακτήρες.");
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
            setVenueLoading(true);
        }

        venueSearchTask.setOnSucceeded(event -> {
            if (showAlerts) {
                setVenueLoading(false);
            }

            if (venueSearchTask.isCancelled()) {
                return;
            }

            String currentSearchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();
            String currentVenueType = getSelectedVenueType();

            if (!requestedSearchText.equals(currentSearchText)) {
                return;
            }

            if (!requestedVenueType.equals(currentVenueType)) {
                return;
            }

            List<Object> results = sortVenueResultsByRelevance(venueSearchTask.getValue(), requestedSearchText);
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
                setVenueLoading(false);
            }

            Throwable exception = venueSearchTask.getException();

            if (showAlerts) {
                showError("Σφάλμα αναζήτησης", exception == null ? null : exception.getMessage());
            } else {
                clearSearchResults();
            }
        });

        startBackgroundTask(venueSearchTask, "scatter-venue-search-task");
    }

    @FXML
    private void addSelectedVenue() {
        addSelectedVenueFromSearch(selectedSearchVenue, true);
    }

    private void addSelectedVenueFromSearch(Object venue, boolean showMessages) {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        if (venue == null) {
            if (showMessages) {
                showError(
                        "Δεν επιλέχθηκε venue",
                        "Πρέπει πρώτα να επιλέξεις ένα αποτέλεσμα από τη λίστα."
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

        if (scatterSelectedVenuesListView.getItems().size() >= MAX_SELECTED_VENUES) {
            if (showMessages) {
                showError(
                        "Πολλά venues",
                        "Για να παραμένει ευανάγνωστο το scatter plot, μπορείς να επιλέξεις μέχρι "
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

        scatterSelectedVenuesListView.getItems().add(selectedVenue);

        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.getSelectionModel().clearSelection();
        }

        selectedSearchVenue = null;
    }

    @FXML
    private void removeSelectedVenue() {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        SelectedVenue selected = scatterSelectedVenuesListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            scatterSelectedVenuesListView.getItems().remove(selected);
        }
    }

    @FXML
    private void clearVenueScatter() {
        stopCurrentVenueSearch();

        if (scatterVenueSearchField != null) {
            scatterVenueSearchField.clear();
        }

        clearSearchResults();

        if (scatterSelectedVenuesListView != null) {
            scatterSelectedVenuesListView.getItems().clear();
        }

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.getSelectionModel().clearSelection();
        }

        if (scatterToYearComboBox != null) {
            scatterToYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        selectedSearchVenue = null;
        clearVenueScatterOnly();
        setVenueStatus("Choose journals/conferences and load venue scatter.");
    }

    @FXML
    private void loadVenueScatter() {
        if (scatterSelectedVenuesListView == null || scatterSelectedVenuesListView.getItems().isEmpty()) {
            showError("Δεν υπάρχουν επιλεγμένα venues", "Πρέπει να προσθέσεις τουλάχιστον ένα journal ή conference.");
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Λάθος χρονιές", exception.getMessage());
            return;
        }

        List<SelectedVenue> selectedVenues = new ArrayList<>(scatterSelectedVenuesListView.getItems());

        Task<List<VenueScatterSeries>> task = new Task<>() {
            @Override
            protected List<VenueScatterSeries> call() {
                List<VenueScatterSeries> chartSeries = new ArrayList<>();

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
                                new VenueScatterSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        journalId,
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
                                new VenueScatterSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        conferenceId,
                                        yearlyStats
                                )
                        );
                    }
                }

                return chartSeries;
            }
        };

        setVenueLoading(true);
        setVenueStatus("Loading venue scatter plot...");

        task.setOnSucceeded(event -> {
            setVenueLoading(false);

            List<VenueScatterSeries> chartData = task.getValue();

            if (chartData == null || chartData.isEmpty()) {
                clearVenueScatterOnly();
                setVenueStatus("No venue scatter data found.");
                return;
            }

            updateVenueScatterChart(chartData);
            setVenueStatus("Loaded " + chartData.size() + " venue series.");
        });

        task.setOnFailed(event -> {
            setVenueLoading(false);
            Throwable exception = task.getException();
            setVenueStatus("Failed to load venue scatter plot.");
            showError("Σφάλμα φόρτωσης scatter", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "scatter-venue-load-task");
    }

    @FXML
    private void loadRankingScatter() {
        String xMetric = rankingXMetricComboBox == null ? null : rankingXMetricComboBox.getValue();
        String yMetric = rankingYMetricComboBox == null ? null : rankingYMetricComboBox.getValue();

        if (xMetric == null || xMetric.isBlank()) {
            showError("Δεν επιλέχθηκε X metric", "Πρέπει να επιλέξεις metric για τον X-axis.");
            return;
        }

        if (yMetric == null || yMetric.isBlank()) {
            showError("Δεν επιλέχθηκε Y metric", "Πρέπει να επιλέξεις metric για τον Y-axis.");
            return;
        }

        if (xMetric.equals(yMetric)) {
            showError("Ίδια metrics", "Διάλεξε διαφορετικό X metric και Y metric για να έχει νόημα το scatter plot.");
            return;
        }

        Integer limit = rankingLimitComboBox == null ? DEFAULT_RANKING_LIMIT : rankingLimitComboBox.getValue();

        if (limit == null || limit <= 0) {
            limit = DEFAULT_RANKING_LIMIT;
        }

        String journalFilter = rankingJournalFilterField == null ? "" : rankingJournalFilterField.getText().trim();
        Integer requestedLimit = limit;

        Task<List<RankingScatterPoint>> task = new Task<>() {
            @Override
            protected List<RankingScatterPoint> call() {
                return loadJournalRankingScatterData(
                        xMetric,
                        yMetric,
                        journalFilter,
                        requestedLimit
                );
            }
        };

        setRankingLoading(true);
        setRankingStatus("Loading journal ranking scatter...");

        task.setOnSucceeded(event -> {
            setRankingLoading(false);

            List<RankingScatterPoint> points = task.getValue();

            if (points == null || points.isEmpty()) {
                clearRankingScatterOnly();
                setRankingStatus("No ranking scatter data found. Check if the Service/DAO query has been connected.");
                showInfo(
                        "Δεν βρέθηκαν δεδομένα",
                        "Το tab είναι έτοιμο, αλλά πρέπει να συνδεθεί με query στο JournalService/DAO για να επιστρέφει πραγματικά δεδομένα."
                );
                return;
            }

            updateRankingScatterChart(points, xMetric, yMetric);
            setRankingStatus("Loaded " + points.size() + " journal points.");
        });

        task.setOnFailed(event -> {
            setRankingLoading(false);
            Throwable exception = task.getException();
            setRankingStatus("Failed to load ranking scatter.");
            showError("Σφάλμα φόρτωσης ranking scatter", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "ranking-scatter-load-task");
    }

    @FXML
    private void clearRankingScatter() {
        if (rankingJournalFilterField != null) {
            rankingJournalFilterField.clear();
        }

        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.getSelectionModel().select("Total Docs");
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.getSelectionModel().select("Cites / Doc 2y");
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.getSelectionModel().select(Integer.valueOf(DEFAULT_RANKING_LIMIT));
        }

        clearRankingScatterOnly();
        setRankingStatus("Choose ranking metrics and load journal ranking scatter.");
    }

    private void setupVenueTypeComboBox() {
        if (scatterVenueTypeComboBox == null) {
            return;
        }

        scatterVenueTypeComboBox.getItems().setAll(TYPE_JOURNAL, TYPE_CONFERENCE);
        scatterVenueTypeComboBox.getSelectionModel().select(TYPE_JOURNAL);

        scatterVenueTypeComboBox.setOnAction(event -> {
            stopCurrentVenueSearch();

            selectedSearchVenue = null;
            clearSearchResults();

            String searchText = scatterVenueSearchField == null ? "" : scatterVenueSearchField.getText().trim();

            if (searchText.length() >= MIN_SEARCH_LENGTH && searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });
    }

    private void setupYearComboBoxes() {
        setupYearComboBox(scatterFromYearComboBox);
        setupYearComboBox(scatterToYearComboBox);

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
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
        if (scatterToYearComboBox == null) {
            return;
        }

        Integer currentToYear = scatterToYearComboBox.getValue();

        scatterToYearComboBox.getItems().setAll(buildYearList(fromYear));

        if (currentToYear != null && fromYear != null && currentToYear < fromYear) {
            scatterToYearComboBox.getSelectionModel().clearSelection();
            return;
        }

        if (currentToYear != null && scatterToYearComboBox.getItems().contains(currentToYear)) {
            scatterToYearComboBox.getSelectionModel().select(currentToYear);
        }
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchVenuesRealtime());

        if (scatterVenueSearchField == null) {
            return;
        }

        scatterVenueSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        scatterVenueSearchField.setOnAction(event -> searchVenues());
    }

    private void setupSearchResultsListView() {
        if (scatterSearchResultsListView == null) {
            return;
        }

        scatterSearchResultsListView.setPlaceholder(new Label("Search results will appear here."));

        scatterSearchResultsListView.setCellFactory(listView -> {
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

        scatterSearchResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> selectedSearchVenue = selected
        );
    }

    private void setupSelectedVenuesListView() {
        if (scatterSelectedVenuesListView == null) {
            return;
        }

        scatterSelectedVenuesListView.setPlaceholder(new Label("Selected venues will appear here."));

        scatterSelectedVenuesListView.setCellFactory(listView -> {
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

                    scatterSelectedVenuesListView.getItems().remove(cell.getItem());
                }
            });

            return cell;
        });
    }

    private void setupVenueScatterChart() {
        if (venueScatterChart != null) {
            venueScatterChart.setAnimated(false);
            venueScatterChart.setLegendVisible(false);
            venueScatterChart.setTitle("");
        }

        if (scatterArticlesAxis != null) {
            scatterArticlesAxis.setAutoRanging(false);
            scatterArticlesAxis.setForceZeroInRange(true);
            scatterArticlesAxis.setMinorTickVisible(false);
            scatterArticlesAxis.setLabel("Articles / year");
        }

        if (scatterAuthorsAxis != null) {
            scatterAuthorsAxis.setAutoRanging(false);
            scatterAuthorsAxis.setForceZeroInRange(true);
            scatterAuthorsAxis.setMinorTickVisible(false);
            scatterAuthorsAxis.setLabel("Avg authors / article");
        }
    }

    private void setupRankingControls() {
        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.getItems().setAll(RANKING_METRICS);
            rankingXMetricComboBox.getSelectionModel().select("Total Docs");
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.getItems().setAll(RANKING_METRICS);
            rankingYMetricComboBox.getSelectionModel().select("Cites / Doc 2y");
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.getItems().setAll(50, 100, 150, 200, 300, 500);
            rankingLimitComboBox.getSelectionModel().select(Integer.valueOf(DEFAULT_RANKING_LIMIT));
        }
    }

    private void setupRankingScatterChart() {
        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.setAnimated(false);
            journalRankingScatterChart.setLegendVisible(false);
            journalRankingScatterChart.setTitle("");
        }

        if (rankingXAxis != null) {
            rankingXAxis.setAutoRanging(false);
            rankingXAxis.setForceZeroInRange(true);
            rankingXAxis.setMinorTickVisible(false);
            rankingXAxis.setLabel("X metric");
        }

        if (rankingYAxis != null) {
            rankingYAxis.setAutoRanging(false);
            rankingYAxis.setForceZeroInRange(true);
            rankingYAxis.setMinorTickVisible(false);
            rankingYAxis.setLabel("Y metric");
        }
    }

    private void renderSearchResults(List<Object> results) {
        if (scatterSearchResultsListView == null) {
            return;
        }

        scatterSearchResultsListView.getItems().clear();
        scatterSearchResultsListView.getSelectionModel().clearSelection();
        selectedSearchVenue = null;

        setSearchResultsVisible(true);

        if (results == null || results.isEmpty()) {
            scatterSearchResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        scatterSearchResultsListView.setPlaceholder(new Label("No matching results"));
        scatterSearchResultsListView.getItems().setAll(results);
    }

    private void setSearchResultsVisible(boolean visible) {
        if (scatterSearchResultsContainer != null) {
            scatterSearchResultsContainer.setVisible(visible);
            scatterSearchResultsContainer.setManaged(visible);
        }
    }

    private void clearSearchResults() {
        clearSearchResultsOnly();
        setSearchResultsVisible(false);
    }

    private void clearSearchResultsOnly() {
        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.getItems().clear();
            scatterSearchResultsListView.getSelectionModel().clearSelection();
            scatterSearchResultsListView.setPlaceholder(new Label("Search results will appear here."));
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

    private void updateVenueScatterChart(List<VenueScatterSeries> allSeries) {
        clearVenueScatterOnly();

        if (allSeries == null || allSeries.isEmpty()) {
            return;
        }

        double maxArticlesPerYear = 0;
        double maxAvgAuthorsPerArticle = 0;
        int seriesIndex = 0;
        int pointCount = 0;

        for (VenueScatterSeries venueSeries : allSeries) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(shortenName(venueSeries.name(), 45));

            for (Object stat : venueSeries.yearlyStats()) {
                Integer year = extractYear(stat);
                Long articlesValue = extractMetricValue(stat, METRIC_ARTICLES);
                Long authorEntriesValue = extractMetricValue(stat, METRIC_AUTHOR_ENTRIES);

                if (year == null || articlesValue == null || authorEntriesValue == null) {
                    continue;
                }

                if (articlesValue <= 0) {
                    continue;
                }

                double articlesPerYear = articlesValue;
                double avgAuthorsPerArticle = (double) authorEntriesValue / articlesValue;

                maxArticlesPerYear = Math.max(maxArticlesPerYear, articlesPerYear);
                maxAvgAuthorsPerArticle = Math.max(maxAvgAuthorsPerArticle, avgAuthorsPerArticle);

                VenueScatterPoint point = new VenueScatterPoint(
                        venueSeries.name(),
                        venueSeries.type(),
                        venueSeries.venueId(),
                        year,
                        articlesPerYear,
                        avgAuthorsPerArticle
                );

                XYChart.Data<Number, Number> dataPoint =
                        new XYChart.Data<>(articlesPerYear, avgAuthorsPerArticle);

                dataPoint.setExtraValue(point);
                series.getData().add(dataPoint);
                pointCount++;
            }

            if (!series.getData().isEmpty() && venueScatterChart != null) {
                venueScatterChart.getData().add(series);

                final int colorIndex = seriesIndex;
                final VenueScatterSeries currentVenueSeries = venueSeries;
                runAfterChartRender(() -> applyVenueScatterColor(series, colorIndex, currentVenueSeries));
            }

            seriesIndex++;
        }

        configureValueAxis(scatterArticlesAxis, maxArticlesPerYear);
        configureValueAxis(scatterAuthorsAxis, maxAvgAuthorsPerArticle);

        if (venueScatterChart != null) {
            venueScatterChart.setTitle("Articles / year vs average authors / article");
        }

        updateVenueLegend(allSeries);
        setVenueStatus("Loaded " + pointCount + " yearly points.");
    }

    private void clearVenueScatterOnly() {
        activeVenueKey = null;
        chartHighlightHandles.clear();
        legendHighlightHandles.clear();

        if (venueScatterChart != null) {
            venueScatterChart.getData().clear();
            venueScatterChart.setTitle("");
        }

        configureValueAxis(scatterArticlesAxis, 10);
        configureValueAxis(scatterAuthorsAxis, 10);

        updateVenueLegend(null);
    }

    private List<RankingScatterPoint> loadJournalRankingScatterData(
            String xMetric,
            String yMetric,
            String journalFilter,
            Integer limit
    ) {
        List<Object> rawRows = invokeRankingServiceMethod(
                List.of(
                        "getJournalRankingScatterData",
                        "getJournalRankingScatter",
                        "getJournalRankingsForScatter",
                        "getJournalRankingMetrics",
                        "getJournalRankings"
                ),
                xMetric,
                yMetric,
                journalFilter,
                limit
        );

        return convertRawRowsToRankingPoints(rawRows, xMetric, yMetric, journalFilter, limit);
    }

    private List<Object> invokeRankingServiceMethod(
            List<String> methodNames,
            String xMetric,
            String yMetric,
            String journalFilter,
            Integer limit
    ) {
        if (methodNames == null || methodNames.isEmpty()) {
            return new ArrayList<>();
        }

        for (String methodName : methodNames) {
            List<Object> result;

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, String.class, String.class, Integer.class},
                    new Object[]{xMetric, yMetric, journalFilter, limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, String.class, String.class, int.class},
                    new Object[]{xMetric, yMetric, journalFilter, limit == null ? DEFAULT_RANKING_LIMIT : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, String.class, Integer.class},
                    new Object[]{xMetric, yMetric, limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, String.class, int.class},
                    new Object[]{xMetric, yMetric, limit == null ? DEFAULT_RANKING_LIMIT : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, Integer.class},
                    new Object[]{journalFilter, limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{String.class, int.class},
                    new Object[]{journalFilter, limit == null ? DEFAULT_RANKING_LIMIT : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{Integer.class},
                    new Object[]{limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{int.class},
                    new Object[]{limit == null ? DEFAULT_RANKING_LIMIT : limit}
            );

            if (result != null) {
                return result;
            }

            result = tryInvokeServiceMethod(
                    journalService,
                    methodName,
                    new Class<?>[]{},
                    new Object[]{}
            );

            if (result != null) {
                return result;
            }
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> tryInvokeServiceMethod(
            Object service,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] arguments
    ) {
        try {
            Method method = service.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(service, arguments);

            if (result instanceof List<?> list) {
                return new ArrayList<>((List<Object>) list);
            }

            return new ArrayList<>();
        } catch (NoSuchMethodException exception) {
            return null;
        } catch (Exception exception) {
            throw new RuntimeException("Could not execute service method: " + methodName, exception);
        }
    }

    private List<RankingScatterPoint> convertRawRowsToRankingPoints(
            List<Object> rawRows,
            String xMetric,
            String yMetric,
            String journalFilter,
            Integer limit
    ) {
        if (rawRows == null || rawRows.isEmpty()) {
            return new ArrayList<>();
        }

        List<RankingScatterPoint> points = new ArrayList<>();

        for (Object row : rawRows) {
            if (row == null) {
                continue;
            }

            String journalName = readStringValue(
                    row,
                    List.of(
                            "journalName",
                            "journal_name",
                            "journal",
                            "title",
                            "name",
                            "label"
                    )
            );

            Double xValue = readDoubleValue(
                    row,
                    metricValueNames(xMetric, true)
            );

            Double yValue = readDoubleValue(
                    row,
                    metricValueNames(yMetric, false)
            );

            if (journalName == null || journalName.isBlank()) {
                journalName = "Unknown journal";
            }

            if (journalFilter != null
                    && !journalFilter.isBlank()
                    && !journalName.toLowerCase(Locale.ROOT).contains(journalFilter.toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (xValue == null || yValue == null) {
                continue;
            }

            if (Double.isNaN(xValue) || Double.isInfinite(xValue)) {
                continue;
            }

            if (Double.isNaN(yValue) || Double.isInfinite(yValue)) {
                continue;
            }

            points.add(
                    new RankingScatterPoint(
                            journalName,
                            xMetric,
                            yMetric,
                            xValue,
                            yValue
                    )
            );
        }

        points.sort(
                Comparator.comparingDouble((RankingScatterPoint point) -> point.xValue() + point.yValue())
                        .reversed()
        );

        int safeLimit = limit == null || limit <= 0 ? DEFAULT_RANKING_LIMIT : limit;

        if (points.size() > safeLimit) {
            return new ArrayList<>(points.subList(0, safeLimit));
        }

        return points;
    }

    private List<String> metricValueNames(String metric, boolean xMetric) {
        List<String> names = new ArrayList<>();

        if (xMetric) {
            names.add("xValue");
            names.add("x_value");
            names.add("x");
        } else {
            names.add("yValue");
            names.add("y_value");
            names.add("y");
        }

        if ("Total Docs".equals(metric)) {
            names.addAll(List.of("totalDocs", "total_docs", "totalDocuments", "total_documents"));
        } else if ("Total Docs 3y".equals(metric)) {
            names.addAll(List.of("totalDocs3y", "total_docs_3y", "totalDocuments3y", "total_documents_3y"));
        } else if ("Total Refs".equals(metric)) {
            names.addAll(List.of("totalRefs", "total_refs", "totalReferences", "total_references"));
        } else if ("Total Cites 3y".equals(metric)) {
            names.addAll(List.of("totalCites3y", "total_cites_3y", "totalCitations3y", "total_citations_3y"));
        } else if ("Citable Docs 3y".equals(metric)) {
            names.addAll(List.of("citableDocs3y", "citable_docs_3y", "citableDocuments3y", "citable_documents_3y"));
        } else if ("Cites / Doc 2y".equals(metric)) {
            names.addAll(List.of("citesDoc2y", "cites_doc_2y", "citesPerDoc2y", "cites_per_doc_2y", "citesPerDocument2y"));
        } else if ("Refs / Doc".equals(metric)) {
            names.addAll(List.of("refsDoc", "refs_doc", "refsPerDoc", "refs_per_doc", "refsPerDocument"));
        } else if ("SJR".equals(metric)) {
            names.addAll(List.of("sjr", "sjrIndex", "sjr_index"));
        } else if ("Cite Score".equals(metric)) {
            names.addAll(List.of("citeScore", "cite_score", "citescore"));
        } else if ("H index".equals(metric)) {
            names.addAll(List.of("hIndex", "h_index", "hindex"));
        }

        return names;
    }

    private void updateRankingScatterChart(List<RankingScatterPoint> points, String xMetric, String yMetric) {
        clearRankingScatterOnly();

        if (points == null || points.isEmpty() || journalRankingScatterChart == null) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Journals");

        double maxX = 0;
        double maxY = 0;

        for (RankingScatterPoint point : points) {
            maxX = Math.max(maxX, point.xValue());
            maxY = Math.max(maxY, point.yValue());

            XYChart.Data<Number, Number> dataPoint =
                    new XYChart.Data<>(point.xValue(), point.yValue());

            dataPoint.setExtraValue(point);
            series.getData().add(dataPoint);
        }

        journalRankingScatterChart.getData().add(series);

        configureValueAxis(rankingXAxis, maxX);
        configureValueAxis(rankingYAxis, maxY);

        if (rankingXAxis != null) {
            rankingXAxis.setLabel(xMetric);
        }

        if (rankingYAxis != null) {
            rankingYAxis.setLabel(yMetric);
        }

        journalRankingScatterChart.setTitle(xMetric + " vs " + yMetric);

        runAfterChartRender(() -> applyRankingScatterStyle(series));
    }

    private void clearRankingScatterOnly() {
        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.getData().clear();
            journalRankingScatterChart.setTitle("");
        }

        configureValueAxis(rankingXAxis, 10);
        configureValueAxis(rankingYAxis, 10);

        if (rankingXAxis != null) {
            rankingXAxis.setLabel("X metric");
        }

        if (rankingYAxis != null) {
            rankingYAxis.setLabel("Y metric");
        }
    }

    private void applyRankingScatterStyle(XYChart.Series<Number, Number> series) {
        if (series == null) {
            return;
        }

        String color = "#1f5fa8";

        for (XYChart.Data<Number, Number> data : series.getData()) {
            Node node = data.getNode();

            if (node == null) {
                continue;
            }

            node.setStyle(getScatterStyle(color, false));
            node.setCursor(Cursor.HAND);

            Object extraValue = data.getExtraValue();

            if (extraValue instanceof RankingScatterPoint point) {
                Tooltip.install(
                        node,
                        new Tooltip(
                                point.journalName()
                                        + "\n" + point.xMetric() + ": " + formatNumber(point.xValue())
                                        + "\n" + point.yMetric() + ": " + formatNumber(point.yValue())
                        )
                );
            }

            node.setOnMouseEntered(event -> {
                node.setStyle(getScatterStyle(color, true));
                node.toFront();
                event.consume();
            });

            node.setOnMouseExited(event -> {
                node.setStyle(getScatterStyle(color, false));
                event.consume();
            });
        }
    }

    private void applyVenueScatterColor(
            XYChart.Series<Number, Number> series,
            int colorIndex,
            VenueScatterSeries venueSeries
    ) {
        if (series == null || venueSeries == null) {
            return;
        }

        String color = getChartColor(colorIndex);
        String venueKey = getVenueKey(venueSeries);

        Runnable normalStyle = () -> {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node scatterNode = data.getNode();

                if (scatterNode != null) {
                    scatterNode.setStyle(getScatterStyle(color, false));

                    Object extraValue = data.getExtraValue();

                    if (extraValue instanceof VenueScatterPoint point) {
                        Tooltip.install(
                                scatterNode,
                                new Tooltip(
                                        point.venueName()
                                                + "\nYear: " + point.year()
                                                + "\nArticles: " + formatNumber(point.articlesPerYear())
                                                + "\nAvg authors/article: " + formatNumber(point.avgAuthorsPerArticle())
                                )
                        );
                    }
                }
            }
        };

        Runnable highlightedStyle = () -> {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node scatterNode = data.getNode();

                if (scatterNode != null) {
                    scatterNode.setStyle(getScatterStyle(color, true));
                    scatterNode.toFront();
                }
            }
        };

        normalStyle.run();

        chartHighlightHandles.add(
                new ChartHighlightHandle(
                        venueKey,
                        normalStyle,
                        highlightedStyle
                )
        );

        for (XYChart.Data<Number, Number> data : series.getData()) {
            setupChartHover(data.getNode(), venueKey);
        }

        applyActiveVenueHighlight();
    }

    private void setupChartHover(Node node, String venueKey) {
        if (node == null || venueKey == null || venueKey.isBlank()) {
            return;
        }

        node.setCursor(Cursor.HAND);
        node.setMouseTransparent(false);

        node.setOnMouseEntered(event -> {
            setActiveVenueHighlight(venueKey);
            event.consume();
        });

        node.setOnMouseExited(event -> {
            setActiveVenueHighlight(null);
            event.consume();
        });
    }

    private void setActiveVenueHighlight(String venueKey) {
        boolean sameVenue = activeVenueKey == null
                ? venueKey == null
                : activeVenueKey.equals(venueKey);

        if (sameVenue) {
            return;
        }

        activeVenueKey = venueKey;
        applyActiveVenueHighlight();
    }

    private void applyActiveVenueHighlight() {
        for (ChartHighlightHandle handle : chartHighlightHandles) {
            boolean highlighted = activeVenueKey != null && activeVenueKey.equals(handle.venueKey());

            if (highlighted) {
                handle.highlightedStyle().run();
            } else {
                handle.normalStyle().run();
            }
        }

        for (LegendHighlightHandle handle : legendHighlightHandles) {
            boolean highlighted = activeVenueKey != null && activeVenueKey.equals(handle.venueKey());
            applyLegendStyle(handle, highlighted);
        }
    }

    private void updateVenueLegend(List<VenueScatterSeries> allSeries) {
        legendHighlightHandles.clear();

        if (venueScatterLegendBox == null || venueScatterLegendFlow == null) {
            return;
        }

        venueScatterLegendFlow.getChildren().clear();

        if (allSeries == null || allSeries.isEmpty()) {
            venueScatterLegendBox.setVisible(false);
            venueScatterLegendBox.setManaged(false);
            return;
        }

        int index = 0;

        for (VenueScatterSeries venueSeries : allSeries) {
            String color = getChartColor(index);
            String venueKey = getVenueKey(venueSeries);

            Region colorDot = new Region();
            colorDot.setMinSize(10, 10);
            colorDot.setPrefSize(10, 10);
            colorDot.setMaxSize(10, 10);
            colorDot.setStyle(getLegendDotStyle(color, false));

            Label nameLabel = new Label(shortenName(venueSeries.name(), 45));
            nameLabel.setStyle(getLegendLabelStyle(false));

            Tooltip.install(nameLabel, new Tooltip(venueSeries.name()));

            HBox legendItem = new HBox(6, colorDot, nameLabel);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            legendItem.setCursor(Cursor.HAND);
            legendItem.setStyle(getLegendItemStyle(color, false));

            legendItem.setOnMouseEntered(event -> setActiveVenueHighlight(venueKey));
            legendItem.setOnMouseExited(event -> setActiveVenueHighlight(null));

            LegendHighlightHandle handle = new LegendHighlightHandle(
                    venueKey,
                    legendItem,
                    colorDot,
                    nameLabel,
                    color
            );

            legendHighlightHandles.add(handle);
            venueScatterLegendFlow.getChildren().add(legendItem);

            index++;
        }

        venueScatterLegendBox.setVisible(true);
        venueScatterLegendBox.setManaged(true);

        applyActiveVenueHighlight();
    }

    private void applyLegendStyle(LegendHighlightHandle handle, boolean highlighted) {
        if (handle == null) {
            return;
        }

        if (handle.legendItem() != null) {
            handle.legendItem().setStyle(getLegendItemStyle(handle.color(), highlighted));
        }

        if (handle.colorDot() != null) {
            double dotSize = highlighted ? 13 : 10;

            handle.colorDot().setMinSize(dotSize, dotSize);
            handle.colorDot().setPrefSize(dotSize, dotSize);
            handle.colorDot().setMaxSize(dotSize, dotSize);
            handle.colorDot().setStyle(getLegendDotStyle(handle.color(), highlighted));
        }

        if (handle.nameLabel() != null) {
            handle.nameLabel().setStyle(getLegendLabelStyle(highlighted));
        }
    }

    private String getScatterStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ", white;" +
                    "-fx-background-insets: 0, 3;" +
                    "-fx-background-radius: 11px;" +
                    "-fx-padding: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0.25, 0, 0);";
        }

        return "-fx-background-color: " + color + ", white;" +
                "-fx-background-insets: 0, 2;" +
                "-fx-background-radius: 8px;" +
                "-fx-padding: 5px;";
    }

    private String getLegendItemStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: rgba(255,255,255,1.0);" +
                    "-fx-border-color: " + color + ";" +
                    "-fx-border-width: 2px;" +
                    "-fx-border-radius: 999px;" +
                    "-fx-background-radius: 999px;" +
                    "-fx-padding: 5 9 5 9;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 6, 0.20, 0, 0);";
        }

        return "-fx-background-color: rgba(255,255,255,0.95);" +
                "-fx-border-color: #b9daf7;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 999px;" +
                "-fx-background-radius: 999px;" +
                "-fx-padding: 5 9 5 9;";
    }

    private String getLegendDotStyle(String color, boolean highlighted) {
        if (highlighted) {
            return "-fx-background-color: " + color + ";" +
                    "-fx-background-radius: 999px;" +
                    "-fx-border-color: #17212b;" +
                    "-fx-border-radius: 999px;" +
                    "-fx-border-width: 1.5px;";
        }

        return "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 999px;" +
                "-fx-border-width: 0;";
    }

    private String getLegendLabelStyle(boolean highlighted) {
        if (highlighted) {
            return "-fx-text-fill: #17212b;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: 900;";
        }

        return "-fx-text-fill: #0f4c81;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 700;";
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

    private YearRange getSelectedYearRange() {
        Integer startYear = scatterFromYearComboBox == null ? null : scatterFromYearComboBox.getValue();
        Integer endYear = scatterToYearComboBox == null ? null : scatterToYearComboBox.getValue();

        if (startYear != null && endYear != null && endYear < startYear) {
            throw new IllegalArgumentException("Το To year πρέπει να είναι μεγαλύτερο ή ίσο από το From year.");
        }

        return new YearRange(startYear, endYear);
    }

    private String getSelectedVenueType() {
        if (scatterVenueTypeComboBox == null) {
            return null;
        }

        return scatterVenueTypeComboBox.getValue();
    }

    private boolean alreadySelected(SelectedVenue newSelectedVenue) {
        if (scatterSelectedVenuesListView == null) {
            return false;
        }

        for (SelectedVenue existing : scatterSelectedVenuesListView.getItems()) {
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
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            if (METRIC_ARTICLES.equals(metric)) {
                return conferenceStat.totalArticles();
            }

            if (METRIC_AUTHOR_ENTRIES.equals(metric)) {
                return conferenceStat.totalAuthorOccurrences();
            }
        }

        return null;
    }

    private String getVenueKey(VenueScatterSeries venueSeries) {
        if (venueSeries == null) {
            return "";
        }

        return venueSeries.type() + "#" + venueSeries.venueId();
    }

    private String shortenName(String name, int maxLength) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        return name.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.000001) {
            return String.valueOf(Math.round(value));
        }

        return String.format(Locale.US, "%.2f", value);
    }

    private String readStringValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        return String.valueOf(value).trim();
    }

    private Double readDoubleValue(Object row, List<String> names) {
        Object value = readValue(row, names);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object readValue(Object row, List<String> names) {
        for (String name : names) {
            Object value = tryReadMethod(row, name);

            if (value != null) {
                return value;
            }

            value = tryReadMethod(row, "get" + capitalize(name));

            if (value != null) {
                return value;
            }

            value = tryReadField(row, name);

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private Object tryReadMethod(Object row, String methodName) {
        try {
            Method method = row.getClass().getMethod(methodName);
            return method.invoke(row);
        } catch (Exception exception) {
            return null;
        }
    }

    private Object tryReadField(Object row, String fieldName) {
        try {
            Field field = row.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(row);
        } catch (Exception exception) {
            return null;
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void setVenueLoading(boolean loading) {
        if (scatterVenueTypeComboBox != null) {
            scatterVenueTypeComboBox.setDisable(loading);
        }

        if (scatterVenueSearchField != null) {
            scatterVenueSearchField.setDisable(loading);
        }

        if (scatterAddSelectedButton != null) {
            scatterAddSelectedButton.setDisable(loading);
        }

        if (scatterRemoveSelectedButton != null) {
            scatterRemoveSelectedButton.setDisable(loading);
        }

        if (scatterClearSelectedButton != null) {
            scatterClearSelectedButton.setDisable(loading);
        }

        if (scatterLoadVenueButton != null) {
            scatterLoadVenueButton.setDisable(loading);
        }

        if (scatterFromYearComboBox != null) {
            scatterFromYearComboBox.setDisable(loading);
        }

        if (scatterToYearComboBox != null) {
            scatterToYearComboBox.setDisable(loading);
        }

        if (scatterSearchResultsListView != null) {
            scatterSearchResultsListView.setDisable(loading);
        }

        if (scatterSelectedVenuesListView != null) {
            scatterSelectedVenuesListView.setDisable(loading);
        }

        if (venueScatterChart != null) {
            venueScatterChart.setDisable(loading);
        }
    }

    private void setRankingLoading(boolean loading) {
        if (rankingXMetricComboBox != null) {
            rankingXMetricComboBox.setDisable(loading);
        }

        if (rankingYMetricComboBox != null) {
            rankingYMetricComboBox.setDisable(loading);
        }

        if (rankingLimitComboBox != null) {
            rankingLimitComboBox.setDisable(loading);
        }

        if (rankingJournalFilterField != null) {
            rankingJournalFilterField.setDisable(loading);
        }

        if (loadRankingScatterButton != null) {
            loadRankingScatterButton.setDisable(loading);
        }

        if (clearRankingScatterButton != null) {
            clearRankingScatterButton.setDisable(loading);
        }

        if (journalRankingScatterChart != null) {
            journalRankingScatterChart.setDisable(loading);
        }
    }

    private void setVenueStatus(String message) {
        if (venueScatterStatusLabel != null) {
            venueScatterStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void setRankingStatus(String message) {
        if (rankingStatusLabel != null) {
            rankingStatusLabel.setText(message == null ? "" : message);
        }
    }

    private void runAfterChartRender(Runnable action) {
        Platform.runLater(() -> Platform.runLater(action));
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

    private record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }

    private record VenueScatterSeries(
            String name,
            String type,
            int venueId,
            List<Object> yearlyStats
    ) {
    }

    private record VenueScatterPoint(
            String venueName,
            String venueType,
            int venueId,
            int year,
            double articlesPerYear,
            double avgAuthorsPerArticle
    ) {
    }

    private record RankingScatterPoint(
            String journalName,
            String xMetric,
            String yMetric,
            double xValue,
            double yValue
    ) {
    }

    private record ChartHighlightHandle(
            String venueKey,
            Runnable normalStyle,
            Runnable highlightedStyle
    ) {
    }

    private record LegendHighlightHandle(
            String venueKey,
            HBox legendItem,
            Region colorDot,
            Label nameLabel,
            String color
    ) {
    }
}