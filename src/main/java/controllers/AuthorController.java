package controllers;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;
import service.AuthorService;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AuthorController {

    private static final String HOME_FXML_PATH = "/com/example/project_pvasil/hello-view.fxml";

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;
    private static final int PUBLICATION_BATCH_SIZE = 1000;

    private final AuthorService authorService = new AuthorService();

    private final ObservableList<AuthorSearchResultDto> authorSearchItems =
            FXCollections.observableArrayList();

    private final ObservableList<AuthorPublicationDto> publicationItems =
            FXCollections.observableArrayList();

    private AuthorSearchResultDto selectedAuthor;

    private PauseTransition searchDebounce;

    private Task<List<AuthorSearchResultDto>> authorSearchTask;

    private Task<Void> publicationLoadingTask;

    private volatile boolean stopPublicationLoading = false;

    private long expectedPublicationCount = 0;

    @FXML
    private TextField authorSearchField;

    @FXML
    private ListView<AuthorSearchResultDto> authorResultsListView;

    @FXML
    private VBox authorResultsContainer;

    @FXML
    private Label selectedAuthorLabel;

    @FXML
    private ComboBox<Integer> fromYearComboBox;

    @FXML
    private ComboBox<Integer> toYearComboBox;

    @FXML
    private CheckBox loadPublicationsCheckBox;

    @FXML
    private Button loadProfileButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button backButton;

    @FXML
    private Label firstYearLabel;

    @FXML
    private Label lastYearLabel;

    @FXML
    private Label activeYearsLabel;

    @FXML
    private Label totalArticlesLabel;

    @FXML
    private Label totalJournalArticlesLabel;

    @FXML
    private Label totalConferenceArticlesLabel;

    @FXML
    private Label distinctJournalsLabel;

    @FXML
    private Label distinctConferencesLabel;

    @FXML
    private Label avgArticlesPerYearLabel;

    @FXML
    private LineChart<Number, Number> totalArticlesLineChart;

    @FXML
    private NumberAxis totalArticlesYearAxis;

    @FXML
    private NumberAxis totalArticlesCountAxis;

    @FXML
    private LineChart<Number, Number> publicationTypeLineChart;

    @FXML
    private NumberAxis publicationTypeYearAxis;

    @FXML
    private NumberAxis publicationTypeCountAxis;

    @FXML
    private VBox publicationReportPanel;

    @FXML
    private Label publicationsLoadedLabel;

    @FXML
    private TableView<AuthorPublicationDto> publicationsTable;

    @FXML
    private TableColumn<AuthorPublicationDto, Integer> publicationYearColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationTypeColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationTitleColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationVenueColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationAuthorsColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationPagesColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationUrlColumn;

    @FXML
    public void initialize() {
        setupSearchField();
        setupAuthorResultsListView();
        setupYearComboBoxes();
        setupLoadPublicationsOption();
        setupCharts();
        setupPublicationsTable();

        clearAuthorResults();
        setSelectedAuthor(null);
        clearResultArea();
        setPublicationReportVisible(false);
    }

    private void setupSearchField() {
        searchDebounce = new PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MS));
        searchDebounce.setOnFinished(event -> searchAuthorsRealtime());

        if (authorSearchField == null) {
            return;
        }

        authorSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        authorSearchField.setOnAction(event -> searchAuthorsRealtime());
    }

    private void searchAuthorsRealtime() {
        String searchText = authorSearchField == null ? "" : authorSearchField.getText().trim();

        if (authorSearchTask != null && authorSearchTask.isRunning()) {
            authorSearchTask.cancel();
        }

        if (searchText.length() < MIN_SEARCH_LENGTH) {
            authorSearchItems.clear();
            setAuthorResultsVisible(false);
            return;
        }

        String requestedSearchText = searchText;

        authorSearchTask = new Task<>() {
            @Override
            protected List<AuthorSearchResultDto> call() {
                return authorService.searchAuthors(requestedSearchText, SEARCH_LIMIT);
            }
        };

        authorSearchTask.setOnSucceeded(event -> {
            String currentSearchText = authorSearchField == null ? "" : authorSearchField.getText().trim();

            if (!currentSearchText.equals(requestedSearchText)) {
                return;
            }

            List<AuthorSearchResultDto> results = authorSearchTask.getValue();
            results = sortAuthorsByRelevance(results, requestedSearchText);

            authorSearchItems.setAll(results);
            setAuthorResultsVisible(true);
        });

        authorSearchTask.setOnFailed(event -> {
            Throwable exception = authorSearchTask.getException();
            showError("Σφάλμα αναζήτησης συγγραφέων", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(authorSearchTask, "author-realtime-search-task");
    }

    private void setupAuthorResultsListView() {
        if (authorResultsListView == null) {
            return;
        }

        authorResultsListView.setItems(authorSearchItems);
        authorResultsListView.setPlaceholder(new Label("Type at least 3 characters to search."));

        authorResultsListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AuthorSearchResultDto author, boolean empty) {
                super.updateItem(author, empty);

                if (empty || author == null) {
                    setText(null);
                } else {
                    setText(getAuthorDisplayName(author));
                }
            }
        });

        authorResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        stopCurrentPublicationLoading();
                        setSelectedAuthor(selected);
                        clearResultArea();
                    }
                }
        );
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

    private void setupLoadPublicationsOption() {
        setPublicationReportVisible(false);

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    stopCurrentPublicationLoading();
                    publicationItems.clear();
                    expectedPublicationCount = 0;
                    updatePublicationsLoadedLabel(0, 0);
                    setPublicationReportVisible(false);
                }
            });
        }
    }

    private void setupCharts() {
        setupLineChart(totalArticlesLineChart, totalArticlesYearAxis, totalArticlesCountAxis);
        setupLineChart(publicationTypeLineChart, publicationTypeYearAxis, publicationTypeCountAxis);
    }

    private void setupLineChart(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis
    ) {
        if (chart != null) {
            chart.setAnimated(false);
            chart.setCreateSymbols(false);
            chart.setLegendVisible(true);
        }

        if (xAxis != null) {
            xAxis.setAutoRanging(false);
            xAxis.setForceZeroInRange(false);
            xAxis.setMinorTickVisible(false);
        }

        if (yAxis != null) {
            yAxis.setAutoRanging(false);
            yAxis.setForceZeroInRange(true);
            yAxis.setMinorTickVisible(false);
        }
    }

    private void setupPublicationsTable() {
        if (publicationsTable != null) {
            publicationsTable.setItems(publicationItems);
            publicationsTable.setPrefHeight(350);
            publicationsTable.setMinHeight(350);
            publicationsTable.setMaxHeight(350);
            publicationsTable.setFixedCellSize(34);
        }

        if (publicationYearColumn != null) {
            publicationYearColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(cellData.getValue().year())
            );
        }

        if (publicationTypeColumn != null) {
            publicationTypeColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().articleType()))
            );
        }

        if (publicationTitleColumn != null) {
            publicationTitleColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().title()))
            );
        }

        if (publicationVenueColumn != null) {
            publicationVenueColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(buildVenueDisplayName(cellData.getValue()))
            );
        }

        if (publicationAuthorsColumn != null) {
            publicationAuthorsColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().authors()))
            );
        }

        if (publicationPagesColumn != null) {
            publicationPagesColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().pages()))
            );
        }

        if (publicationUrlColumn != null) {
            publicationUrlColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(firstNonBlank(
                            cellData.getValue().url(),
                            cellData.getValue().ee()
                    )))
            );
        }
    }

    @FXML
    private void loadAuthorProfile() {
        stopCurrentPublicationLoading();

        if (selectedAuthor == null || selectedAuthor.authorId() == null) {
            showError(
                    "Δεν επιλέχθηκε συγγραφέας",
                    "Πρέπει πρώτα να αναζητήσεις και να επιλέξεις έναν συγγραφέα."
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

        int authorId = selectedAuthor.authorId();

        boolean shouldLoadPublications =
                loadPublicationsCheckBox != null && loadPublicationsCheckBox.isSelected();

        Task<AuthorPageData> task = new Task<>() {
            @Override
            protected AuthorPageData call() {
                Optional<AuthorProfileDto> profile =
                        authorService.getAuthorProfile(
                                authorId,
                                yearRange.startYear(),
                                yearRange.endYear()
                        );

                List<AuthorYearlyStatsDto> yearlyStats =
                        authorService.getAuthorYearlyStats(
                                authorId,
                                yearRange.startYear(),
                                yearRange.endYear()
                        );

                List<AuthorYearlyStatsByTypeDto> yearlyStatsByType =
                        authorService.getAuthorYearlyStatsByType(
                                authorId,
                                yearRange.startYear(),
                                yearRange.endYear()
                        );

                return new AuthorPageData(
                        profile.orElse(null),
                        yearlyStats,
                        yearlyStatsByType
                );
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            AuthorPageData data = task.getValue();

            if (data.profile() == null) {
                clearResultArea();
                showInfo(
                        "Δεν βρέθηκαν δεδομένα",
                        "Δεν υπάρχουν δεδομένα για τον επιλεγμένο συγγραφέα στο συγκεκριμένο εύρος χρονιών."
                );
                return;
            }

            updateProfileLabels(data.profile());
            updateTotalArticlesChart(data.yearlyStats());
            updatePublicationTypeChart(data.yearlyStatsByType());

            expectedPublicationCount = nullToZero(data.profile().totalArticles());

            if (shouldLoadPublications) {
                publicationItems.clear();
                setPublicationReportVisible(true);
                updatePublicationsLoadedLabel(0, expectedPublicationCount);

                startPublicationBatchLoading(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            } else {
                publicationItems.clear();
                expectedPublicationCount = 0;
                updatePublicationsLoadedLabel(0, 0);
                setPublicationReportVisible(false);
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα φόρτωσης προφίλ συγγραφέα", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "author-profile-task");
    }

    private void startPublicationBatchLoading(
            int authorId,
            Integer startYear,
            Integer endYear
    ) {
        stopCurrentPublicationLoading();

        publicationItems.clear();
        stopPublicationLoading = false;

        updatePublicationsLoadedLabel(0, expectedPublicationCount);

        publicationLoadingTask = new Task<>() {
            @Override
            protected Void call() {
                int lastArticleId = 0;

                while (!stopPublicationLoading && !isCancelled()) {
                    List<AuthorPublicationDto> batch =
                            authorService.getAuthorPublicationsBatch(
                                    authorId,
                                    startYear,
                                    endYear,
                                    lastArticleId,
                                    PUBLICATION_BATCH_SIZE
                            );

                    if (batch.isEmpty()) {
                        break;
                    }

                    AuthorPublicationDto lastPublication = batch.get(batch.size() - 1);

                    if (lastPublication.articleId() == null) {
                        break;
                    }

                    lastArticleId = lastPublication.articleId();

                    Platform.runLater(() -> {
                        publicationItems.addAll(batch);
                        updatePublicationsLoadedLabel(
                                publicationItems.size(),
                                expectedPublicationCount
                        );
                    });
                }

                Platform.runLater(() ->
                        updatePublicationsLoadedLabel(
                                publicationItems.size(),
                                expectedPublicationCount
                        )
                );

                return null;
            }
        };

        publicationLoadingTask.setOnFailed(event -> {
            Throwable exception = publicationLoadingTask.getException();
            showError(
                    "Σφάλμα φόρτωσης δημοσιεύσεων",
                    exception == null ? null : exception.getMessage()
            );
        });

        startBackgroundTask(publicationLoadingTask, "author-publication-batch-loading-task");
    }

    private void stopCurrentPublicationLoading() {
        stopPublicationLoading = true;

        if (publicationLoadingTask != null && publicationLoadingTask.isRunning()) {
            publicationLoadingTask.cancel();
        }
    }

    @FXML
    private void clear() {
        stopCurrentPublicationLoading();

        if (authorSearchTask != null && authorSearchTask.isRunning()) {
            authorSearchTask.cancel();
        }

        if (authorSearchField != null) {
            authorSearchField.clear();
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.getSelectionModel().clearSelection();
        }

        if (toYearComboBox != null) {
            toYearComboBox.getSelectionModel().clearSelection();
            refreshToYearOptions(null);
        }

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.setSelected(false);
        }

        clearAuthorResults();
        setSelectedAuthor(null);
        clearResultArea();
    }

    @FXML
    private void backToHome() {
        stopCurrentPublicationLoading();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(HOME_FXML_PATH)
            );

            Node node = backButton != null ? backButton : authorSearchField;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            showError(
                    "Σφάλμα πλοήγησης",
                    "Δεν ήταν δυνατή η επιστροφή στην αρχική σελίδα. Έλεγξε το HOME_FXML_PATH."
            );
        }
    }

    private void updateProfileLabels(AuthorProfileDto profile) {
        setLabelText(firstYearLabel, profile.firstYear());
        setLabelText(lastYearLabel, profile.lastYear());
        setLabelText(activeYearsLabel, profile.activeYears());

        setLabelText(totalArticlesLabel, profile.totalArticles());
        setLabelText(totalJournalArticlesLabel, profile.totalJournalArticles());
        setLabelText(totalConferenceArticlesLabel, profile.totalConferenceArticles());

        setLabelText(distinctJournalsLabel, profile.distinctJournals());
        setLabelText(distinctConferencesLabel, profile.distinctConferences());

        setLabelText(avgArticlesPerYearLabel, formatDouble(profile.avgArticlesPerYear()));
    }

    private void updateTotalArticlesChart(List<AuthorYearlyStatsDto> yearlyStats) {
        if (totalArticlesLineChart != null) {
            totalArticlesLineChart.getData().clear();
        }

        if (yearlyStats == null || yearlyStats.isEmpty()) {
            resetLineChartAxes(totalArticlesYearAxis, totalArticlesCountAxis);
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        long maxArticles = 0;

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Total articles");

        for (AuthorYearlyStatsDto stat : yearlyStats) {
            if (stat.year() == null) {
                continue;
            }

            long totalArticles = nullToZero(stat.totalArticles());

            minYear = minYear == null ? stat.year() : Math.min(minYear, stat.year());
            maxYear = maxYear == null ? stat.year() : Math.max(maxYear, stat.year());
            maxArticles = Math.max(maxArticles, totalArticles);

            series.getData().add(new XYChart.Data<>(stat.year(), totalArticles));
        }

        if (totalArticlesLineChart != null) {
            totalArticlesLineChart.getData().add(series);
        }

        configureYearAxis(totalArticlesYearAxis, minYear, maxYear);
        configureValueAxis(totalArticlesCountAxis, maxArticles);
    }

    private void updatePublicationTypeChart(List<AuthorYearlyStatsByTypeDto> yearlyStats) {
        if (publicationTypeLineChart != null) {
            publicationTypeLineChart.getData().clear();
        }

        if (yearlyStats == null || yearlyStats.isEmpty()) {
            resetLineChartAxes(publicationTypeYearAxis, publicationTypeCountAxis);
            return;
        }

        Integer minYear = null;
        Integer maxYear = null;
        long maxValue = 0;

        XYChart.Series<Number, Number> journalSeries = new XYChart.Series<>();
        journalSeries.setName("Journal articles");

        XYChart.Series<Number, Number> conferenceSeries = new XYChart.Series<>();
        conferenceSeries.setName("Conference articles");

        for (AuthorYearlyStatsByTypeDto stat : yearlyStats) {
            if (stat.year() == null) {
                continue;
            }

            long journalArticles = nullToZero(stat.totalJournalArticles());
            long conferenceArticles = nullToZero(stat.totalConferenceArticles());

            minYear = minYear == null ? stat.year() : Math.min(minYear, stat.year());
            maxYear = maxYear == null ? stat.year() : Math.max(maxYear, stat.year());

            maxValue = Math.max(maxValue, journalArticles);
            maxValue = Math.max(maxValue, conferenceArticles);

            journalSeries.getData().add(new XYChart.Data<>(stat.year(), journalArticles));
            conferenceSeries.getData().add(new XYChart.Data<>(stat.year(), conferenceArticles));
        }

        if (publicationTypeLineChart != null) {
            publicationTypeLineChart.getData().add(journalSeries);
            publicationTypeLineChart.getData().add(conferenceSeries);
        }

        configureYearAxis(publicationTypeYearAxis, minYear, maxYear);
        configureValueAxis(publicationTypeCountAxis, maxValue);
    }

    private void clearCharts() {
        if (totalArticlesLineChart != null) {
            totalArticlesLineChart.getData().clear();
        }

        if (publicationTypeLineChart != null) {
            publicationTypeLineChart.getData().clear();
        }

        resetLineChartAxes(totalArticlesYearAxis, totalArticlesCountAxis);
        resetLineChartAxes(publicationTypeYearAxis, publicationTypeCountAxis);
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

    private void clearResultArea() {
        setLabelText(firstYearLabel, "-");
        setLabelText(lastYearLabel, "-");
        setLabelText(activeYearsLabel, "-");

        setLabelText(totalArticlesLabel, "-");
        setLabelText(totalJournalArticlesLabel, "-");
        setLabelText(totalConferenceArticlesLabel, "-");

        setLabelText(distinctJournalsLabel, "-");
        setLabelText(distinctConferencesLabel, "-");

        setLabelText(avgArticlesPerYearLabel, "-");

        clearCharts();

        publicationItems.clear();
        expectedPublicationCount = 0;
        updatePublicationsLoadedLabel(0, 0);

        setPublicationReportVisible(false);
    }

    private void clearAuthorResults() {
        authorSearchItems.clear();

        if (authorResultsListView != null) {
            authorResultsListView.getSelectionModel().clearSelection();
            authorResultsListView.setPlaceholder(new Label("Type at least 3 characters to search."));
        }

        setAuthorResultsVisible(false);
    }

    private void setSelectedAuthor(AuthorSearchResultDto author) {
        selectedAuthor = author;

        if (selectedAuthorLabel == null) {
            return;
        }

        if (author == null) {
            selectedAuthorLabel.setText("No author selected");
        } else {
            selectedAuthorLabel.setText(getAuthorDisplayName(author));
        }
    }

    private void setAuthorResultsVisible(boolean visible) {
        if (authorResultsContainer != null) {
            authorResultsContainer.setVisible(visible);
            authorResultsContainer.setManaged(visible);
        }
    }

    private void setPublicationReportVisible(boolean visible) {
        if (publicationReportPanel != null) {
            publicationReportPanel.setVisible(visible);
            publicationReportPanel.setManaged(visible);
        }
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

    private String getAuthorDisplayName(AuthorSearchResultDto author) {
        if (author == null) {
            return "Unknown author";
        }

        if (author.authorName() != null && !author.authorName().isBlank()) {
            return author.authorName();
        }

        return "Author #" + author.authorId();
    }

    private String buildVenueDisplayName(AuthorPublicationDto publication) {
        if (publication == null) {
            return "-";
        }

        if (publication.journalName() != null && !publication.journalName().isBlank()) {
            return publication.journalName();
        }

        if (publication.conferenceAcronym() != null && !publication.conferenceAcronym().isBlank()) {
            if (publication.conferenceTitle() != null && !publication.conferenceTitle().isBlank()) {
                return publication.conferenceAcronym() + " - " + publication.conferenceTitle();
            }

            return publication.conferenceAcronym();
        }

        if (publication.conferenceTitle() != null && !publication.conferenceTitle().isBlank()) {
            return publication.conferenceTitle();
        }

        return "-";
    }

    private List<AuthorSearchResultDto> sortAuthorsByRelevance(
            List<AuthorSearchResultDto> results,
            String query
    ) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuery = normalizeSearchText(query);
        List<AuthorSearchResultDto> sortedResults = new ArrayList<>(results);

        sortedResults.sort((first, second) -> {
            int firstScore = authorRelevanceScore(first, normalizedQuery);
            int secondScore = authorRelevanceScore(second, normalizedQuery);

            if (firstScore != secondScore) {
                return Integer.compare(firstScore, secondScore);
            }

            String firstName = normalizeSearchText(first.authorName());
            String secondName = normalizeSearchText(second.authorName());

            if (firstName.length() != secondName.length()) {
                return Integer.compare(firstName.length(), secondName.length());
            }

            return firstName.compareTo(secondName);
        });

        return sortedResults;
    }

    private int authorRelevanceScore(AuthorSearchResultDto author, String query) {
        String name = normalizeSearchText(author == null ? null : author.authorName());

        if (name.equals(query)) {
            return 0;
        }

        if (name.startsWith(query)) {
            return 1;
        }

        if (name.contains(" " + query)) {
            return 2;
        }

        if (name.contains(query)) {
            return 3;
        }

        if (containsAllQueryWords(name, query)) {
            return 4;
        }

        return 5;
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private void updatePublicationsLoadedLabel(long loaded, long total) {
        if (publicationsLoadedLabel != null) {
            publicationsLoadedLabel.setText("Articles Loaded: " + loaded + " / " + total);
        }
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
        if (authorSearchField != null) {
            authorSearchField.setDisable(loading);
        }

        if (authorResultsListView != null) {
            authorResultsListView.setDisable(loading);
        }

        if (fromYearComboBox != null) {
            fromYearComboBox.setDisable(loading);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(loading);
        }

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.setDisable(loading);
        }

        if (loadProfileButton != null) {
            loadProfileButton.setDisable(loading);
        }

        if (clearButton != null) {
            clearButton.setDisable(loading);
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

    private record AuthorPageData(
            AuthorProfileDto profile,
            List<AuthorYearlyStatsDto> yearlyStats,
            List<AuthorYearlyStatsByTypeDto> yearlyStatsByType
    ) {
    }
}