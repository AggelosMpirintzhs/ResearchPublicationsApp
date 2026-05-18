package util;

import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public final class TableCopySupport {

    private TableCopySupport() {
    }

    public static <S> void enableCellCopy(TableView<S> tableView) {
        if (tableView == null) {
            return;
        }

        /*
         * Single click:
         * Επιλέγεται κελί αντί για ολόκληρη γραμμή.
         */
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        /*
         * Χρειάζεται focus για να πιάνει σωστά το Ctrl+C / Cmd+C.
         */
        tableView.setFocusTraversable(true);
        tableView.setOnMouseClicked(event -> tableView.requestFocus());

        tableView.addEventHandler(KeyEvent.KEY_PRESSED, event ->
                copySelectedCells(tableView, event)
        );
    }

    public static <S> void makeStringColumnTextSelectable(TableColumn<S, String> column) {
        if (column == null) {
            return;
        }

        column.setCellFactory(tableColumn -> new TableCell<>() {
            private final TextField textField = new TextField();

            {
                textField.setEditable(false);
                textField.setFocusTraversable(true);
                textField.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-background-insets: 0;" +
                                "-fx-padding: 0;"
                );

                /*
                 * Escape: επιστροφή σε normal προβολή κελιού.
                 */
                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        showTextOnly();
                        event.consume();
                    }
                });

                /*
                 * Αν κάνουμε click έξω, κλείνει το selectable mode.
                 */
                textField.focusedProperty().addListener((observable, oldValue, focused) -> {
                    if (!focused) {
                        showTextOnly();
                    }
                });

                /*
                 * Double click:
                 * Το κελί γίνεται read-only TextField ώστε να μπορείς
                 * να επιλέξεις συγκεκριμένο κομμάτι του κειμένου.
                 */
                setOnMouseClicked(event -> {
                    if (!isEmpty() && event.getClickCount() == 2) {
                        showTextField();
                        event.consume();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (getGraphic() == textField) {
                    textField.setText(item == null ? "" : item);
                    setText(null);
                    return;
                }

                setText(item == null ? "" : item);
                setGraphic(null);
            }

            private void showTextOnly() {
                setGraphic(null);
                setText(getItem() == null ? "" : getItem());
            }

            private void showTextField() {
                String text = getItem() == null ? "" : getItem();

                textField.setText(text);
                setText(null);
                setGraphic(textField);

                textField.requestFocus();
                textField.positionCaret(text.length());
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <S> void copySelectedCells(TableView<S> tableView, KeyEvent event) {
        /*
         * isShortcutDown:
         * Ctrl σε Windows/Linux, Cmd σε Mac.
         */
        if (!event.isShortcutDown() || event.getCode() != KeyCode.C) {
            return;
        }

        ObservableList<TablePosition> selectedCells =
                tableView.getSelectionModel().getSelectedCells();

        if (selectedCells == null || selectedCells.isEmpty()) {
            return;
        }

        List<TablePosition> cellsToCopy = new ArrayList<>(selectedCells);

        cellsToCopy.sort((first, second) -> {
            int rowComparison = Integer.compare(first.getRow(), second.getRow());

            if (rowComparison != 0) {
                return rowComparison;
            }

            return Integer.compare(first.getColumn(), second.getColumn());
        });

        StringBuilder copiedText = new StringBuilder();

        int currentRow = -1;
        boolean firstCellInRow = true;

        for (TablePosition position : cellsToCopy) {
            int row = position.getRow();

            if (row < 0 || row >= tableView.getItems().size()) {
                continue;
            }

            if (currentRow == -1) {
                currentRow = row;
            } else if (row != currentRow) {
                copiedText.append(System.lineSeparator());
                currentRow = row;
                firstCellInRow = true;
            } else if (!firstCellInRow) {
                copiedText.append("\t");
            }

            TableColumn column = position.getTableColumn();
            Object cellData = column == null ? null : column.getCellData(row);

            copiedText.append(cellData == null ? "" : cellData.toString());

            firstCellInRow = false;
        }

        if (copiedText.length() == 0) {
            return;
        }

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(copiedText.toString());
        Clipboard.getSystemClipboard().setContent(clipboardContent);

        event.consume();
    }
}