package controllers;

import dto.author.AuthorProfileDto;
import dto.author.AuthorPublicationDto;
import dto.author.AuthorSearchResultDto;
import dto.author.AuthorYearlyStatsByTypeDto;
import dto.author.AuthorYearlyStatsDto;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import service.AuthorService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AuthorController {

    /*
     * Αν το hello-view.fxml βρίσκεται σε άλλο path, άλλαξε αυτό εδώ.
     * Π.χ. αν είναι στο src/main/resources/fxml/hello-view.fxml,
     * βάλε: "/fxml/hello-view.fxml"
     */
    private static final String HOME_FXML_PATH = "/com/example/project_pvasil/hello-view.fxml";

    private final AuthorService authorService = new AuthorService();

    @FXML
    private TextField authorSearchField;

    @FXML
    private ComboBox<AuthorSearchResultDto> authorComboBox;

    @FXML
    private TextField fromYearField;

    @FXML
    private TextField toYearField;

    @FXML
    private Button searchButton;

    @FXML
    private Button loadProfileButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button backButton;

    @FXML
    private Label authorNameLabel;

    @FXML
    private Label firstYearLabel;

    @FXML
    private Label lastYearLabel;

    @FXML
    private Label activeYearsLabel;

    @FXML
    private Label totalPublicationsLabel;

    @FXML
    private Label avgPublicationsPerYearLabel;

    @FXML
    private LineChart<Number, Number> yearlyPublicationsChart;

    @FXML
    private BarChart<String, Number> yearlyStatsByTypeChart;

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
    private TableColumn<AuthorPublicationDto, String> publicationPagesColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationAuthorsColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, String> publicationUrlColumn;

    @FXML
    private TableColumn<AuthorPublicationDto, Long> publicationAuthorCountColumn;

    @FXML
    public void initialize() {
        setupAuthorComboBox();
        setupPublicationsTable();
        setupSearchField();
        clearResultArea();
    }

    @FXML
    private void searchAuthors() {
        String searchText = authorSearchField == null ? "" : authorSearchField.getText();

        Task<List<AuthorSearchResultDto>> task = new Task<>() {
            @Override
            protected List<AuthorSearchResultDto> call() {
                return authorService.searchAuthors(searchText, 20);
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);

            List<AuthorSearchResultDto> authors = task.getValue();
            authorComboBox.getItems().setAll(authors);

            if (!authors.isEmpty()) {
                authorComboBox.getSelectionModel().selectFirst();
            } else {
                showInfo(
                        "Δεν βρέθηκαν αποτελέσματα",
                        "Δεν βρέθηκε συγγραφέας με αυτό το κείμενο αναζήτησης."
                );
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable exception = task.getException();
            showError("Σφάλμα αναζήτησης", exception == null ? null : exception.getMessage());
        });

        new Thread(task, "author-search-task").start();
    }

    @FXML
    private void loadAuthorProfile() {
        AuthorSearchResultDto selectedAuthor = authorComboBox.getValue();

        if (selectedAuthor == null) {
            showError(
                    "Δεν επιλέχθηκε συγγραφέας",
                    "Πρέπει πρώτα να αναζητήσεις και να επιλέξεις έναν συγγραφέα."
            );
            return;
        }

        try {
            Integer startYear = parseOptionalYear(
                    fromYearField == null ? null : fromYearField.getText(),
                    "From year"
            );

            Integer endYear = parseOptionalYear(
                    toYearField == null ? null : toYearField.getText(),
                    "To year"
            );

            int authorId = getAuthorId(selectedAuthor);

            Task<AuthorPageData> task = new Task<>() {
                @Override
                protected AuthorPageData call() {
                    Optional<AuthorProfileDto> profile =
                            authorService.getAuthorProfile(authorId, startYear, endYear);

                    List<AuthorYearlyStatsDto> yearlyStats =
                            authorService.getAuthorYearlyStats(authorId, startYear, endYear);

                    List<AuthorYearlyStatsByTypeDto> yearlyStatsByType =
                            authorService.getAuthorYearlyStatsByType(authorId, startYear, endYear);

                    /*
                     * Αυτό μπορεί να είναι βαρύ αν ο συγγραφέας έχει πολλές δημοσιεύσεις.
                     * Αργότερα μπορούμε να το αλλάξουμε σε pagination.
                     */
                    List<AuthorPublicationDto> publications =
                            authorService.getAuthorPublications(authorId, startYear, endYear);

                    return new AuthorPageData(
                            profile,
                            yearlyStats,
                            yearlyStatsByType,
                            publications
                    );
                }
            };

            setLoading(true);

            task.setOnSucceeded(event -> {
                setLoading(false);

                AuthorPageData data = task.getValue();

                if (data.profile().isEmpty()) {
                    clearResultArea();
                    showInfo(
                            "Δεν βρέθηκαν δεδομένα",
                            "Δεν υπάρχουν δημοσιεύσεις για τον συγκεκριμένο συγγραφέα στο επιλεγμένο εύρος χρονιών."
                    );
                    return;
                }

                updateProfileLabels(data.profile().get());
                updateYearlyPublicationsChart(data.yearlyStats());
                updateYearlyStatsByTypeChart(data.yearlyStatsByType());
                updatePublicationsTable(data.publications());
            });

            task.setOnFailed(event -> {
                setLoading(false);
                Throwable exception = task.getException();
                showError("Σφάλμα φόρτωσης", exception == null ? null : exception.getMessage());
            });

            new Thread(task, "author-profile-task").start();

        } catch (IllegalArgumentException exception) {
            showError("Λάθος είσοδος", exception.getMessage());
        }
    }

    @FXML
    private void clear() {
        if (authorSearchField != null) {
            authorSearchField.clear();
        }

        if (authorComboBox != null) {
            authorComboBox.getItems().clear();
        }

        if (fromYearField != null) {
            fromYearField.clear();
        }

        if (toYearField != null) {
            toYearField.clear();
        }

        clearResultArea();
    }

    @FXML
    private void backToHome() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(HOME_FXML_PATH)
            );

            Node node = backButton != null ? backButton : authorComboBox;
            node.getScene().setRoot(root);

        } catch (IOException | NullPointerException exception) {
            showError(
                    "Σφάλμα πλοήγησης",
                    "Δεν ήταν δυνατή η επιστροφή στην αρχική σελίδα. Έλεγξε το HOME_FXML_PATH."
            );
        }
    }

    private void setupSearchField() {
        if (authorSearchField != null) {
            authorSearchField.setOnAction(event -> searchAuthors());
        }
    }

    private void setupAuthorComboBox() {
        if (authorComboBox == null) {
            return;
        }

        authorComboBox.setPromptText("Select author");

        authorComboBox.setCellFactory(listView -> new ListCell<>() {
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

        authorComboBox.setButtonCell(new ListCell<>() {
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
    }

    private void setupPublicationsTable() {
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

        if (publicationPagesColumn != null) {
            publicationPagesColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().pages()))
            );
        }

        if (publicationAuthorsColumn != null) {
            publicationAuthorsColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().authors()))
            );
        }

        if (publicationUrlColumn != null) {
            publicationUrlColumn.setCellValueFactory(cellData ->
                    new SimpleStringProperty(nullToDash(cellData.getValue().url()))
            );
        }

        if (publicationAuthorCountColumn != null) {
            publicationAuthorCountColumn.setCellValueFactory(cellData ->
                    new ReadOnlyObjectWrapper<>(cellData.getValue().authorCount())
            );
        }
    }

    private void updateProfileLabels(AuthorProfileDto profile) {
        setLabelText(authorNameLabel, profile.authorName());
        setLabelText(firstYearLabel, profile.firstYear());
        setLabelText(lastYearLabel, profile.lastYear());
        setLabelText(activeYearsLabel, profile.activeYears());
        setLabelText(totalPublicationsLabel, profile.totalArticles());
        setLabelText(avgPublicationsPerYearLabel, formatDouble(profile.avgArticlesPerYear()));
    }

    private void updateYearlyPublicationsChart(List<AuthorYearlyStatsDto> yearlyStats) {
        if (yearlyPublicationsChart == null) {
            return;
        }

        yearlyPublicationsChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Publications per year");

        for (AuthorYearlyStatsDto stat : yearlyStats) {
            if (stat.year() != null && stat.totalArticles() != null) {
                series.getData().add(
                        new XYChart.Data<>(stat.year(), stat.totalArticles())
                );
            }
        }

        yearlyPublicationsChart.getData().add(series);
    }

    private void updateYearlyStatsByTypeChart(List<AuthorYearlyStatsByTypeDto> yearlyStatsByType) {
        if (yearlyStatsByTypeChart == null) {
            return;
        }

        yearlyStatsByTypeChart.getData().clear();

        XYChart.Series<String, Number> journalSeries = new XYChart.Series<>();
        journalSeries.setName("Journal articles");

        XYChart.Series<String, Number> conferenceSeries = new XYChart.Series<>();
        conferenceSeries.setName("Conference articles");

        for (AuthorYearlyStatsByTypeDto stat : yearlyStatsByType) {
            if (stat.year() == null) {
                continue;
            }

            String year = String.valueOf(stat.year());

            journalSeries.getData().add(
                    new XYChart.Data<>(
                            year,
                            stat.totalJournalArticles() == null ? 0 : stat.totalJournalArticles()
                    )
            );

            conferenceSeries.getData().add(
                    new XYChart.Data<>(
                            year,
                            stat.totalConferenceArticles() == null ? 0 : stat.totalConferenceArticles()
                    )
            );
        }

        yearlyStatsByTypeChart.getData().add(journalSeries);
        yearlyStatsByTypeChart.getData().add(conferenceSeries);
    }

    private void updatePublicationsTable(List<AuthorPublicationDto> publications) {
        if (publicationsTable != null) {
            publicationsTable.getItems().setAll(publications);
        }
    }

    private void clearResultArea() {
        setLabelText(authorNameLabel, "-");
        setLabelText(firstYearLabel, "-");
        setLabelText(lastYearLabel, "-");
        setLabelText(activeYearsLabel, "-");
        setLabelText(totalPublicationsLabel, "-");
        setLabelText(avgPublicationsPerYearLabel, "-");

        if (yearlyPublicationsChart != null) {
            yearlyPublicationsChart.getData().clear();
        }

        if (yearlyStatsByTypeChart != null) {
            yearlyStatsByTypeChart.getData().clear();
        }

        if (publicationsTable != null) {
            publicationsTable.getItems().clear();
        }
    }

    private Integer parseOptionalYear(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            int year = Integer.parseInt(text.trim());

            if (year <= 0) {
                throw new IllegalArgumentException("Το πεδίο " + fieldName + " πρέπει να είναι θετικός αριθμός.");
            }

            return year;

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Το πεδίο " + fieldName + " πρέπει να είναι αριθμός.");
        }
    }

    private int getAuthorId(AuthorSearchResultDto author) {
        if (author == null || author.authorId() == null || author.authorId() <= 0) {
            throw new IllegalArgumentException("Δεν βρέθηκε έγκυρο authorId για τον επιλεγμένο συγγραφέα.");
        }

        return author.authorId();
    }

    private String getAuthorDisplayName(AuthorSearchResultDto author) {
        if (author == null) {
            return "Unknown author";
        }

        if (author.authorName() != null && !author.authorName().isBlank()) {
            return author.authorName();
        }

        if (author.authorId() != null) {
            return "Author #" + author.authorId();
        }

        return "Unknown author";
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

    private void setLabelText(Label label, Object value) {
        if (label != null) {
            label.setText(value == null ? "-" : String.valueOf(value));
        }
    }

    private String nullToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "-";
        }

        return String.format("%.2f", value);
    }

    private void setLoading(boolean loading) {
        if (searchButton != null) {
            searchButton.setDisable(loading);
        }

        if (loadProfileButton != null) {
            loadProfileButton.setDisable(loading);
        }

        if (clearButton != null) {
            clearButton.setDisable(loading);
        }

        if (authorSearchField != null) {
            authorSearchField.setDisable(loading);
        }

        if (authorComboBox != null) {
            authorComboBox.setDisable(loading);
        }

        if (fromYearField != null) {
            fromYearField.setDisable(loading);
        }

        if (toYearField != null) {
            toYearField.setDisable(loading);
        }
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

    private record AuthorPageData(
            Optional<AuthorProfileDto> profile,
            List<AuthorYearlyStatsDto> yearlyStats,
            List<AuthorYearlyStatsByTypeDto> yearlyStatsByType,
            List<AuthorPublicationDto> publications
    ) {
    }
}