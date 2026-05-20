package util;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TableCopySupportTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        JavaFxTestSupport.initializeJavaFx();
    }

    @Test
    void enableCellCopy_whenTableViewIsNull_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> TableCopySupport.enableCellCopy(null));
    }

    @Test
    void makeStringColumnTextSelectable_whenColumnIsNull_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> TableCopySupport.makeStringColumnTextSelectable(null));
    }

    @Test
    void makeStringColumnTextSelectable_shouldInstallCellFactory() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TableColumn<TestRow, String> column = new TableColumn<>("Title");

            TableCopySupport.makeStringColumnTextSelectable(column);

            Assertions.assertNotNull(column.getCellFactory());
        });
    }

    @Test
    void enableCellCopy_whenShortcutCIsPressed_shouldCopySelectedCellsToClipboard() throws Exception {
        JavaFxTestSupport.runOnJavaFxThread(() -> {
            TableView<TestRow> tableView = new TableView<>();

            TableColumn<TestRow, String> authorColumn = new TableColumn<>("Author");
            authorColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(cellData.getValue().author())
            );

            TableColumn<TestRow, String> titleColumn = new TableColumn<>("Title");
            titleColumn.setCellValueFactory(cellData ->
                    new ReadOnlyStringWrapper(cellData.getValue().title())
            );

            tableView.getColumns().add(authorColumn);
            tableView.getColumns().add(titleColumn);

            tableView.setItems(FXCollections.observableArrayList(
                    new TestRow("John Smith", "Database Systems"),
                    new TestRow("Maria Papadopoulou", "Machine Learning")
            ));

            TableCopySupport.enableCellCopy(tableView);

            Assertions.assertTrue(tableView.getSelectionModel().isCellSelectionEnabled());
            Assertions.assertTrue(tableView.isFocusTraversable());

            tableView.getSelectionModel().select(1, titleColumn);
            tableView.getSelectionModel().select(0, authorColumn);
            tableView.getSelectionModel().select(0, titleColumn);

            KeyEvent copyEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED,
                    "",
                    "",
                    KeyCode.C,
                    false,
                    true,
                    false,
                    false
            );

            tableView.fireEvent(copyEvent);

            String expectedText =
                    "John Smith\tDatabase Systems" +
                            System.lineSeparator() +
                            "Machine Learning";

            Assertions.assertEquals(
                    expectedText,
                    Clipboard.getSystemClipboard().getString()
            );
        });
    }

    private record TestRow(
            String author,
            String title
    ) {
    }
}