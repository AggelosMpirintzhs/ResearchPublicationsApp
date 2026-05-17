package controllers;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceSearchResultDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalSearchResultDto;
import dto.journal.JournalYearlyStatsDto;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import java.util.Optional;

public class VenueController {

    private static final String HOME_FXML_PATH = "/com/example/project_pvasil/hello-view.fxml";

    private static final String TYPE_JOURNAL = "Journal";
    private static final String TYPE_CONFERENCE = "Conference";

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;

    private static final int ARTICLE_BATCH_SIZE = 1000;

    private final JournalService journalService = new JournalService();
    private final ConferenceService conferenceService = new ConferenceService();

    private final ObservableList<Object> articleItems =
            FXCollections.observableArrayList();

    private Object selectedVenue;

    private volatile boolean stopArticleLoading = false;

    private Task<Void> articleLoadingTask;

    private long expectedArticleCount = 0;

    @FXML
    private ComboBox<String> venueTypeComboBox;

    @FXML
    private TextField venueSearchField;

    @FXML
    private Button searchVenueButton;

    @FXML
    private Button clearButton;

    @FXML
    private ListView<Object> venueResultsListView;

    @FXML
    private Label selectedVenueLabel;

    @FXML
    private ComboBox<Integer> fromYearComboBox;

    @FXML
    private ComboBox<Integer> toYearComboBox;

    @FXML
    private CheckBox loadArticlesCheckBox;

    @FXML
    private Button loadProfileButton;

    @FXML
    private Button backButton;

    @FXML
    private Label rankingLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label rankingMetricsLabel;

    @FXML
    private Label firstYearLabel;

    @FXML
    private Label lastYearLabel;

    @FXML
    private Label totalArticlesLabel;

    @FXML
    private Label totalAuthorsLabel;

    @FXML
    private Label distinctAuthorsLabel;

    @FXML
    private Label avgAuthorsArticleLabel;

    @FXML
    private Label avgArticlesYearLabel;

    @FXML
    private Label avgAuthorsYearLabel;

    @FXML
    private Label articlesLoadedLabel;

    @FXML
    private LineChart<Number, Number> articlesLineChart;

    @FXML
    private NumberAxis articlesYearAxis;

    @FXML
    private NumberAxis articlesCountAxis;

    @FXML
    private LineChart<Number, Number> authorEntriesLineChart;

    @FXML
    private NumberAxis authorEntriesYearAxis;

    @FXML
    private NumberAxis authorEntriesCountAxis;

    @FXML
    private LineChart<Number, Number> distinctAuthorsLineChart;

    @FXML
    private NumberAxis distinctAuthorsYearAxis;

    @FXML
    private NumberAxis distinctAuthorsCountAxis;

    @FXML
    private VBox articleReportPanel;

    @FXML
    private TableView<Object> articlesTable;

    @FXML
    private TableColumn<Object, Integer> yearColumn;

    @FXML
    private TableColumn<Object, String> titleColumn;

    @FXML
    private TableColumn<Object, String> authorsColumn;

    @FXML
    private TableColumn<Object, String> pagesColumn;

    @FXML
    private TableColumn<Object, String> urlColumn;

    @FXML
    private VBox venueResultsContainer;
    

    @FXML
    public void initialize() {
        setupVenueTypeComboBox();
        setupYearComboBoxes();
        setupSearchField();
        setupVenueResultsListView();
        setupArticlesTable();
        setupLoadArticlesOption();
        setupYearlyCharts();

        clearVenueResults();
        setSelectedVenue(null);
        clearResultArea();
        setArticleReportVisible(false);
    }

    private void setVenueResultsVisible(boolean visible) {
        if (venueResultsContainer != null) {
            venueResultsContainer.setVisible(visible);
            venueResultsContainer.setManaged(visible);
        }
    }

    @FXML
    private void searchVenues() {
        stopCurrentArticleLoading();

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

            clearResultArea();
            setSelectedVenue(null);
            renderVenueResults(results);

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

        startBackgroundTask(task, "venue-search-task");
    }

