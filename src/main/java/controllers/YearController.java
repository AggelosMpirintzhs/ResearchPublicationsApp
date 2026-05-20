package controllers;

import dto.year.AvailableYearDto;
import dto.year.YearProfileDto;
import dto.year.YearPublicationDto;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import service.YearService;
import util.TableCopySupport;
import util.TableSearchSupport;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class YearController {

    private static final String HOME_FXML_PATH = "/app/hello-view.fxml";

    private static final int INITIAL_PUBLICATION_BATCH_SIZE = 100;
    private static final int PUBLICATION_BATCH_SIZE = 1000;

    private static final String SEARCH_MODE_TITLE = "Title";
    private static final String SEARCH_MODE_VENUE = "Venue";
    private static final String SEARCH_MODE_AUTHOR = "Author";

    private final YearService yearService = new YearService();

    private final ObservableList<YearPublicationDto> publicationItems =
            FXCollections.observableArrayList();

    /*
     * To loadVersion mas prostateuei apo palia background tasks.
     * An o xristis patisei Clear i fortosei alli xronia, ta palia tasks
     * den prepei na ksanaenimerosoun to UI.
     */
    private final AtomicInteger loadVersion = new AtomicInteger(0);

    private volatile boolean stopPublicationLoading = false;

    private Task<Void> publicationLoadingTask;

    private long expectedPublicationCount = 0;

    /*
     * The in-table search logic is now handled by TableSearchSupport.
     * This keeps the controller smaller and avoids duplicate search/highlight code.
     */
    private TableSearchSupport<YearPublicationDto> publicationSearchSupport;

    @FXML
    private ComboBox<AvailableYearDto> yearComboBox;

    @FXML
    private ComboBox<String> publicationTypeComboBox;

    @FXML
    private CheckBox loadPublicationsCheckBox;

    @FXML
    private Button loadYearButton;

    @FXML
    private Button refreshYearsButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button backButton;

    @FXML
    private Label selectedYearLabel;

    @FXML
    private Label yearLabel;

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
    private Label totalAuthorOccurrencesLabel;

    @FXML
    private Label distinctAuthorsLabel;

    @FXML
    private Label avgAuthorsPerArticleLabel;

    @FXML
    private Label articlesLoadedLabel;

    @FXML
    private Label publicationLoadingLabel;

    @FXML
    private VBox publicationReportPanel;

    @FXML
    private ComboBox<String> publicationSearchModeComboBox;

    @FXML
    private TextField publicationSearchField;

    @FXML
    private Button publicationSearchNextButton;

    @FXML
    private Label publicationSearchStatusLabel;

    @FXML
    private TableView<YearPublicationDto> publicationsTable;

    @FXML
    private TableColumn<YearPublicationDto, Integer> publicationYearColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationTypeColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationTitleColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationVenueColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationAuthorsColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationPagesColumn;

    @FXML
    private TableColumn<YearPublicationDto, String> publicationUrlColumn;

    @FXML
    public void initialize() {
        setupYearComboBox();
        setupPublicationTypeComboBox();
        setupPublicationTable();
        setupPublicationSearchSupport();
        setupLoadPublicationsOption();

        clearResultArea();
        loadAvailableYears();
    }

    @FXML
    private void loadAvailableYears() {
        Task<List<AvailableYearDto>> task = new Task<>() {
            @Override
            protected List<AvailableYearDto> call() {
                return yearService.getAvailableYears();
            }
        };

        setYearLoading(true);

        task.setOnSucceeded(event -> {
            setYearLoading(false);

            List<AvailableYearDto> years = task.getValue();

            if (yearComboBox != null) {
                yearComboBox.getItems().setAll(years);
                yearComboBox.getSelectionModel().clearSelection();
            }

            updateSelectedYearLabel(null);

            if (years == null || years.isEmpty()) {
                showInfo(
                        "No years found",
                        "No available years were found in the database."
                );
            }
        });

        task.setOnFailed(event -> {
            setYearLoading(false);
            Throwable exception = task.getException();
            showError("Year loading error", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "available-years-task");
    }

    @FXML
    private void loadYearProfile() {
        stopCurrentPublicationLoading();
        resetPublicationSearchNavigation();

        int currentLoadVersion = loadVersion.incrementAndGet();

        AvailableYearDto selectedYear = yearComboBox == null ? null : yearComboBox.getValue();

        if (selectedYear == null || selectedYear.year() == null) {
            showError("No year selected", "You must select a year first.");
            return;
        }

        int year = selectedYear.year();
        String publicationType = getSelectedPublicationType();

        boolean shouldLoadPublications =
                loadPublicationsCheckBox != null && loadPublicationsCheckBox.isSelected();

        updateSelectedYearLabel(selectedYear);

        /*
         * Fast preview:
         * Ta vasika counts iparxoun idi sto AvailableYearDto apo to dropdown.
         * Ta emfanizoume amesa, prin girisei to varitero year_profile query.
         */
        updateFastYearPreview(selectedYear);

        expectedPublicationCount =
                yearService.getExpectedPublicationCount(selectedYear, publicationType);

        if (shouldLoadPublications) {
            publicationItems.clear();
            resetPublicationSearchNavigation();
            updateArticlesLoadedLabel(0, expectedPublicationCount);
            setPublicationLoadingText("Loading first publications...");

            startPublicationBatchLoading(
                    year,
                    publicationType,
                    currentLoadVersion
            );
        } else {
            publicationItems.clear();
            resetPublicationSearchNavigation();
            expectedPublicationCount = 0;
            updateArticlesLoadedLabel(0, 0);
            setPublicationLoadingText("Publication loading is disabled.");
        }

        /*
         * Full profile:
         * Trexei parallila me to proto batch ton articles.
         * Etsi den perimenei o xristis na teleiosei to ena gia na arxisei to allo.
         */
        Task<YearService.YearPageData> task = new Task<>() {
            @Override
            protected YearService.YearPageData call() {
                return yearService.loadYearPageData(
                        year,
                        publicationType
                );
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            if (currentLoadVersion != loadVersion.get()) {
                return;
            }

            setLoading(false);

            YearService.YearPageData data = task.getValue();

            if (data == null || !data.hasProfile()) {
                stopCurrentPublicationLoading();
                clearResultArea();
                showInfo(
                        "No data found",
                        "There are no statistics for the selected year."
                );
                return;
            }

            YearProfileDto profileDto = data.profile();

            updateSelectedYearLabel(selectedYear);
            updateProfileLabels(profileDto);

            /*
             * Otan girisei to plires profile, kanoume update to total tou counter
             * me tin pio akrivi timi.
             */
            expectedPublicationCount = data.expectedPublicationCount();
            updateArticlesLoadedLabel(publicationItems.size(), expectedPublicationCount);

            if (publicationSearchSupport != null) {
                publicationSearchSupport.refreshStatus();
                publicationSearchSupport.refreshTable();
            }
        });

        task.setOnFailed(event -> {
            if (currentLoadVersion != loadVersion.get()) {
                return;
            }

            setLoading(false);
            Throwable exception = task.getException();
            showError("Year profile loading error", exception == null ? null : exception.getMessage());
        });

        startBackgroundTask(task, "year-profile-task");
    }

    private void startPublicationBatchLoading(
            int year,
            String publicationType,
            int currentLoadVersion
    ) {
        publicationItems.clear();
        resetPublicationSearchNavigation();
        stopPublicationLoading = false;

        publicationLoadingTask = new Task<>() {
            @Override
            protected Void call() {
                yearService.loadYearPublicationsInBatches(
                        year,
                        publicationType,
                        null,
                        null,
                        null,
                        0,
                        INITIAL_PUBLICATION_BATCH_SIZE,
                        PUBLICATION_BATCH_SIZE,
                        batch -> Platform.runLater(() -> {
                            if (currentLoadVersion != loadVersion.get()) {
                                return;
                            }

                            publicationItems.addAll(batch);
                            updateArticlesLoadedLabel(publicationItems.size(), expectedPublicationCount);

                            if (publicationSearchSupport != null) {
                                publicationSearchSupport.refreshStatus();
                                publicationSearchSupport.refreshTable();
                            }

                            if (publicationItems.size() <= INITIAL_PUBLICATION_BATCH_SIZE) {
                                setPublicationLoadingText(
                                        "First publications loaded: " + publicationItems.size()
                                );
                            } else {
                                setPublicationLoadingText(
                                        "Loaded publications: " + publicationItems.size()
                                );
                            }
                        }),
                        () -> !stopPublicationLoading
                                && !isCancelled()
                                && currentLoadVersion == loadVersion.get()
                );

                Platform.runLater(() -> {
                    if (currentLoadVersion != loadVersion.get()) {
                        return;
                    }

                    if (stopPublicationLoading || isCancelled()) {
                        setPublicationLoadingText(
                                "Publication loading stopped. Loaded: " + publicationItems.size()
                        );
                    } else {
                        updateArticlesLoadedLabel(publicationItems.size(), expectedPublicationCount);

                        if (publicationSearchSupport != null) {
                            publicationSearchSupport.refreshStatus();
                            publicationSearchSupport.refreshTable();
                        }

                        setPublicationLoadingText(
                                "Finished loading publications: " + publicationItems.size()
                        );
                    }
                });

                return null;
            }
        };

        publicationLoadingTask.setOnFailed(event -> {
            if (currentLoadVersion != loadVersion.get()) {
                return;
            }

            Throwable exception = publicationLoadingTask.getException();
            setPublicationLoadingText("Error while loading publications.");
            showError(
                    "Publication loading error",
                    exception == null ? null : exception.getMessage()
            );
        });

        startBackgroundTask(publicationLoadingTask, "publication-batch-loading-task");
    }

    private void stopCurrentPublicationLoading() {
        stopPublicationLoading = true;

        if (publicationLoadingTask != null && publicationLoadingTask.isRunning()) {
            publicationLoadingTask.cancel();
        }
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
    private void clear() {
        loadVersion.incrementAndGet();
        stopCurrentPublicationLoading();

        if (yearComboBox != null) {
            yearComboBox.getSelectionModel().clearSelection();
        }

        if (publicationTypeComboBox != null) {
            publicationTypeComboBox.getSelectionModel().select(YearService.PUBLICATION_TYPE_ALL);
        }

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.setSelected(false);
        }

        clearPublicationSearchText();
        updateSelectedYearLabel(null);
        clearResultArea();
        setPublicationLoadingText("Publications cleared.");
        setLoading(false);
    }

    @FXML
    private void backToHome() {
        loadVersion.incrementAndGet();
        stopCurrentPublicationLoading();

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(HOME_FXML_PATH)
            );

            Node node = backButton != null ? backButton : yearComboBox;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            showError(
                    "Navigation error",
                    "Could not return to the home page. Please check HOME_FXML_PATH."
            );
        }
    }

    private void setupYearComboBox() {
        if (yearComboBox == null) {
            return;
        }

        yearComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AvailableYearDto yearDto, boolean empty) {
                super.updateItem(yearDto, empty);

                if (empty || yearDto == null) {
                    setText(null);
                } else {
                    setText(buildYearDisplayName(yearDto));
                }
            }
        });

        yearComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AvailableYearDto yearDto, boolean empty) {
                super.updateItem(yearDto, empty);

                if (empty || yearDto == null) {
                    setText(null);
                } else {
                    setText(buildYearDisplayName(yearDto));
                }
            }
        });

        yearComboBox.setOnAction(event ->
                updateSelectedYearLabel(yearComboBox.getValue())
        );
    }

    private void setupPublicationTypeComboBox() {
        if (publicationTypeComboBox == null) {
            return;
        }

        publicationTypeComboBox.getItems().setAll(
                YearService.PUBLICATION_TYPE_ALL,
                YearService.PUBLICATION_TYPE_JOURNAL,
                YearService.PUBLICATION_TYPE_CONFERENCE
        );

        publicationTypeComboBox.getSelectionModel().select(YearService.PUBLICATION_TYPE_ALL);
    }

    private void setupLoadPublicationsOption() {
        updatePublicationReportVisibility();

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                updatePublicationReportVisibility();

                if (!newValue) {
                    stopCurrentPublicationLoading();
                    publicationItems.clear();
                    expectedPublicationCount = 0;
                    updateArticlesLoadedLabel(0, 0);
                    resetPublicationSearchNavigation();
                    setPublicationLoadingText("Publication loading is disabled.");
                }
            });
        }
    }

    private void updatePublicationReportVisibility() {
        boolean visible = loadPublicationsCheckBox != null && loadPublicationsCheckBox.isSelected();

        if (publicationReportPanel != null) {
            publicationReportPanel.setVisible(visible);
            publicationReportPanel.setManaged(visible);
        }
    }

    private void setupPublicationTable() {
        if (publicationsTable != null) {
            publicationsTable.setItems(publicationItems);

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
                    new SimpleStringProperty(firstNonBlank(
                            cellData.getValue().ee(),
                            cellData.getValue().url()
                    ))
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

        publicationSearchSupport.addSearchMode(
                SEARCH_MODE_AUTHOR,
                publicationAuthorsColumn,
                publication -> nullToDash(publication.authors())
        );

        publicationSearchSupport.initialize(SEARCH_MODE_TITLE);
    }

    private void updateFastYearPreview(AvailableYearDto yearDto) {
        if (yearDto == null) {
            return;
        }

        /*
         * Amesa statistics apo to AvailableYearDto.
         * Ta ipoloipa tha gemisoun otan epistrepsei to plires YearProfileDto.
         */
        setLabelText(yearLabel, yearDto.year());

        setLabelText(totalArticlesLabel, yearDto.totalArticles());
        setLabelText(totalJournalArticlesLabel, yearDto.totalJournalArticles());
        setLabelText(totalConferenceArticlesLabel, yearDto.totalConferenceArticles());

        setLabelText(distinctJournalsLabel, "-");
        setLabelText(distinctConferencesLabel, "-");

        setLabelText(totalAuthorOccurrencesLabel, "-");
        setLabelText(distinctAuthorsLabel, "-");
        setLabelText(avgAuthorsPerArticleLabel, "-");
    }

    private void updateProfileLabels(YearProfileDto profile) {
        setLabelText(yearLabel, profile.year());

        setLabelText(totalArticlesLabel, profile.totalArticles());
        setLabelText(totalJournalArticlesLabel, profile.totalJournalArticles());
        setLabelText(totalConferenceArticlesLabel, profile.totalConferenceArticles());

        setLabelText(distinctJournalsLabel, profile.distinctJournals());
        setLabelText(distinctConferencesLabel, profile.distinctConferences());

        setLabelText(totalAuthorOccurrencesLabel, profile.totalAuthorOccurrences());
        setLabelText(distinctAuthorsLabel, profile.distinctAuthors());
        setLabelText(avgAuthorsPerArticleLabel, formatDouble(profile.avgAuthorsPerArticle()));
    }

    private void clearResultArea() {
        setLabelText(yearLabel, "-");

        setLabelText(totalArticlesLabel, "-");
        setLabelText(totalJournalArticlesLabel, "-");
        setLabelText(totalConferenceArticlesLabel, "-");

        setLabelText(distinctJournalsLabel, "-");
        setLabelText(distinctConferencesLabel, "-");

        setLabelText(totalAuthorOccurrencesLabel, "-");
        setLabelText(distinctAuthorsLabel, "-");
        setLabelText(avgAuthorsPerArticleLabel, "-");

        publicationItems.clear();
        resetPublicationSearchNavigation();

        expectedPublicationCount = 0;
        updateArticlesLoadedLabel(0, 0);
    }

    private void updateSelectedYearLabel(AvailableYearDto yearDto) {
        if (selectedYearLabel == null) {
            return;
        }

        if (yearDto == null) {
            selectedYearLabel.setText("No year selected");
        } else {
            selectedYearLabel.setText(buildYearDisplayName(yearDto));
        }
    }

    private String buildYearDisplayName(AvailableYearDto yearDto) {
        if (yearDto == null || yearDto.year() == null) {
            return "Unknown year";
        }

        return String.valueOf(yearDto.year());
    }

    private String getSelectedPublicationType() {
        if (publicationTypeComboBox == null || publicationTypeComboBox.getValue() == null) {
            return YearService.PUBLICATION_TYPE_ALL;
        }

        return publicationTypeComboBox.getValue();
    }

    private void updateArticlesLoadedLabel(long loaded, long total) {
        if (articlesLoadedLabel != null) {
            articlesLoadedLabel.setText(
                    "Articles Loaded: " + loaded + " / " + total
            );
        }
    }

    private void setPublicationLoadingText(String text) {
        if (publicationLoadingLabel != null) {
            publicationLoadingLabel.setText(text);
        }
    }

    private String buildVenueDisplayName(YearPublicationDto publication) {
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

        return "-";
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

    private void setYearLoading(boolean loading) {
        if (yearComboBox != null) {
            yearComboBox.setDisable(loading);
        }

        if (refreshYearsButton != null) {
            refreshYearsButton.setDisable(loading);
        }
    }

    private void setLoading(boolean loading) {
        /*
         * Kratame to Clear energopoiimeno gia na mporei o xristis
         * na stamataei/akyronei mia fortosi pou argise.
         */
        if (yearComboBox != null) {
            yearComboBox.setDisable(loading);
        }

        if (publicationTypeComboBox != null) {
            publicationTypeComboBox.setDisable(loading);
        }

        if (loadPublicationsCheckBox != null) {
            loadPublicationsCheckBox.setDisable(loading);
        }

        if (loadYearButton != null) {
            loadYearButton.setDisable(loading);
        }

        if (refreshYearsButton != null) {
            refreshYearsButton.setDisable(loading);
        }

        /*
         * Den kanoume disable to in-table search.
         * Etsi o xristis mporei na psaxnei mesa sta rows pou exoun idi fortothei,
         * akoma kai an ta epomena batches sinexizoun na erxontai.
         */
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
}