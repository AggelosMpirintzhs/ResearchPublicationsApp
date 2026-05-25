package controllers;

import dto.conference.ConferenceArticleDto;
import dto.conference.ConferenceProfileDto;
import dto.conference.ConferenceRankingDto;
import dto.conference.ConferenceYearlyStatsDto;
import dto.journal.JournalArticleDto;
import dto.journal.JournalProfileDto;
import dto.journal.JournalRankingDto;
import dto.journal.JournalYearlyStatsDto;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import service.VenueService;
import util.TableCopySupport;
import util.TableSearchSupport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VenueController {

    private static final String HOME_FXML_PATH = "/app/hello-view.fxml";

    private static final int MIN_YEAR = 1900;
    private static final Integer SEARCH_LIMIT = 20;

    private static final int MIN_VENUE_SEARCH_LENGTH = 3;
    private static final int VENUE_SEARCH_DEBOUNCE_MS = 250;

    private static final Integer ARTICLE_BATCH_SIZE = 1000;

    private static final String SEARCH_MODE_TITLE = "Title";
    private static final String SEARCH_MODE_AUTHOR = "Author";

    private final VenueService venueService = new VenueService();

    private final ObservableList<Object> articleItems =
            FXCollections.observableArrayList();

    private Object selectedVenue;

    private PauseTransition venueSearchDebounce;

    private Task<List<Object>> venueSearchTask;

    private volatile boolean stopArticleLoading = false;

    private Task<Void> articleLoadingTask;

    private long expectedArticleCount = 0;

    private TableSearchSupport<Object> articleSearchSupport;

    private enum EnterActionTarget {
        PROFILE_LOAD,
        ARTICLE_SEARCH_NEXT
    }

    private EnterActionTarget enterActionTarget = EnterActionTarget.PROFILE_LOAD;

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
    private ComboBox<String> articleSearchModeComboBox;

    @FXML
    private TextField articleSearchField;

    @FXML
    private Button articleSearchNextButton;

    @FXML
    private Label articleSearchStatusLabel;

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
        setupArticleSearchSupport();
        setupLoadArticlesOption();
        setupYearlyCharts();
        setupEnterModeTracking();
        setupEnterKeyBehavior();

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
        executeVenueSearch(true);
    }

    private void searchVenuesRealtime() {
        executeVenueSearch(false);
    }

    private void executeVenueSearch(boolean showAlerts) {
        stopCurrentArticleLoading();

        String venueType = getSelectedVenueType();
        String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

        if (venueSearchTask != null && venueSearchTask.isRunning()) {
            venueSearchTask.cancel();
        }

        if (venueType == null || venueType.isBlank()) {
            if (showAlerts) {
                showError("No venue type selected", "You must select Journal or Conference first.");
            }

            return;
        }

        if (searchText.length() < MIN_VENUE_SEARCH_LENGTH) {
            clearVenueResults();
            setSelectedVenue(null);
            clearResultArea();

            if (showAlerts && !searchText.isBlank()) {
                showError(
                        "Invalid search",
                        "You must type at least 3 characters."
                );
            }

            if (showAlerts && searchText.isBlank()) {
                showError(
                        "Invalid search",
                        "You must type a journal or conference name."
                );
            }

            return;
        }

        String requestedVenueType = venueType;
        String requestedSearchText = searchText;

        venueSearchTask = new Task<>() {
            @Override
            protected List<Object> call() {
                return venueService.searchVenues(
                        requestedVenueType,
                        requestedSearchText,
                        SEARCH_LIMIT
                );
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

            clearResultArea();
            setSelectedVenue(null);
            renderVenueResults(results);

            if (results.isEmpty() && showAlerts) {
                showInfo(
                        "No results found",
                        "No journal or conference was found for this search text."
                );
            }
        });

        venueSearchTask.setOnFailed(event -> {
            if (showAlerts) {
                setLoading(false);
            }

            Throwable exception = venueSearchTask.getException();

            if (exception != null) {
                exception.printStackTrace();
            }

            if (showAlerts) {
                showError("Venue search error", exception == null ? null : exception.getMessage());
            } else {
                clearVenueResults();
            }
        });

        startBackgroundTask(venueSearchTask, "venue-realtime-search-task");
    }

    @FXML
    private void loadVenueProfile() {
        stopCurrentVenueSearch();
        stopCurrentArticleLoading();
        resetArticleSearchNavigation();

        String venueType = getSelectedVenueType();

        if (venueType == null || venueType.isBlank()) {
            showError("No venue type selected", "You must select Journal or Conference first.");
            return;
        }

        if (selectedVenue == null) {
            showError(
                    "No venue selected",
                    "You must search and select a venue from the results list first."
            );
            return;
        }

        YearRange yearRange;

        try {
            yearRange = getSelectedYearRange();
        } catch (IllegalArgumentException exception) {
            showError("Invalid year range", exception.getMessage());
            return;
        }

        boolean shouldLoadArticles =
                loadArticlesCheckBox != null && loadArticlesCheckBox.isSelected();

        Task<VenueService.VenuePageData> task = new Task<>() {
            @Override
            protected VenueService.VenuePageData call() {
                return venueService.loadVenuePageData(
                        venueType,
                        selectedVenue,
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            VenueService.VenuePageData data = task.getValue();

            if (!data.hasAnyData()) {
                clearResultArea();
                showInfo(
                        "No data found",
                        "There is no available data for the selected venue."
                );
                return;
            }

            updateProfileLabels(data.profile());
            updateRankingPanel(data.ranking());
            updateYearlyLineCharts(data.yearlyStats());

            expectedArticleCount = data.expectedArticleCount();

            if (shouldLoadArticles && data.hasArticleData()) {
                articleItems.clear();
                resetArticleSearchNavigation();

                setArticleReportVisible(true);
                updateArticlesLoadedLabel(0, expectedArticleCount);

                startArticleBatchLoading(
                        data.venueType(),
                        data.venueId(),
                        yearRange.startYear(),
                        yearRange.endYear()
                );
            } else {
                articleItems.clear();
                resetArticleSearchNavigation();

                updateArticlesLoadedLabel(0, expectedArticleCount);
                setArticleReportVisible(false);
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Venue profile loading error", exception == null ? null : exception.getMessage());
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
        resetArticleSearchNavigation();

        stopArticleLoading = false;

        updateArticlesLoadedLabel(0, expectedArticleCount);

        articleLoadingTask = new Task<>() {
            @Override
            protected Void call() {
                venueService.loadVenueArticlesInBatches(
                        venueType,
                        venueId,
                        startYear,
                        endYear,
                        Integer.valueOf(0),
                        ARTICLE_BATCH_SIZE,
                        batch -> Platform.runLater(() -> {
                            articleItems.addAll(batch);
                            updateArticlesLoadedLabel(articleItems.size(), expectedArticleCount);

                            if (articleSearchSupport != null) {
                                articleSearchSupport.refreshStatus();
                                articleSearchSupport.refreshTable();
                            }
                        }),
                        () -> !stopArticleLoading && !isCancelled()
                );

                Platform.runLater(() -> {
                    updateArticlesLoadedLabel(articleItems.size(), expectedArticleCount);

                    if (articleSearchSupport != null) {
                        articleSearchSupport.refreshStatus();
                        articleSearchSupport.refreshTable();
                    }
                });

                return null;
            }
        };

        articleLoadingTask.setOnFailed(event -> {
            Throwable exception = articleLoadingTask.getException();
            showError(
                    "Article loading error",
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
    private void findNextArticleMatch() {
        if (articleSearchSupport != null) {
            articleSearchSupport.findNext();
        }
    }

    private void resetArticleSearchNavigation() {
        if (articleSearchSupport != null) {
            articleSearchSupport.resetNavigation();
            articleSearchSupport.refreshStatus();
            articleSearchSupport.refreshTable();
        }
    }

    private void clearArticleSearchText() {
        if (articleSearchSupport != null) {
            articleSearchSupport.clearSearchText();
        }
    }

    private void stopCurrentVenueSearch() {
        if (venueSearchDebounce != null) {
            venueSearchDebounce.stop();
        }

        if (venueSearchTask != null && venueSearchTask.isRunning()) {
            venueSearchTask.cancel();
        }
    }

    @FXML
    private void clear() {
        stopCurrentVenueSearch();
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

        clearArticleSearchText();
        clearVenueResults();
        setSelectedVenue(null);
        clearResultArea();
        setLoading(false);
        useProfileLoadEnter();
    }

    @FXML
    private void backToHome() {
        stopCurrentVenueSearch();
        stopCurrentArticleLoading();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(HOME_FXML_PATH)
            );

            Node node = backButton != null ? backButton : venueTypeComboBox;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            showError(
                    "Navigation error",
                    "Could not return to the home page. Please check HOME_FXML_PATH."
            );
        }
    }

    private void setupVenueTypeComboBox() {
        if (venueTypeComboBox == null) {
            return;
        }

        venueTypeComboBox.getItems().setAll(
                VenueService.TYPE_JOURNAL,
                VenueService.TYPE_CONFERENCE
        );
        venueTypeComboBox.getSelectionModel().select(VenueService.TYPE_JOURNAL);

        venueTypeComboBox.setOnAction(event -> {
            useProfileLoadEnter();

            stopCurrentArticleLoading();
            stopCurrentVenueSearch();

            clearVenueResults();
            setSelectedVenue(null);
            clearResultArea();

            String searchText = venueSearchField == null ? "" : venueSearchField.getText().trim();

            if (searchText.length() >= MIN_VENUE_SEARCH_LENGTH && venueSearchDebounce != null) {
                venueSearchDebounce.playFromStart();
            }
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
        venueSearchDebounce = new PauseTransition(
                Duration.millis(VENUE_SEARCH_DEBOUNCE_MS)
        );

        venueSearchDebounce.setOnFinished(event -> searchVenuesRealtime());

        if (venueSearchField == null) {
            return;
        }

        venueSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            useProfileLoadEnter();

            if (venueSearchDebounce != null) {
                venueSearchDebounce.playFromStart();
            }
        });

        venueSearchField.setOnAction(event -> event.consume());
    }

    private void setupEnterModeTracking() {

        registerProfileLoadEnterTarget(venueTypeComboBox);
        registerProfileLoadEnterTarget(venueSearchField);
        registerProfileLoadEnterTarget(searchVenueButton);
        registerProfileLoadEnterTarget(venueResultsListView);
        registerProfileLoadEnterTarget(fromYearComboBox);
        registerProfileLoadEnterTarget(toYearComboBox);
        registerProfileLoadEnterTarget(loadArticlesCheckBox);
        registerProfileLoadEnterTarget(loadProfileButton);

        registerArticleSearchEnterTarget(articleSearchModeComboBox);
        registerArticleSearchEnterTarget(articleSearchField);
        registerArticleSearchEnterTarget(articleSearchNextButton);

        if (venueTypeComboBox != null) {
            venueTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    useProfileLoadEnter()
            );
        }

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

        if (loadArticlesCheckBox != null) {
            loadArticlesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    useProfileLoadEnter()
            );
        }

        if (articleSearchField != null) {
            articleSearchField.textProperty().addListener((observable, oldValue, newValue) ->
                    useArticleSearchNextEnter()
            );
        }

        if (articleSearchModeComboBox != null) {
            articleSearchModeComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                    useArticleSearchNextEnter()
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

    private void registerArticleSearchEnterTarget(Node node) {
        if (node == null) {
            return;
        }

        node.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                useArticleSearchNextEnter();
            }
        });

        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
                useArticleSearchNextEnter()
        );
    }

    private void useProfileLoadEnter() {
        enterActionTarget = EnterActionTarget.PROFILE_LOAD;
    }

    private void useArticleSearchNextEnter() {
        enterActionTarget = EnterActionTarget.ARTICLE_SEARCH_NEXT;
    }

    private boolean shouldEnterRunArticleSearch() {
        if (articleSearchSupport == null) {
            return false;
        }

        if (articleReportPanel != null && !articleReportPanel.isVisible()) {
            return false;
        }

        if (articleSearchField != null && articleSearchField.isFocused()) {
            return true;
        }

        if (articleSearchModeComboBox != null && articleSearchModeComboBox.isFocused()) {
            return true;
        }

        if (articleSearchNextButton != null && articleSearchNextButton.isFocused()) {
            return true;
        }

        return enterActionTarget == EnterActionTarget.ARTICLE_SEARCH_NEXT;
    }

    private boolean shouldIgnoreProfileLoadEnter() {

        return venueSearchField != null && venueSearchField.isFocused();
    }

    private void loadSelectedVenueFromEnter() {
        Object selected = null;

        if (venueResultsListView != null) {
            selected = venueResultsListView.getSelectionModel().getSelectedItem();
        }

        if (selected == null) {
            selected = selectedVenue;
        }

        if (selected == null) {
            return;
        }

        setSelectedVenue(selected);
        loadVenueProfile();
    }

    private void setupEnterKeyBehavior() {
        Platform.runLater(() -> {
            if (venueResultsListView == null || venueResultsListView.getScene() == null) {
                return;
            }

            venueResultsListView.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() != KeyCode.ENTER) {
                    return;
                }

                event.consume();

                if (shouldEnterRunArticleSearch()) {
                    findNextArticleMatch();
                    return;
                }

                if (shouldIgnoreProfileLoadEnter()) {
                    return;
                }

                loadSelectedVenueFromEnter();
            });
        });
    }

    private void setupVenueResultsListView() {
        if (venueResultsListView == null) {
            return;
        }

        venueResultsListView.setPlaceholder(new Label("Search results will appear here."));

        venueResultsListView.setCellFactory(listView -> {
            ListCell<Object> cell = new ListCell<>() {
                @Override
                protected void updateItem(Object venue, boolean empty) {
                    super.updateItem(venue, empty);

                    if (empty || venue == null) {
                        setText(null);
                    } else {
                        setText(getVenueDisplayName(venue));
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
                    venueResultsListView.getSelectionModel().select(cell.getItem());
                    openSelectedVenueProfile();
                    event.consume();
                }
            });

            return cell;
        });

        venueResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        useProfileLoadEnter();
                        stopCurrentArticleLoading();
                        setSelectedVenue(selected);
                        clearResultArea();
                    }
                }
        );

        venueResultsListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();

                if (venueResultsListView.getSelectionModel().getSelectedItem() != null) {
                    useProfileLoadEnter();
                    openSelectedVenueProfile();
                }
            }
        });
    }

    private void openSelectedVenueProfile() {
        if (venueResultsListView == null) {
            return;
        }

        Object selected = venueResultsListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        useProfileLoadEnter();
        setSelectedVenue(selected);
        loadVenueProfile();
    }

    private void setupLoadArticlesOption() {
        setArticleReportVisible(false);

        if (loadArticlesCheckBox != null) {
            loadArticlesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                useProfileLoadEnter();

                if (!newValue) {
                    stopCurrentArticleLoading();
                    articleItems.clear();
                    expectedArticleCount = 0;
                    updateArticlesLoadedLabel(0, 0);
                    resetArticleSearchNavigation();
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

            TableSearchSupport.applyReadableSelectionStyle(articlesTable);
            TableCopySupport.enableCellCopy(articlesTable);
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
            TableSearchSupport.makePlainTextColumn(titleColumn);
        }

        if (authorsColumn != null) {
            authorsColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticleAuthors(cellData.getValue())))
            );
            TableSearchSupport.makePlainTextColumn(authorsColumn);
        }

        if (pagesColumn != null) {
            pagesColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticlePages(cellData.getValue())))
            );
            TableCopySupport.makeStringColumnTextSelectable(pagesColumn);
        }

        if (urlColumn != null) {
            urlColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(extractArticleUrlOrEe(cellData.getValue())))
            );
            TableCopySupport.makeStringColumnTextSelectable(urlColumn);
        }
    }

    private void setupArticleSearchSupport() {
        articleSearchSupport = new TableSearchSupport<>(
                articlesTable,
                articleItems,
                articleSearchModeComboBox,
                articleSearchField,
                articleSearchNextButton,
                articleSearchStatusLabel
        );

        articleSearchSupport.addSearchMode(
                SEARCH_MODE_TITLE,
                titleColumn,
                article -> nullToDash(extractArticleTitle(article))
        );

        articleSearchSupport.addSearchMode(
                SEARCH_MODE_AUTHOR,
                authorsColumn,
                article -> nullToDash(extractArticleAuthors(article))
        );

        articleSearchSupport.initialize(SEARCH_MODE_TITLE);
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

    private void clearResultArea() {
        setLabelText(rankingLabel, "-");
        setLabelText(categoryLabel, "-");
        setLabelText(rankingMetricsLabel, "-");

        clearProfileLabels();
        clearLineCharts();

        articleItems.clear();
        resetArticleSearchNavigation();

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
            throw new IllegalArgumentException("The To year must be greater than or equal to the From year.");
        }

        return new YearRange(startYear, endYear);
    }

    private Integer getComboBoxYearValue(ComboBox<Integer> comboBox) {
        if (comboBox == null) {
            return null;
        }

        return comboBox.getValue();
    }

    private String getVenueDisplayName(Object venue) {
        return venueService.getVenueDisplayName(venue);
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
            return firstNonBlank(journalArticle.ee(), journalArticle.url());
        }

        if (article instanceof ConferenceArticleDto conferenceArticle) {
            return firstNonBlank(conferenceArticle.ee(), conferenceArticle.url());
        }

        return null;
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
            fromYearComboBox.setDisable(false);
        }

        if (toYearComboBox != null) {
            toYearComboBox.setDisable(false);
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