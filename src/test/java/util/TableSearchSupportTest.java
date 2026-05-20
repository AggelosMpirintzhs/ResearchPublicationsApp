package util;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TableSearchSupportTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        JavaFxTestSupport.initializeJavaFx();
    }

    @Test
    void addSearchMode_whenArgumentsAreValid_shouldInstallModeAndCellFactory() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode(
                    "Title",
                    setup.titleColumn,
                    TestRow::title
            );

            setup.searchSupport.initialize("Title");

            Assertions.assertEquals(1, setup.modeComboBox.getItems().size());
            Assertions.assertEquals("Title", setup.modeComboBox.getValue());
            Assertions.assertNotNull(setup.titleColumn.getCellFactory());
            Assertions.assertEquals("", setup.statusLabel.getText());
        });
    }

    @Test
    void addSearchMode_whenArgumentsAreInvalid_shouldIgnoreMode() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode(
                    "",
                    setup.titleColumn,
                    TestRow::title
            );

            setup.searchSupport.addSearchMode(
                    "Author",
                    null,
                    TestRow::author
            );

            setup.searchSupport.addSearchMode(
                    "Title",
                    setup.titleColumn,
                    null
            );

            setup.searchSupport.initialize("Title");

            Assertions.assertTrue(setup.modeComboBox.getItems().isEmpty());
            Assertions.assertNull(setup.modeComboBox.getValue());
            Assertions.assertEquals("", setup.statusLabel.getText());
        });
    }

    @Test
    void initialize_shouldSelectDefaultModeWhenItExists() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.addSearchMode("Author", setup.authorColumn, TestRow::author);

            setup.searchSupport.initialize("Author");

            Assertions.assertEquals(2, setup.modeComboBox.getItems().size());
            Assertions.assertEquals("Author", setup.modeComboBox.getValue());
            Assertions.assertEquals("", setup.statusLabel.getText());
        });
    }

    @Test
    void initialize_whenDefaultModeDoesNotExist_shouldSelectFirstAvailableMode() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.addSearchMode("Author", setup.authorColumn, TestRow::author);

            setup.searchSupport.initialize("Not Existing Mode");

            Assertions.assertEquals("Title", setup.modeComboBox.getValue());
        });
    }

    @Test
    void findNext_whenQueryMatchesRows_shouldSelectNextMatchingRowAndUpdateStatus() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("data");

            setup.searchSupport.findNext();

            Assertions.assertEquals(0, getSelectedRow(setup.tableView));
            Assertions.assertEquals("1/2", setup.statusLabel.getText());

            setup.searchSupport.findNext();

            Assertions.assertEquals(2, getSelectedRow(setup.tableView));
            Assertions.assertEquals("2/2", setup.statusLabel.getText());

            setup.searchSupport.findNext();

            Assertions.assertEquals(0, getSelectedRow(setup.tableView));
            Assertions.assertEquals("1/2", setup.statusLabel.getText());
        });
    }

    @Test
    void findNext_whenQueryDoesNotMatchRows_shouldShowZeroStatus() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("not-existing-text");

            setup.searchSupport.findNext();

            Assertions.assertEquals("0/0", setup.statusLabel.getText());
            Assertions.assertTrue(setup.tableView.getSelectionModel().getSelectedCells().isEmpty());
        });
    }

    @Test
    void findNext_whenSearchTextIsBlank_shouldClearStatus() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("   ");

            setup.searchSupport.findNext();

            Assertions.assertEquals("", setup.statusLabel.getText());
            Assertions.assertTrue(setup.tableView.getSelectionModel().getSelectedCells().isEmpty());
        });
    }

    @Test
    void clearSearchText_shouldClearSearchFieldResetNavigationAndClearStatus() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("data");
            setup.searchSupport.findNext();

            Assertions.assertEquals("1/2", setup.statusLabel.getText());

            setup.searchSupport.clearSearchText();

            Assertions.assertEquals("", setup.searchField.getText());
            Assertions.assertEquals("", setup.statusLabel.getText());
        });
    }

    @Test
    void refreshStatus_whenQueryHasMatchesButNoSelectedMatch_shouldShowZeroOutOfTotal() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("data");
            setup.searchSupport.resetNavigation();
            setup.searchSupport.refreshStatus();

            Assertions.assertEquals("0/2", setup.statusLabel.getText());
        });
    }

    @Test
    void refreshStatus_whenItemsAreEmpty_shouldShowZeroStatus() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TestSetup setup = createEmptyTestSetup();

            setup.searchSupport.addSearchMode("Title", setup.titleColumn, TestRow::title);
            setup.searchSupport.initialize("Title");

            setup.searchField.setText("data");
            setup.searchSupport.refreshStatus();

            Assertions.assertEquals("0/0", setup.statusLabel.getText());
        });
    }

    @Test
    void applyReadableSelectionStyle_whenTableViewIsNull_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> TableSearchSupport.applyReadableSelectionStyle(null));
    }

    @Test
    void makePlainTextColumn_whenColumnIsNull_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> TableSearchSupport.makePlainTextColumn(null));
    }

    @Test
    void makePlainTextColumn_shouldInstallCellFactory() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TableColumn<TestRow, String> column = new TableColumn<>("Title");

            TableSearchSupport.makePlainTextColumn(column);

            Assertions.assertNotNull(column.getCellFactory());
        });
    }

    private static TestSetup createTestSetup() {
        ObservableList<TestRow> items = FXCollections.observableArrayList(
                new TestRow("Database Systems", "John Smith"),
                new TestRow("Operating Systems", "Maria Papadopoulou"),
                new TestRow("Data Mining", "Nick Brown")
        );

        return createSetupWithItems(items);
    }

    private static TestSetup createEmptyTestSetup() {
        ObservableList<TestRow> items = FXCollections.observableArrayList();

        return createSetupWithItems(items);
    }

    private static TestSetup createSetupWithItems(ObservableList<TestRow> items) {
        TableView<TestRow> tableView = new TableView<>();

        TableColumn<TestRow, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().title())
        );

        TableColumn<TestRow, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().author())
        );

        tableView.getColumns().add(titleColumn);
        tableView.getColumns().add(authorColumn);
        tableView.setItems(items);

        ComboBox<String> modeComboBox = new ComboBox<>();
        TextField searchField = new TextField();
        Button nextButton = new Button("Next");
        Label statusLabel = new Label();

        TableSearchSupport<TestRow> searchSupport = new TableSearchSupport<>(
                tableView,
                items,
                modeComboBox,
                searchField,
                nextButton,
                statusLabel
        );

        return new TestSetup(
                tableView,
                items,
                titleColumn,
                authorColumn,
                modeComboBox,
                searchField,
                nextButton,
                statusLabel,
                searchSupport
        );
    }

    private static int getSelectedRow(TableView<TestRow> tableView) {
        Assertions.assertFalse(tableView.getSelectionModel().getSelectedCells().isEmpty());

        TablePosition<?, ?> selectedCell =
                tableView.getSelectionModel().getSelectedCells().get(0);

        return selectedCell.getRow();
    }

    private record TestRow(
            String title,
            String author
    ) {
    }

    private record TestSetup(
            TableView<TestRow> tableView,
            ObservableList<TestRow> items,
            TableColumn<TestRow, String> titleColumn,
            TableColumn<TestRow, String> authorColumn,
            ComboBox<String> modeComboBox,
            TextField searchField,
            Button nextButton,
            Label statusLabel,
            TableSearchSupport<TestRow> searchSupport
    ) {
    }
}