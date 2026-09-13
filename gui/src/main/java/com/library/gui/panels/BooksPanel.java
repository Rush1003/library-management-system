package com.library.gui.panels;

import com.library.gui.model.BookDto;
import com.library.gui.net.ApiClient;
import com.library.gui.net.ApiException;
import com.library.gui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab for viewing and managing Books. All network calls run on a
 * SwingWorker background thread so the UI never freezes, and every failure
 * is caught and shown to the user via a styled dialog instead of crashing
 * the application.
 */
public class BooksPanel extends JPanel {

    private final ApiClient api;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField searchField = UITheme.roundedSearchField(18);

    // Real database IDs, kept in the same order as the visible table rows.
    // The table itself only shows a friendly "#" serial number (1, 2, 3...).
    private final List<String> rowIds = new ArrayList<>();

    private static final String[] COLUMNS = {
            "#", "ISBN", "Title", "Author", "Genre", "Year", "Total Copies", "Available"
    };
    private static final int COL_AVAILABLE = 7;

    public BooksPanel(ApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UITheme.BACKGROUND);

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        // Available copies get their own soft green/red pill renderer so they're
        // easy to scan without being a loud, distracting highlight.
        table.getColumnModel().getColumn(COL_AVAILABLE).setCellRenderer(UITheme.availabilityRenderer());

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(new BorderLayout(0, 10));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(UITheme.sectionTitle("All Books"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(buildTopBar(), BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildTopBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setBackground(UITheme.BACKGROUND);

        JLabel label = new JLabel("Search by title");
        label.setForeground(UITheme.SECONDARY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(label);
        panel.add(UITheme.searchBar(searchField, 36));

        JButton searchBtn = UITheme.button("Search", UITheme.PRIMARY);
        JButton clearBtn = UITheme.button("Clear", UITheme.SECONDARY);
        searchBtn.addActionListener(e -> search());
        clearBtn.addActionListener(e -> { searchField.setText(""); refresh(); });
        panel.add(searchBtn);
        panel.add(clearBtn);
        return panel;
    }

    private JComponent buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(UITheme.BACKGROUND);

        JButton refreshBtn = UITheme.button("Refresh", UITheme.PRIMARY);
        JButton addBtn = UITheme.button("Add Book", UITheme.SUCCESS);
        JButton editBtn = UITheme.button("Edit Selected", UITheme.ACCENT);
        JButton deleteBtn = UITheme.button("Delete Selected", UITheme.DANGER);

        refreshBtn.addActionListener(e -> refresh());
        addBtn.addActionListener(e -> openBookDialog(null));
        editBtn.addActionListener(e -> {
            BookDto selected = getSelectedBook();
            if (selected == null) {
                UITheme.showWarning(this, "No Selection", "Select a book first.");
                return;
            }
            openBookDialog(selected);
        });
        deleteBtn.addActionListener(e -> deleteSelected());

        panel.add(refreshBtn);
        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        return panel;
    }

    public void refresh() {
        runAsync(api::getAllBooks, this::populateTable, "Failed to load books");
    }

    private void search() {
        String title = searchField.getText().trim();
        if (title.isEmpty()) {
            refresh();
            return;
        }
        runAsync(() -> api.searchBooks("title", title), this::populateTable, "Search failed");
    }

    private void populateTable(List<BookDto> books) {
        model.setRowCount(0);
        rowIds.clear();
        int serial = 1;
        for (BookDto b : books) {
            model.addRow(new Object[]{
                    serial++, b.isbn, b.title, b.author, b.genre, b.publishedYear, b.totalCopies, b.availableCopies
            });
            rowIds.add(b.id);
        }
    }

    private BookDto getSelectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        BookDto b = new BookDto();
        b.id = rowIds.get(row);
        b.isbn = (String) model.getValueAt(row, 1);
        b.title = (String) model.getValueAt(row, 2);
        b.author = (String) model.getValueAt(row, 3);
        b.genre = (String) model.getValueAt(row, 4);
        b.publishedYear = (int) model.getValueAt(row, 5);
        b.totalCopies = (int) model.getValueAt(row, 6);
        b.availableCopies = (int) model.getValueAt(row, COL_AVAILABLE);
        return b;
    }

    private void deleteSelected() {
        BookDto selected = getSelectedBook();
        if (selected == null) {
            UITheme.showWarning(this, "No Selection", "Select a book first.");
            return;
        }
        boolean confirmed = UITheme.confirm(this, "Delete '" + selected.title + "'?", "Confirm Delete",
                "Delete", UITheme.DANGER);
        if (!confirmed) return;

        runAsyncVoid(() -> { api.deleteBook(selected.id); return null; }, v -> {
            refresh();
            UITheme.showSuccess(this, "Deleted", "'" + selected.title + "' was deleted successfully.");
        }, "Failed to delete book");
    }

    private void openBookDialog(BookDto existing) {
        JTextField isbn = UITheme.textField(existing != null ? existing.isbn : "");
        JTextField title = UITheme.textField(existing != null ? existing.title : "");
        JTextField author = UITheme.textField(existing != null ? existing.author : "");
        JTextField genre = UITheme.textField(existing != null ? existing.genre : "");
        UITheme.NumericField year = UITheme.numericFieldWithHint(existing != null ? String.valueOf(existing.publishedYear) : "");
        UITheme.NumericField total = UITheme.numericFieldWithHint(existing != null ? String.valueOf(existing.totalCopies) : "");
        UITheme.NumericField available = UITheme.numericFieldWithHint(existing != null ? String.valueOf(existing.availableCopies) : "");

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setOpaque(false);
        form.add(formLabel("ISBN:")); form.add(isbn);
        form.add(formLabel("Title:")); form.add(title);
        form.add(formLabel("Author:")); form.add(author);
        form.add(formLabel("Genre:")); form.add(genre);
        form.add(formLabel("Published Year:")); form.add(year.component);
        form.add(formLabel("Total Copies:")); form.add(total.component);
        form.add(formLabel("Available Copies:")); form.add(available.component);

        String dialogTitle = existing == null ? "Add Book" : "Edit Book";
        boolean saved = UITheme.showForm(this, form, dialogTitle, existing == null ? "Add" : "Save", () -> {
            if (year.field.getText().isBlank() || total.field.getText().isBlank() || available.field.getText().isBlank()) {
                return "Year, Total Copies and Available Copies are required.";
            }
            int totalVal = Integer.parseInt(total.field.getText().trim());
            int availableVal = Integer.parseInt(available.field.getText().trim());
            if (availableVal > totalVal) {
                return "Available copies cannot be more than total copies (" + totalVal + ").";
            }
            return null;
        });
        if (!saved) return;

        BookDto dto = new BookDto();
        dto.isbn = isbn.getText().trim();
        dto.title = title.getText().trim();
        dto.author = author.getText().trim();
        dto.genre = genre.getText().trim();
        dto.publishedYear = Integer.parseInt(year.field.getText().trim());
        dto.totalCopies = Integer.parseInt(total.field.getText().trim());
        dto.availableCopies = Integer.parseInt(available.field.getText().trim());

        if (existing == null) {
            runAsyncVoid(() -> { api.createBook(dto); return null; }, v -> {
                refresh();
                UITheme.showSuccess(this, "Book Added", "'" + dto.title + "' was added successfully.");
            }, "Failed to create book");
        } else {
            runAsyncVoid(() -> { api.updateBook(existing.id, dto); return null; }, v -> {
                refresh();
                UITheme.showSuccess(this, "Book Updated", "'" + dto.title + "' was updated successfully.");
            }, "Failed to update book");
        }
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT);
        return label;
    }

    // ---------------- Generic async helpers with unified error handling ----------------

    private interface ApiCall<T> { T call() throws ApiException; }

    private <T> void runAsync(ApiCall<T> call, java.util.function.Consumer<T> onSuccess, String errorTitle) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            ApiException error;
            @Override protected T doInBackground() {
                try {
                    return call.call();
                } catch (ApiException e) {
                    error = e;
                    return null;
                }
            }
            @Override protected void done() {
                if (error != null) {
                    showError(errorTitle, error);
                } else {
                    try { onSuccess.accept(get()); } catch (Exception ignored) { }
                }
            }
        };
        worker.execute();
    }

    private <T> void runAsyncVoid(ApiCall<T> call, java.util.function.Consumer<T> onSuccess, String errorTitle) {
        runAsync(call, onSuccess, errorTitle);
    }

    private void showError(String title, ApiException e) {
        String suffix = e.isConnectivityFailure() ? " (Is the backend running on the configured URL?)" : "";
        UITheme.showError(this, title, e.getMessage() + suffix);
    }
}
