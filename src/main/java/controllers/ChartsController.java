package controllers;

import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;
    private static final int MAX_SELECTED_VENUES = 10;

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    private Object selectedSearchVenue;

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
    private LineChart<Number, Number> comparisonLineChart;

    @FXML
    private NumberAxis yearAxis;

    @FXML
    private NumberAxis valueAxis;

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupMetricComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupSearchResultsListView();
        setupSelectedVenuesListView();
        setupChart();

        selectedSearchVenue = null;
        clearSearchResults();
        clearChart();
    }

    @FXML
    private void searchVenues() {
        String venueType = getSelectedVenueType();
        String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

        if (venueType == null || venueType.isBlank()) {
            showError("Δεν επιλέχθηκε τύπος", "Πρέπει πρώτα να επιλέξεις Journal ή Conference.");
            return;
        }

        if (searchText.isBlank()) {
            showError("Λάθος αναζήτηση", "Πρέπει να γράψεις όνομα journal ή conference.");
            return;
        }

        selectedSearchVenue = null;
        clearSearchResultsOnly();

        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                List<Object> results = new ArrayList<>();

                if (TYPE_JOURNAL.equals(venueType)) {
                    results.addAll(journalService.searchJournals(searchText, SEARCH_LIMIT));
                    return results;
                }

                if (TYPE_CONFERENCE.equals(venueType)) {
                    results.addAll(conferenceService.searchConferences(searchText, SEARCH_LIMIT));
                    return results;
                }

                return results;
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<Object> results = task.getValue();
            results = sortVenueResultsByRelevance(results, searchText);

            renderSearchResults(results);

            if (results.isEmpty()) {
                showInfo(
                        "Δεν βρέθηκαν αποτελέσματα",
                        "Δεν βρέθηκε journal/conference με αυτό το κείμενο αναζήτησης."
                );
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα αναζήτησης", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "charts-search-task");
    }

    @FXML
    private void addSelectedVenue() {
        if (selectedVenuesListView == null) {
            return;
        }

        if (selectedSearchVenue == null) {
            showError(
                    "Δεν επιλέχθηκε venue",
                    "Πρέπει πρώτα να επιλέξεις ένα αποτέλεσμα από τη λίστα Matching results."
            );
            return;
        }

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            showError("Δεν επιλέχθηκε τύπος", "Πρέπει πρώτα να επιλέξεις Journal ή Conference.");
            return;
        }

        if (selectedVenuesListView.getItems().size() >= MAX_SELECTED_VENUES) {
            showError(
                    "Πολλά venues",
                    "Για να παραμένει ευανάγνωστο το chart, μπορείς να επιλέξεις μέχρι "
                            + MAX_SELECTED_VENUES + " venues."
            );
            return;
        }

        SelectedVenue selectedVenue = new SelectedVenue(venueType, selectedSearchVenue);

        if (alreadySelected(selectedVenue)) {
            showInfo("Ήδη επιλεγμένο", "Το συγκεκριμένο venue υπάρχει ήδη στη λίστα.");
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
            showError("Δεν επιλέχθηκε metric", "Πρέπει να επιλέξεις τι θέλεις να εμφανίζει το chart.");
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

                        chartSeries.add(
                                new VenueChartSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        new ArrayList<>(stats)
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

                        chartSeries.add(
                                new VenueChartSeries(
                                        getVenueDisplayName(selectedVenue),
                                        selectedVenue.type(),
                                        new ArrayList<>(stats)
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
            updateLineChart(task.getValue(), metric);
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα φόρτωσης chart", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "charts-load-task");
    }

    @FXML
    private void clear() {
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
            selectedSearchVenue = null;
            clearSearchResults();
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
        if (venueSearchField != null) {
            venueSearchField.setOnAction(event -> searchVenues());
        }
    }

    private void setupSearchResultsListView() {
        if (searchResultsListView == null) {
            return;
        }

        searchResultsListView.setPlaceholder(new Label("Search results will appear here."));

        searchResultsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Object venue, boolean empty) {
                super.updateItem(venue, empty);

                if (empty || venue == null) {
                    setText(null);
                } else {
                    setText(getRawVenueDisplayName(venue));
                }
            }
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

        selectedVenuesListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SelectedVenue selectedVenue, boolean empty) {
                super.updateItem(selectedVenue, empty);

                if (empty || selectedVenue == null) {
                    setText(null);
                } else {
                    setText(getVenueDisplayName(selectedVenue));
                }
            }
        });
    }

    private void setupChart() {
        if (comparisonLineChart != null) {
            comparisonLineChart.setAnimated(false);
            comparisonLineChart.setCreateSymbols(false);
            comparisonLineChart.setLegendVisible(true);
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
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        long maxValue = 0;

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
            }
        }

        configureYearAxis(yearAxis, minYear, maxYear);
        configureValueAxis(valueAxis, maxValue);

        if (valueAxis != null) {
            valueAxis.setLabel(metric);
        }
    }

    private void clearChart() {
        if (comparisonLineChart != null) {
            comparisonLineChart.getData().clear();
        }

        configureYearAxis(yearAxis, MIN_YEAR, LocalDate.now().getYear());
        configureValueAxis(valueAxis, 10);
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

    private void configureValueAxis(NumberAxis axis, long maxValue) {
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

    private double calculateNiceUpperBound(long maxValue) {
        if (maxValue <= 0) {
            return 10;
        }

        double paddedValue = maxValue * 1.15;

        if (paddedValue <= 10) {
            return Math.ceil(paddedValue);
        }

        double magnitude = Math.pow(10, Math.floor(Math.log10(paddedValue)));
        double normalized = paddedValue / magnitude;

        double rounded;

        if (normalized <= 1) {
            rounded = 1;
        } else if (normalized <= 2) {
            rounded = 2;
        } else if (normalized <= 5) {
            rounded = 5;
        } else {
            rounded = 10;
        }

        return rounded * magnitude;
    }

    private double calculateNiceTickUnit(double upperBound) {
        if (upperBound <= 10) {
            return 1;
        }

        if (upperBound <= 20) {
            return 2;
        }

        if (upperBound <= 50) {
            return 5;
        }

        if (upperBound <= 100) {
            return 10;
        }

        if (upperBound <= 500) {
            return 50;
        }

        if (upperBound <= 1000) {
            return 100;
        }

        if (upperBound <= 5000) {
            return 500;
        }

        return Math.ceil(upperBound / 10.0);
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

    private record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }
}