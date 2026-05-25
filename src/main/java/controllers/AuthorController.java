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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import service.AuthorService;
import util.TableCopySupport;
import util.TableSearchSupport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuthorController {

    private static final String HOME_FXML_PATH = "/app/hello-view.fxml";

    private static final int MIN_YEAR = 1900;
    private static final int SEARCH_LIMIT = 20;
    private static final int MIN_SEARCH_LENGTH = 3;
    private static final int SEARCH_DEBOUNCE_MS = 250;
    private static final int PUBLICATION_BATCH_SIZE = 1000;

    private static final String SEARCH_MODE_TITLE = "Title";
    private static final String SEARCH_MODE_VENUE = "Venue";

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

    private TableSearchSupport<AuthorPublicationDto> publicationSearchSupport;

    private enum EnterActionTarget {
        PROFILE_LOAD,
        PUBLICATION_SEARCH_NEXT
    }

    private EnterActionTarget enterActionTarget = EnterActionTarget.PROFILE_LOAD;

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
    private ComboBox<String> publicationSearchModeComboBox;

    @FXML
    private TextField publicationSearchField;

    @FXML
    private Button publicationSearchNextButton;

    @FXML
    private Label publicationSearchStatusLabel;

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
        setupPublicationSearchSupport();
        setupEnterModeTracking();
        setupEnterKeyBehavior();

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
            useProfileLoadEnter();

            if (searchDebounce != null) {
                searchDebounce.playFromStart();
            }
        });

        authorSearchField.setOnAction(event -> event.consume());
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

            authorSearchItems.setAll(results);
            setAuthorResultsVisible(true);
        });

        authorSearchTask.setOnFailed(event -> {
            Throwable exception = authorSearchTask.getException();
            showError("Failed author search", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(authorSearchTask, "author-realtime-search-task");
    }

    private void setupAuthorResultsListView() {
        if (authorResultsListView == null) {
            return;
        }

        authorResultsListView.setItems(authorSearchItems);
        authorResultsListView.setPlaceholder(new Label("Type at least 3 characters to search."));

        authorResultsListView.setCellFactory(listView -> {
            ListCell<AuthorSearchResultDto> cell = new ListCell<>() {
                @Override
                protected void updateItem(AuthorSearchResultDto author, boolean empty) {
                    super.updateItem(author, empty);

                    if (empty || author == null) {
                        setText(null);
                    } else {
                        setText(getAuthorDisplayName(author));
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (
                        event.getButton() == MouseButton.PRIMARY
                                && event.getClickCount() == 2
                                && !cell.isEmpty()
                ) {
                    useProfileLoadEnter();
                    authorResultsListView.getSelectionModel().select(cell.getItem());
                    openSelectedAuthorProfile();
                    event.consume();
                }
            });

            return cell;
        });

        authorResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        useProfileLoadEnter();
                        stopCurrentPublicationLoading();
                        setSelectedAuthor(selected);
                        clearResultArea();
                    }
                }
        );

        authorResultsListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();

                if (authorResultsListView.getSelectionModel().getSelectedItem() != null) {
                    useProfileLoadEnter();
                    openSelectedAuthorProfile();
                }
            }
        });
    }

    private void openSelectedAuthorProfile() {
        if (authorResultsListView == null) {
            return;
        }

        AuthorSearchResultDto selected = authorResultsListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        useProfileLoadEnter();
        setSelectedAuthor(selected);
        loadAuthorProfile();
    }

    private void setupEnterModeTracking() {
        registerProfileLoadEnterTarget(authorSearchField);
        registerProfileLoadEnterTarget(authorResultsListView);
        registerProfileLoadEnterTarget(fromYearComboBox);
        registerProfileLoadEnterTarget(toYearComboBox);
        registerProfileLoadEnterTarget(loadPublicationsCheckBox);
        registerProfileLoadEnterTarget(loadProfileButton);

        registerPublicationSearchEnterTarget(publicationSearchModeComboBox);
        registerPublicationSearchEnterTarget(publicationSearchField);
        registerPublicationSearchEnterTarget(publicationSearchNextButton);

        if (fromYearComboBox != null) {
            fromYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    useProfileLoadEnter()
            );
        }

        if (toYearComboBox != null) {
            toYearComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    useProfileLoadEnter()
            );
        }

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    useProfileLoadEnter()
            );
        }

        if (publicationSearchField != null) {
            publicationSearchField.textProperty().addListener((observable, oldValue, newValue) ->
                    usePublicationSearchNextEnter()
            );
        }

        if (publicationSearchModeComboBox != null) {
            publicationSearchModeComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    usePublicationSearchNextEnter()
            );
        }
    }

    private void registerProfileLoadEnterTarget(Node node) {
        if (node == null) {
            return;
        }

        node.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                useProfileLoadEnter();
            }
        });

        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
                useProfileLoadEnter()
        );
    }

    private void registerPublicationSearchEnterTarget(Node node) {
        if (node == null) {
            return;
        }

        node.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                usePublicationSearchNextEnter();
            }
        });

        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
                usePublicationSearchNextEnter()
        );
    }

    private void useProfileLoadEnter() {
        enterActionTarget = EnterActionTarget.PROFILE_LOAD;
    }

    private void usePublicationSearchNextEnter() {
        enterActionTarget = EnterActionTarget.PUBLICATION_SEARCH_NEXT;
    }

    private boolean shouldEnterRunPublicationSearch() {
        if (publicationSearchSupport == null) {
            return false;
        }

        if (publicationReportPanel != null && !publicationReportPanel.isVisible()) {
            return false;
        }

        if (publicationSearchField != null && publicationSearchField.isFocused()) {
            return true;
        }

        if (publicationSearchModeComboBox != null && publicationSearchModeComboBox.isFocused()) {
            return true;
        }

        if (publicationSearchNextButton != null && publicationSearchNextButton.isFocused()) {
            return true;
        }

        return enterActionTarget == EnterActionTarget.PUBLICATION_SEARCH_NEXT;
    }

    private boolean shouldIgnoreProfileLoadEnter() {
        return authorSearchField != null && authorSearchField.isFocused();
    }

    private void loadSelectedAuthorFromEnter() {
        AuthorSearchResultDto selected = null;

        if (authorResultsListView != null) {
            selected = authorResultsListView.getSelectionModel().getSelectedItem();
        }

        if (selected == null) {
            selected = selectedAuthor;
        }

        if (selected == null) {
            return;
        }

        setSelectedAuthor(selected);
        loadAuthorProfile();
    }

    private void setupEnterKeyBehavior() {
        Platform.runLater(() -> {
            if (authorResultsListView == null || authorResultsListView.getScene() == null) {
                return;
            }

            authorResultsListView.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() != KeyCode.ENTER) {
                    return;
                }

                event.consume();

                if (shouldEnterRunPublicationSearch()) {
                    findNextPublicationMatch();
                    return;
                }

                if (shouldIgnoreProfileLoadEnter()) {
                    return;
                }

                loadSelectedAuthorFromEnter();
            });
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

    private void setupLoadPublicationsOption() {
        setPublicationReportVisible(false);

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                useProfileLoadEnter();

                if (!newValue) {
                    stopCurrentPublicationLoading();
                    publicationItems.clear();
                    expectedPublicationCount = 0;
                    updatePublicationsLoadedLabel(0, 0);
                    resetPublicationSearchNavigation();
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

            TableSearchSupport.applyReadableSelectionStyle(publicationsTable);

            TableCopySupport.enableCellCopy(publicationsTable);
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
            TableCopySupport.makeStringColumnTextSelectable(publicationTypeColumn);
        }

        if (publicationTitleColumn != null) {
            publicationTitleColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().title()))
            );

            TableSearchSupport.makePlainTextColumn(publicationTitleColumn);
        }

        if (publicationVenueColumn != null) {
            publicationVenueColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(buildVenueDisplayName(cellData.getValue()))
            );

            TableSearchSupport.makePlainTextColumn(publicationVenueColumn);
        }

        if (publicationAuthorsColumn != null) {
            publicationAuthorsColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().authors()))
            );

            TableSearchSupport.makePlainTextColumn(publicationAuthorsColumn);
        }

        if (publicationPagesColumn != null) {
            publicationPagesColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().pages()))
            );
            TableCopySupport.makeStringColumnTextSelectable(publicationPagesColumn);
        }

        if (publicationUrlColumn != null) {
            publicationUrlColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(firstNonBlank(
                            cellData.getValue().ee(),
                            cellData.getValue().url()
                    )))
            );
            TableCopySupport.makeStringColumnTextSelectable(publicationUrlColumn);
        }
    }

    private void setupPublicationSearchSupport() {
        publicationSearchSupport = new TableSearchSupport<>(
                publicationsTable,
                publicationItems,
                publicationSearchModeComboBox,
                publicationSearchField,
                publicationSearchNextButton,
                publicationSearchStatusLabel
        );

        publicationSearchSupport.addSearchMode(
                SEARCH_MODE_TITLE,
                publicationTitleColumn,
                publication -> nullToDash(publication.title())
        );

        publicationSearchSupport.addSearchMode(
                SEARCH_MODE_VENUE,
                publicationVenueColumn,
                this::buildVenueDisplayName
        );

        publicationSearchSupport.initialize(SEARCH_MODE_TITLE);
    }

    @FXML
    private void findNextPublicationMatch() {
        if (publicationSearchSupport != null) {
            publicationSearchSupport.findNext();
        }
    }

    private void resetPublicationSearchNavigation() {
        if (publicationSearchSupport != null) {
            publicationSearchSupport.resetNavigation();
            publicationSearchSupport.refreshStatus();
            publicationSearchSupport.refreshTable();
        }
    }

    private void clearPublicationSearchText() {
        if (publicationSearchSupport != null) {
            publicationSearchSupport.clearSearchText();
        }
    }

    @FXML
    private void loadAuthorProfile() {
        stopCurrentPublicationLoading();
        resetPublicationSearchNavigation();

        if (selectedAuthor == null || selectedAuthor.authorId() == null) {
            showError(
                    "No author selected",
                    "You first need to search and select an author."
            );
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Wrong year selection", exception.getMessage());
            return;
        }

        int authorId = selectedAuthor.authorId();

        boolean shouldLoadPublications =
                loadPublicationsCheckBox != null && loadPublicationsCheckBox.isSelected();

        Task<AuthorService.AuthorPageData> task = new Task<>() {
            @Override
            protected AuthorService.AuthorPageData call() {
                return authorService.loadAuthorPageData(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            AuthorService.AuthorPageData data = task.getValue();

            if (!data.hasProfile()) {
                clearResultArea();
                showInfo(
                        "No data found",
                        "There are no data for this author in the selected time period."
                );
                return;
            }

            updateProfileLabels(data.profile());
            updateTotalArticlesChart(data.yearlyStats());
            updatePublicationTypeChart(data.yearlyStatsByType());

            expectedPublicationCount = data.expectedPublicationCount();

            if (shouldLoadPublications) {
                publicationItems.clear();
                resetPublicationSearchNavigation();
                setPublicationReportVisible(true);
                updatePublicationsLoadedLabel(0, expectedPublicationCount);

                startPublicationBatchLoading(
                        authorId,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            } else {
                publicationItems.clear();
                resetPublicationSearchNavigation();
                expectedPublicationCount = 0;
                updatePublicationsLoadedLabel(0, 0);
                setPublicationReportVisible(false);
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Error loading author profile", exception == null ? null : exception.getMessage());
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
        resetPublicationSearchNavigation();
        stopPublicationLoading = false;

        updatePublicationsLoadedLabel(0, expectedPublicationCount);

        publicationLoadingTask = new Task<>() {
            @Override
            protected Void call() {
                authorService.loadAuthorPublicationsInBatches(
                        authorId,
                        startYear,
                        endYear,
                        0,
                        PUBLICATION_BATCH_SIZE,
                        batch -> Platform.runLater(() -> {
                            publicationItems.addAll(batch);

                            updatePublicationsLoadedLabel(
                                    publicationItems.size(),
                                    expectedPublicationCount
                            );

                            if (publicationSearchSupport != null) {
                                publicationSearchSupport.refreshStatus();
                                publicationSearchSupport.refreshTable();
                            }
                        }),
                        () -> !stopPublicationLoading && !isCancelled()
                );

                Platform.runLater(() -> {
                    updatePublicationsLoadedLabel(
                            publicationItems.size(),
                            expectedPublicationCount
                    );

                    if (publicationSearchSupport != null) {
                        publicationSearchSupport.refreshStatus();
                        publicationSearchSupport.refreshTable();
                    }
                });

                return null;
            }
        };

        publicationLoadingTask.setOnFailed(event -> {
            Throwable exception = publicationLoadingTask.getException();
            showError(
                    "Failed publications loading",
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
        clearPublicationSearchText();
        clearResultArea();
        useProfileLoadEnter();
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
                    "Failed to navigate",
                    "Failed to return to the home page.Check the HOME_FXML_PATH."
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
        double maxArticles = 0;

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
        double maxValue = 0;

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
        resetPublicationSearchNavigation();

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
            throw new IllegalArgumentException("The start year must be greater than the end year");
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
            fromYearComboBox.setDisable(false);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(false);
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
        alert.setContentText(message == null || message.isBlank() ? "Unknown error." : message);
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
}