    @FXML
    private void loadVenueProfile() {
        stopCurrentArticleLoading();

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            showError("Δεν επιλέχθηκε τύπος", "Πρέπει πρώτα να επιλέξεις Journal ή Conference.");
            return;
        }

        if (selectedVenue == null) {
            showError(
                    "Δεν επιλέχθηκε venue",
                    "Πρέπει πρώτα να κάνεις αναζήτηση και να επιλέξεις ένα αποτέλεσμα από τη λίστα."
            );
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Λάθος χρονιές", exception.getMessage());
            return;
        }

        boolean shouldLoadArticles =
                loadArticlesCheckBox != null && loadArticlesCheckBox.isSelected();

        Task<VenuePageData> task = new Task<>() {
            @Override
            protected VenuePageData call() {
                if (TYPE_JOURNAL.equals(venueType)) {
                    int journalId = getJournalId(selectedVenue);

                    Optional<JournalProfileDto> profile =
                            journalService.getJournalProfile(
                                    journalId,
                                    yearRange.startYear(),
                                    yearRange.endYear()
                            );

                    Optional<JournalRankingDto> ranking =
                            journalService.getJournalRanking(journalId);

                    List<JournalYearlyStatsDto> yearlyStats =
                            journalService.getJournalYearlyStats(
                                    journalId,
                                    yearRange.startYear(),
                                    yearRange.endYear()
                            );

                    List<Object> yearlyStatsAsObjects = new ArrayList<>();
                    yearlyStatsAsObjects.addAll(yearlyStats);

                    return new VenuePageData(
                            TYPE_JOURNAL,
                            profile.orElse(null),
                            ranking.orElse(null),
                            yearlyStatsAsObjects
                    );
                }

                if (TYPE_CONFERENCE.equals(venueType)) {
                    int conferenceId = getConferenceId(selectedVenue);

                    Optional<ConferenceProfileDto> profile =
                            conferenceService.getConferenceProfile(
                                    conferenceId,
                                    yearRange.startYear(),
                                    yearRange.endYear()
                            );

                    Optional<ConferenceRankingDto> ranking =
                            conferenceService.getConferenceRanking(conferenceId);

                    List<ConferenceYearlyStatsDto> yearlyStats =
                            conferenceService.getConferenceYearlyStats(
                                    conferenceId,
                                    yearRange.startYear(),
                                    yearRange.endYear()
                            );

                    List<Object> yearlyStatsAsObjects = new ArrayList<>();
                    yearlyStatsAsObjects.addAll(yearlyStats);

                    return new VenuePageData(
                            TYPE_CONFERENCE,
                            profile.orElse(null),
                            ranking.orElse(null),
                            yearlyStatsAsObjects
                    );
                }

                throw new IllegalArgumentException("Μη έγκυρος τύπος venue.");
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            VenuePageData data = task.getValue();

            if (data.profile() == null) {
                clearResultArea();
                showInfo(
                        "Δεν βρέθηκαν δεδομένα",
                        "Δεν υπάρχουν δεδομένα για το επιλεγμένο venue στο συγκεκριμένο εύρος χρονιών."
                );
                return;
            }

            updateProfileLabels(data.profile());
            updateRankingPanel(data.ranking());
            updateYearlyLineCharts(data.yearlyStats());

            expectedArticleCount = getExpectedArticleCount(data.profile());

            if (shouldLoadArticles) {
                articleItems.clear();
                setArticleReportVisible(true);
                updateArticlesLoadedLabel(0, expectedArticleCount);

                int venueId = TYPE_JOURNAL.equals(venueType)
                        ? getJournalId(selectedVenue)
                        : getConferenceId(selectedVenue);

                startArticleBatchLoading(
                        venueType,
                        venueId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            } else {
                articleItems.clear();
                expectedArticleCount = 0;
                updateArticlesLoadedLabel(0, 0);
                setArticleReportVisible(false);
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα φόρτωσης", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "venue-profile-task");
    }

    private void startArticleBatchLoading(
            String venueType,
            int venueId,
            Integer startYear,
            Integer endYear
    ) {
        stopCurrentArticleLoading();

        articleItems.clear();
        stopArticleLoading = false;

        updateArticlesLoadedLabel(0, expectedArticleCount);

        articleLoadingTask = new Task<>() {
            @Override
            protected Void call() {
                int lastArticleId = 0;

                while (!stopArticleLoading && !isCancelled()) {
                    List<Object> batch = new ArrayList<>();

                    if (TYPE_JOURNAL.equals(venueType)) {
                        batch.addAll(
                                journalService.getJournalArticlesBatch(
                                        venueId,
                                        startYear,
                                        endYear,
                                        lastArticleId,
                                        ARTICLE_BATCH_SIZE
                                )
                        );
                    } else if (TYPE_CONFERENCE.equals(venueType)) {
                        batch.addAll(
                                conferenceService.getConferenceArticlesBatch(
                                        venueId,
                                        startYear,
                                        endYear,
                                        lastArticleId,
                                        ARTICLE_BATCH_SIZE
                                )
                        );
                    }

                    if (batch.isEmpty()) {
                        break;
                    }

                    Integer newLastArticleId = extractArticleId(batch.get(batch.size() - 1));

                    if (newLastArticleId == null) {
                        break;
                    }

                    lastArticleId = newLastArticleId;

                    Platform.runLater(() -> {
                        articleItems.addAll(batch);
                        updateArticlesLoadedLabel(articleItems.size(), expectedArticleCount);
                    });
                }

                Platform.runLater(() ->
                        updateArticlesLoadedLabel(articleItems.size(), expectedArticleCount)
                );

                return null;
            }
        };

        articleLoadingTask.setOnFailed(event -> {
            Throwable exception = articleLoadingTask.getException();
            showError(
                    "Σφάλμα φόρτωσης άρθρων",
                    exception == null ? null : exception.getMessage()
            );
        });

        startBackgroundTask(articleLoadingTask, "venue-article-batch-loading-task");
    }

    private void stopCurrentArticleLoading() {
        stopArticleLoading = true;

        if (articleLoadingTask != null && articleLoadingTask.isRunning()) {
            articleLoadingTask.cancel();
        }
    }

    @FXML
    private void clear() {
        stopCurrentArticleLoading();

        if (venueSearchField != null) {
            venueSearchField.clear();
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.getSelectionModel().clearSelection();
        }

        if (toYearComboBox != null) {
            toYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        if (loadArticlesCheckBox != null) {
            loadArticlesCheckBox.setSelected(false);
        }

        clearVenueResults();
        setSelectedVenue(null);
        clearResultArea();
    }

    @FXML
    private void backToHome() {
        stopCurrentArticleLoading();

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
            stopCurrentArticleLoading();
            clearVenueResults();
            setSelectedVenue(null);
            clearResultArea();
        });
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

    private void setupVenueResultsListView() {
        if (venueResultsListView == null) {
            return;
        }

        venueResultsListView.setPlaceholder(new Label("Search results will appear here."));

        venueResultsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Object venue, boolean empty) {
                super.updateItem(venue, empty);

                if (empty || venue == null) {
                    setText(null);
                } else {
                    setText(getVenueDisplayName(venue));
                }
            }
        });

        venueResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        stopCurrentArticleLoading();
                        setSelectedVenue(selected);
                        clearResultArea();
                    }
                }
        );
    }

    private void setupLoadArticlesOption() {
        setArticleReportVisible(false);

        if (loadArticlesCheckBox != null) {
            loadArticlesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    stopCurrentArticleLoading();
                    articleItems.clear();
                    expectedArticleCount = 0;
                    updateArticlesLoadedLabel(0, 0);
                    setArticleReportVisible(false);
                }
            });
        }
    }

    private void setArticleReportVisible(boolean visible) {
        if (articleReportPanel != null) {
            articleReportPanel.setVisible(visible);
            articleReportPanel.setManaged(visible);
        }
    }

    private void setupYearlyCharts() {
        setupLineChart(articlesLineChart, articlesYearAxis, articlesCountAxis);
        setupLineChart(authorEntriesLineChart, authorEntriesYearAxis, authorEntriesCountAxis);
        setupLineChart(distinctAuthorsLineChart, distinctAuthorsYearAxis, distinctAuthorsCountAxis);
    }

    private void setupLineChart(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis
    ) {
        if (chart != null) {
            chart.setAnimated(false);
            chart.setCreateSymbols(false);
            chart.setLegendVisible(false);
        }

        if (xAxis != null) {
            xAxis.setAutoRanging(false);
            xAxis.setForceZeroInRange(false);
            xAxis.setTickLabelRotation(0);
        }

        if (yAxis != null) {
            yAxis.setAutoRanging(false);
            yAxis.setForceZeroInRange(true);
        }
    }

    private void setupArticlesTable() {
        if (articlesTable != null) {
            articlesTable.setPrefHeight(340);
            articlesTable.setMinHeight(340);
            articlesTable.setMaxHeight(340);
            articlesTable.setFixedCellSize(34);
            articlesTable.setItems(articleItems);
        }

        if (yearColumn != null) {
            yearColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(extractArticleYear(cellData.getValue()))
            );
        }

        if (titleColumn != null) {
            titleColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticleTitle(cellData.getValue())))
            );
        }

        if (authorsColumn != null) {
            authorsColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticleAuthors(cellData.getValue())))
            );
        }

        if (pagesColumn != null) {
            pagesColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticlePages(cellData.getValue())))
            );
        }

        if (urlColumn != null) {
            urlColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticleUrlOrEe(cellData.getValue())))
            );
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
        String displayName = normalizeSearchText(getVenueDisplayName(venue));

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

    private void renderVenueResults(List<Object> results) {
        if (venueResultsListView == null) {
            return;
        }

        venueResultsListView.getItems().clear();
        venueResultsListView.getSelectionModel().clearSelection();

        setVenueResultsVisible(true);

        if (results == null || results.isEmpty()) {
            venueResultsListView.setPlaceholder(new Label("No matching results"));
            return;
        }

        venueResultsListView.setPlaceholder(new Label("No matching results"));
        venueResultsListView.getItems().setAll(results);
    }

    private void clearVenueResults() {
        if (venueResultsListView == null) {
            return;
        }

        venueResultsListView.getItems().clear();
        venueResultsListView.getSelectionModel().clearSelection();
        venueResultsListView.setPlaceholder(new Label("Search results will appear here."));

        setVenueResultsVisible(false);
    }

    private void setSelectedVenue(Object venue) {
        selectedVenue = venue;

        if (selectedVenueLabel == null) {
            return;
        }

        if (venue == null) {
            selectedVenueLabel.setText("No venue selected");
        } else {
            selectedVenueLabel.setText(getVenueDisplayName(venue));
        }
    }

    private void updateProfileLabels(Object profile) {
        if (profile instanceof JournalProfileDto journalProfile) {
            setLabelText(firstYearLabel, journalProfile.firstYear());
            setLabelText(lastYearLabel, journalProfile.lastYear());

            setLabelText(totalArticlesLabel, journalProfile.totalArticles());
            setLabelText(totalAuthorsLabel, journalProfile.totalAuthorOccurrences());
            setLabelText(distinctAuthorsLabel, journalProfile.distinctAuthorsAllTime());

            setLabelText(avgAuthorsArticleLabel, formatDouble(journalProfile.avgAuthorsPerArticle()));
            setLabelText(avgArticlesYearLabel, formatDouble(journalProfile.avgArticlesPerYear()));
            setLabelText(avgAuthorsYearLabel, formatDouble(journalProfile.avgAuthorOccurrencesPerYear()));
            return;
        }

        if (profile instanceof ConferenceProfileDto conferenceProfile) {
            setLabelText(firstYearLabel, conferenceProfile.firstYear());
            setLabelText(lastYearLabel, conferenceProfile.lastYear());

            setLabelText(totalArticlesLabel, conferenceProfile.totalArticles());
            setLabelText(totalAuthorsLabel, conferenceProfile.totalAuthorOccurrences());
            setLabelText(distinctAuthorsLabel, conferenceProfile.distinctAuthors());

            setLabelText(avgAuthorsArticleLabel, formatDouble(conferenceProfile.avgAuthorsPerArticle()));
            setLabelText(avgArticlesYearLabel, formatDouble(conferenceProfile.avgArticlesPerYear()));
            setLabelText(avgAuthorsYearLabel, formatDouble(conferenceProfile.avgAuthorOccurrencesPerYear()));
            return;
        }

        clearProfileLabels();
    }

    private void updateRankingPanel(Object ranking) {
        if (ranking == null) {
            setLabelText(rankingLabel, "-");
            setLabelText(categoryLabel, "-");
            setLabelText(rankingMetricsLabel, "-");
            return;
        }

        if (ranking instanceof JournalRankingDto journalRanking) {
            setLabelText(rankingLabel, buildJournalRankText(journalRanking));
            setLabelText(categoryLabel, journalRanking.bestSubjectArea());

            String metrics = "SJR: " + nullToDash(formatDouble(journalRanking.sjrIndex()))
                    + "\nCiteScore: " + nullToDash(formatDouble(journalRanking.citeScore()))
                    + "\nH-index: " + nullToDash(journalRanking.hIndex())
                    + "\nTotal docs: " + nullToDash(journalRanking.totalDocs())
                    + "\nTotal refs: " + nullToDash(journalRanking.totalRefs())
                    + "\nCites/doc 2y: " + nullToDash(formatDouble(journalRanking.citesPerDoc2y()))
                    + "\nRefs/doc: " + nullToDash(formatDouble(journalRanking.refsPerDoc()));

            setLabelText(rankingMetricsLabel, metrics);
            return;
        }

        if (ranking instanceof ConferenceRankingDto conferenceRanking) {
            setLabelText(rankingLabel, conferenceRanking.rankLabel());

            if (conferenceRanking.primaryFoRName() != null && !conferenceRanking.primaryFoRName().isBlank()) {
                setLabelText(categoryLabel, conferenceRanking.primaryFoRName());
            } else {
                setLabelText(categoryLabel, "-");
            }

            String metrics = "ICORE ID: " + nullToDash(conferenceRanking.icoreId());

            setLabelText(rankingMetricsLabel, metrics);
        }
    }

    private void updateYearlyLineCharts(List<Object> yearlyStats) {
        clearLineCharts();

        if (yearlyStats == null || yearlyStats.isEmpty()) {
            resetLineChartAxes(articlesYearAxis, articlesCountAxis);
            resetLineChartAxes(authorEntriesYearAxis, authorEntriesCountAxis);
            resetLineChartAxes(distinctAuthorsYearAxis, distinctAuthorsCountAxis);
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;

        long maxArticles = 0;
        long maxAuthorEntries = 0;
        long maxDistinctAuthors = 0;

        XYChart.Series<Number, Number> articlesSeries = new XYChart.Series<>();
        articlesSeries.setName("Articles");

        XYChart.Series<Number, Number> authorEntriesSeries = new XYChart.Series<>();
        authorEntriesSeries.setName("Author entries");

        XYChart.Series<Number, Number> distinctAuthorsSeries = new XYChart.Series<>();
        distinctAuthorsSeries.setName("Distinct authors");

        for (Object stat : yearlyStats) {
            Integer year = extractYear(stat);

            if (year == null) {
                continue;
            }

            long totalArticles = defaultLong(extractTotalArticles(stat));
            long totalAuthorOccurrences = defaultLong(extractTotalAuthorOccurrences(stat));
            long distinctAuthors = defaultLong(extractDistinctAuthors(stat));

            minYear = minYear == null ? year : Math.min(minYear, year);
            maxYear = maxYear == null ? year : Math.max(maxYear, year);

            maxArticles = Math.max(maxArticles, totalArticles);
            maxAuthorEntries = Math.max(maxAuthorEntries, totalAuthorOccurrences);
            maxDistinctAuthors = Math.max(maxDistinctAuthors, distinctAuthors);

            articlesSeries.getData().add(new XYChart.Data<>(year, totalArticles));
            authorEntriesSeries.getData().add(new XYChart.Data<>(year, totalAuthorOccurrences));
            distinctAuthorsSeries.getData().add(new XYChart.Data<>(year, distinctAuthors));
        }

        if (articlesLineChart != null) {
            articlesLineChart.getData().add(articlesSeries);
        }

        if (authorEntriesLineChart != null) {
            authorEntriesLineChart.getData().add(authorEntriesSeries);
        }

        if (distinctAuthorsLineChart != null) {
            distinctAuthorsLineChart.getData().add(distinctAuthorsSeries);
        }

        configureYearAxis(articlesYearAxis, minYear, maxYear);
        configureYearAxis(authorEntriesYearAxis, minYear, maxYear);
        configureYearAxis(distinctAuthorsYearAxis, minYear, maxYear);

        configureValueAxis(articlesCountAxis, maxArticles);
        configureValueAxis(authorEntriesCountAxis, maxAuthorEntries);
        configureValueAxis(distinctAuthorsCountAxis, maxDistinctAuthors);
    }

    private void clearLineCharts() {
        if (articlesLineChart != null) {
            articlesLineChart.getData().clear();
        }

        if (authorEntriesLineChart != null) {
            authorEntriesLineChart.getData().clear();
        }

        if (distinctAuthorsLineChart != null) {
            distinctAuthorsLineChart.getData().clear();
        }
    }

    private void resetLineChartAxes(NumberAxis xAxis, NumberAxis yAxis) {
        configureYearAxis(xAxis, MIN_YEAR, LocalDate.now().getYear());
        configureValueAxis(yAxis, 10);
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

    private void updateArticlesTable(List<Object> articles) {
        articleItems.setAll(articles);
        updateArticlesLoadedLabel(articleItems.size(), expectedArticleCount);
    }

    private void clearResultArea() {
        setLabelText(rankingLabel, "-");
        setLabelText(categoryLabel, "-");
        setLabelText(rankingMetricsLabel, "-");

        clearProfileLabels();
        clearLineCharts();

        articleItems.clear();
        expectedArticleCount = 0;
        updateArticlesLoadedLabel(0, 0);

        setArticleReportVisible(false);
    }

    private void clearProfileLabels() {
        setLabelText(firstYearLabel, "-");
        setLabelText(lastYearLabel, "-");

        setLabelText(totalArticlesLabel, "-");
        setLabelText(totalAuthorsLabel, "-");
        setLabelText(distinctAuthorsLabel, "-");

        setLabelText(avgAuthorsArticleLabel, "-");
        setLabelText(avgArticlesYearLabel, "-");
        setLabelText(avgAuthorsYearLabel, "-");
    }

    private String getSelectedVenueType() {
        if (venueTypeComboBox == null) {
            return null;
        }

        return venueTypeComboBox.getValue();
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

    private int getJournalId(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalId() != null && journal.journalId() > 0) {
                return journal.journalId();
            }
        }

        throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο journalId για το επιλεγμένο journal.");
    }

    private int getConferenceId(Object venue) {
        if (venue instanceof ConferenceSearchResultDto conference) {
            if (conference.conferenceId() != null && conference.conferenceId() > 0) {
                return conference.conferenceId();
            }
        }

        throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο conferenceId για το επιλεγμένο conference.");
    }

    private String getVenueDisplayName(Object venue) {
        if (venue instanceof JournalSearchResultDto journal) {
            if (journal.journalName() != null && !journal.journalName().isBlank()) {
                if (journal.publisherName() != null && !journal.publisherName().isBlank()) {
                    return journal.journalName() + " (" + journal.publisherName() + ")";
                }

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

    private String buildJournalRankText(JournalRankingDto ranking) {
        StringBuilder builder = new StringBuilder();

        if (ranking.rankingPosition() != null) {
            builder.append("Rank ").append(ranking.rankingPosition());
        }

        if (ranking.bestQuartile() != null && !ranking.bestQuartile().isBlank()) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }

            builder.append(ranking.bestQuartile());
        }

        return builder.length() == 0 ? "-" : builder.toString();
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

    private Long extractTotalArticles(Object stat) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            return journalStat.totalArticles();
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            return conferenceStat.totalArticles();
        }

        return null;
    }

    private Long extractTotalAuthorOccurrences(Object stat) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            return journalStat.totalAuthorOccurrences();
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            return conferenceStat.totalAuthorOccurrences();
        }

        return null;
    }

    private Long extractDistinctAuthors(Object stat) {
        if (stat instanceof JournalYearlyStatsDto journalStat) {
            return journalStat.distinctAuthors();
        }

        if (stat instanceof ConferenceYearlyStatsDto conferenceStat) {
            return conferenceStat.distinctAuthors();
        }

        return null;
    }

    private Integer extractArticleId(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.articleId();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.articleId();
        }

        return null;
    }

    private Integer extractArticleYear(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.year();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.year();
        }

        return null;
    }

    private String extractArticleTitle(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.title();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.title();
        }

        return null;
    }

    private String extractArticleAuthors(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.authors();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.authors();
        }

        return null;
    }

    private String extractArticlePages(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return journalArticle.pages();
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return conferenceArticle.pages();
        }

        return null;
    }

    private String extractArticleUrlOrEe(Object article) {
        if (article instanceof JournalArticleDto journalArticle) {
            return firstNonBlank(journalArticle.url(), journalArticle.ee());
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return firstNonBlank(conferenceArticle.url(), conferenceArticle.ee());
        }

        return null;
    }

    private long getExpectedArticleCount(Object profile) {
        if (profile instanceof JournalProfileDto journalProfile) {
            return defaultLong(journalProfile.totalArticles());
        }

        if (profile instanceof ConferenceProfileDto conferenceProfile) {
            return defaultLong(conferenceProfile.totalArticles());
        }

        return 0;
    }

    private void updateArticlesLoadedLabel(long loaded, long total) {
        if (articlesLoadedLabel != null) {
            articlesLoadedLabel.setText("Articles Loaded: " + loaded + " / " + total);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private void setLabelText(Label label, Object value) {
        if (label != null) {
            label.setText(value == null ? "-" : String.valueOf(value));
        }
    }

    private String nullToDash(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value);

        if (text.isBlank()) {
            return "-";
        }

        return text;
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "-";
        }

        return String.format("%.2f", value);
    }

    private void setLoading(boolean loading) {
        if (venueTypeComboBox != null) {
            venueTypeComboBox.setDisable(loading);
        }

        if (venueSearchField != null) {
            venueSearchField.setDisable(loading);
        }

        if (searchVenueButton != null) {
            searchVenueButton.setDisable(loading);
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.setDisable(loading);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(loading);
        }

        if (loadArticlesCheckBox != null) {
            loadArticlesCheckBox.setDisable(loading);
        }

        if (loadProfileButton != null) {
            loadProfileButton.setDisable(loading);
        }

        if (clearButton != null) {
            clearButton.setDisable(loading);
        }

        if (venueResultsListView != null) {
            venueResultsListView.setDisable(loading);
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

    private record YearRange(
            Integer startYear,
            Integer endYear
    ) {
    }

    private record VenuePageData(
            String venueType,
            Object profile,
            Object ranking,
            List<Object> yearlyStats
    ) {
    }
}