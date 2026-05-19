package util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class TableSearchSupport<S> {

    private static final String EMPTY_STATUS = "-";
    private static final String ZERO_STATUS = "0/0";

    private final TableView<S> tableView;
    private final ObservableList<S> items;
    private final ComboBox<String> modeComboBox;
    private final TextField searchField;
    private final Button nextButton;
    private final Label statusLabel;

    private final Map<String, SearchMode<S>> searchModes = new LinkedHashMap<>();

    private int currentSearchIndex = -1;

    public TableSearchSupport(
            TableView<S> tableView,
            ObservableList<S> items,
            ComboBox<String> modeComboBox,
            TextField searchField,
            Button nextButton,
            Label statusLabel
    ) {
        this.tableView = tableView;
        this.items = items;
        this.modeComboBox = modeComboBox;
        this.searchField = searchField;
        this.nextButton = nextButton;
        this.statusLabel = statusLabel;
    }

    public void addSearchMode(
            String modeName,
            TableColumn<S, String> column,
            Function<S, String> textExtractor
    ) {
        if (modeName == null || modeName.isBlank() || column == null || textExtractor == null) {
            return;
        }

        searchModes.put(
                modeName,
                new SearchMode<>(modeName, column, textExtractor)
        );

        installSearchableCellFactory(column, modeName);
    }

    public void initialize(String defaultMode) {
        if (modeComboBox != null) {
            modeComboBox.getItems().setAll(searchModes.keySet());

            if (defaultMode != null && searchModes.containsKey(defaultMode)) {
                modeComboBox.getSelectionModel().select(defaultMode);
            } else if (!searchModes.isEmpty()) {
                modeComboBox.getSelectionModel().select(0);
            }

            modeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                resetNavigation();
                refreshStatus();
                refreshTable();
            });
        }

        if (searchField != null) {
            searchField.setOnAction(event -> findNext());
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                resetNavigation();
                refreshStatus();
                refreshTable();
            });
        }

        if (nextButton != null) {
            nextButton.setOnAction(event -> findNext());
        }

        if (statusLabel != null) {
            statusLabel.setText(EMPTY_STATUS);
            statusLabel.setMinWidth(Math.max(statusLabel.getMinWidth(), 58));
        }

        if (tableView != null) {
            applyReadableSelectionStyle(tableView);
            tableView.getSelectionModel().setCellSelectionEnabled(true);
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            tableView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER && !getCurrentQuery().isBlank()) {
                    findNext();
                    event.consume();
                }
            });
        }

        if (items != null) {
            items.addListener((ListChangeListener<S>) change -> {
                if (currentSearchIndex >= items.size()) {
                    currentSearchIndex = -1;
                }

                refreshStatus();
                refreshTable();
            });
        }

        refreshStatus();
    }

    public void findNext() {
        String query = getCurrentQuery();

        if (query.isBlank()) {
            resetNavigation();
            setStatus(EMPTY_STATUS);
            return;
        }

        if (items == null || items.isEmpty()) {
            resetNavigation();
            setStatus(ZERO_STATUS);
            return;
        }

        SearchMode<S> mode = getSelectedMode();

        if (mode == null) {
            resetNavigation();
            setStatus(ZERO_STATUS);
            return;
        }

        int matchIndex = findNextMatchIndex(query, mode);

        if (matchIndex < 0) {
            resetNavigation();
            setStatus(ZERO_STATUS);
            return;
        }

        currentSearchIndex = matchIndex;
        selectAndScrollTo(matchIndex, mode.column());
        refreshStatus();
    }

    public void resetNavigation() {
        currentSearchIndex = -1;
    }

    public void clearSearchText() {
        resetNavigation();

        if (searchField != null) {
            searchField.clear();
        }

        setStatus(EMPTY_STATUS);
        refreshTable();
    }

    public void refreshStatus() {
        String query = getCurrentQuery();

        if (query.isBlank()) {
            setStatus(EMPTY_STATUS);
            return;
        }

        SearchMode<S> mode = getSelectedMode();

        if (mode == null || items == null || items.isEmpty()) {
            setStatus(ZERO_STATUS);
            return;
        }

        SearchStats stats = calculateStats(query, mode);

        if (stats.totalMatches() <= 0) {
            setStatus(ZERO_STATUS);
            return;
        }

        if (currentSearchIndex >= 0 && stats.selectedMatchNumber() > 0) {
            setStatus(stats.selectedMatchNumber() + "/" + stats.totalMatches());
        } else {
            setStatus("0/" + stats.totalMatches());
        }
    }

    public void refreshTable() {
        if (tableView != null) {
            tableView.refresh();
        }
    }

    public static <S> void applyReadableSelectionStyle(TableView<S> tableView) {
        if (tableView == null) {
            return;
        }

        tableView.setStyle(
                "-fx-selection-bar: #fff3b0;" +
                        "-fx-selection-bar-non-focused: #fff3b0;" +
                        "-fx-selection-bar-text: #0b2f4a;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
    }

    public static <S> void makePlainTextColumn(TableColumn<S, String> column) {
        if (column == null) {
            return;
        }

        column.setCellFactory(tableColumn -> new TableCell<>() {
            private final TextField textField = buildReadonlyTextField();

            {
                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        showTextOnly();
                        event.consume();
                    }
                });

                textField.focusedProperty().addListener((observable, oldValue, focused) -> {
                    if (!focused) {
                        showTextOnly();
                    }
                });

                setTextOverrun(OverrunStyle.ELLIPSIS);
                setWrapText(false);

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

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (getGraphic() == textField) {
                    textField.setText(item);
                    setText(null);
                    return;
                }

                showTextOnly();
            }

            private void showTextOnly() {
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                setGraphic(null);
                setText(getItem() == null ? "" : getItem());
                setTextOverrun(OverrunStyle.ELLIPSIS);
                setWrapText(false);
            }

            private void showTextField() {
                String text = getItem() == null ? "" : getItem();
                textField.setText(text);
                setText(null);
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                textField.requestFocus();
                textField.positionCaret(text.length());
            }
        });
    }

    private void installSearchableCellFactory(
            TableColumn<S, String> column,
            String modeName
    ) {
        if (column == null) {
            return;
        }

        column.setCellFactory(tableColumn -> new SearchableTableCell(modeName));
    }

    private int findNextMatchIndex(String query, SearchMode<S> mode) {
        int size = items.size();
        int startIndex = currentSearchIndex < 0 ? 0 : currentSearchIndex + 1;

        for (int offset = 0; offset < size; offset++) {
            int index = (startIndex + offset) % size;
            S item = items.get(index);

            if (matches(item, query, mode)) {
                return index;
            }
        }

        return -1;
    }

    private boolean matches(S item, String query, SearchMode<S> mode) {
        if (item == null || query == null || query.isBlank() || mode == null) {
            return false;
        }

        String text = safeText(mode.extractor().apply(item));

        if (text.isBlank()) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private SearchStats calculateStats(String query, SearchMode<S> mode) {
        int totalMatches = 0;
        int selectedMatchNumber = 0;

        for (int index = 0; index < items.size(); index++) {
            S item = items.get(index);

            if (!matches(item, query, mode)) {
                continue;
            }

            totalMatches++;

            if (index == currentSearchIndex) {
                selectedMatchNumber = totalMatches;
            }
        }

        return new SearchStats(totalMatches, selectedMatchNumber);
    }

    private void selectAndScrollTo(int rowIndex, TableColumn<S, String> targetColumn) {
        if (tableView == null || rowIndex < 0 || rowIndex >= items.size()) {
            return;
        }

        tableView.getSelectionModel().clearSelection();

        if (targetColumn != null) {
            tableView.getSelectionModel().select(rowIndex, targetColumn);
            tableView.getFocusModel().focus(rowIndex, targetColumn);
        } else {
            tableView.getSelectionModel().select(rowIndex);
            tableView.getFocusModel().focus(rowIndex);
        }

        tableView.scrollTo(rowIndex);

        Platform.runLater(() -> {
            tableView.scrollTo(rowIndex);

            if (searchField != null) {
                searchField.requestFocus();
                searchField.positionCaret(searchField.getText() == null ? 0 : searchField.getText().length());
            }
        });
    }

    private SearchMode<S> getSelectedMode() {
        if (modeComboBox == null || modeComboBox.getValue() == null) {
            if (searchModes.isEmpty()) {
                return null;
            }

            return searchModes.values().iterator().next();
        }

        SearchMode<S> mode = searchModes.get(modeComboBox.getValue());

        if (mode != null) {
            return mode;
        }

        if (searchModes.isEmpty()) {
            return null;
        }

        return searchModes.values().iterator().next();
    }

    private String getCurrentQuery() {
        if (searchField == null || searchField.getText() == null) {
            return "";
        }

        return searchField.getText().trim();
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? EMPTY_STATUS : text);
        }
    }

    private TextFlow buildHighlightedTextFlow(String text, String query, Font font) {
        TextFlow textFlow = new TextFlow();
        textFlow.setMouseTransparent(true);
        textFlow.setMinWidth(Region.USE_PREF_SIZE);
        textFlow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        textFlow.setMaxWidth(Region.USE_PREF_SIZE);

        if (text == null || text.isBlank() || query == null || query.isBlank()) {
            textFlow.getChildren().add(createNormalText(text == null ? "" : text, font));
            return textFlow;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        int queryLength = query.length();
        int currentIndex = 0;

        while (currentIndex < text.length()) {
            int matchIndex = lowerText.indexOf(lowerQuery, currentIndex);

            if (matchIndex < 0) {
                textFlow.getChildren().add(createNormalText(text.substring(currentIndex), font));
                break;
            }

            if (matchIndex > currentIndex) {
                textFlow.getChildren().add(createNormalText(text.substring(currentIndex, matchIndex), font));
            }

            int matchEnd = Math.min(matchIndex + queryLength, text.length());
            textFlow.getChildren().add(createMatchedText(text.substring(matchIndex, matchEnd), font));
            currentIndex = matchEnd;
        }

        return textFlow;
    }

    private Text createNormalText(String text, Font font) {
        Text normalText = new Text(text == null ? "" : text);
        normalText.setFont(font);
        normalText.setStyle("-fx-fill: #0b2f4a;");
        return normalText;
    }

    private Text createMatchedText(String text, Font font) {
        Text matchedText = new Text(text == null ? "" : text);
        matchedText.setFont(font);
        matchedText.setStyle(
                "-fx-fill: #082f49;" +
                        "-fx-font-weight: 900;" +
                        "-fx-underline: true;"
        );
        matchedText.setUnderline(true);
        return matchedText;
    }

    private double measureTextWidth(String text, Font font) {
        Text measure = new Text(text == null ? "" : text);
        measure.setFont(font);
        return measure.getLayoutBounds().getWidth();
    }

    private String safeText(String text) {
        if (text == null) {
            return "";
        }

        return text;
    }

    private static TextField buildReadonlyTextField() {
        TextField textField = new TextField();
        textField.setEditable(false);
        textField.setFocusTraversable(true);
        textField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-insets: 0;" +
                        "-fx-padding: 0 2 0 2;" +
                        "-fx-text-fill: #0b2f4a;"
        );
        return textField;
    }

    private final class SearchableTableCell extends TableCell<S, String> {
        private final String modeName;
        private final TextField textField = buildReadonlyTextField();

        private SearchableTableCell(String modeName) {
            this.modeName = modeName;

            setTextOverrun(OverrunStyle.ELLIPSIS);
            setWrapText(false);

            textField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    showCellText();
                    event.consume();
                }
            });

            textField.focusedProperty().addListener((observable, oldValue, focused) -> {
                if (!focused) {
                    showCellText();
                }
            });

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

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            if (getGraphic() == textField) {
                textField.setText(item);
                setText(null);
                return;
            }

            showCellText();
        }

        private void showCellText() {
            String item = getItem();

            if (item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String query = getCurrentQuery();
            boolean shouldHighlight = !query.isBlank()
                    && modeName.equals(getSelectedModeName())
                    && item.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));

            if (!shouldHighlight) {
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                setGraphic(null);
                setText(item);
                setTextOverrun(OverrunStyle.ELLIPSIS);
                setWrapText(false);
                return;
            }

            setText(null);
            setGraphic(buildSingleLineHighlightedGraphic(item, query, getFont()));
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        private void showTextField() {
            String text = getItem() == null ? "" : getItem();
            textField.setText(text);
            setText(null);
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.requestFocus();
            textField.positionCaret(text.length());
        }

        private String getSelectedModeName() {
            SearchMode<S> selectedMode = getSelectedMode();
            return selectedMode == null ? "" : selectedMode.name();
        }

        private StackPane buildSingleLineHighlightedGraphic(String text, String query, Font font) {
            TextFlow textFlow = buildHighlightedTextFlow(text, query, font);
            Label ellipsisLabel = new Label("...");
            ellipsisLabel.setMouseTransparent(true);
            ellipsisLabel.setStyle(
                    "-fx-text-fill: #0b2f4a;" +
                            "-fx-font-weight: 800;" +
                            "-fx-background-color: white;" +
                            "-fx-padding: 0 0 0 2;"
            );

            Rectangle clip = new Rectangle();
            textFlow.setClip(clip);

            StackPane pane = new StackPane(textFlow, ellipsisLabel);
            pane.setAlignment(Pos.CENTER_LEFT);
            pane.setMaxHeight(20);
            pane.setMinHeight(20);
            pane.setPrefHeight(20);
            StackPane.setAlignment(ellipsisLabel, Pos.CENTER_RIGHT);

            Runnable updateClip = () -> {
                double availableWidth = Math.max(0, getWidth() - 14);
                boolean overflow = measureTextWidth(text, font) > availableWidth;

                ellipsisLabel.setVisible(overflow);
                ellipsisLabel.setManaged(false);

                double clipWidth = overflow
                        ? Math.max(0, availableWidth - 12)
                        : availableWidth;

                clip.setWidth(clipWidth);
                clip.setHeight(20);
            };

            pane.widthProperty().addListener((observable, oldValue, newValue) -> updateClip.run());
            widthProperty().addListener((observable, oldValue, newValue) -> updateClip.run());
            Platform.runLater(updateClip);

            return pane;
        }
    }

    private record SearchMode<T>(
            String name,
            TableColumn<T, String> column,
            Function<T, String> extractor
    ) {
    }

    private record SearchStats(
            int totalMatches,
            int selectedMatchNumber
    ) {
    }
}
