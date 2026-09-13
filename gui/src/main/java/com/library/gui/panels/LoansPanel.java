package com.library.gui.panels;

import com.library.gui.model.BookDto;
import com.library.gui.model.LoanDto;
import com.library.gui.model.MemberDto;
import com.library.gui.net.ApiClient;
import com.library.gui.net.ApiException;
import com.library.gui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tab for issuing and returning books. Also refreshes the Books panel after
 * a transaction so the "Available" copy counts stay in sync across tabs.
 */
public class LoansPanel extends JPanel {

    private final ApiClient api;
    private final DefaultTableModel model;
    private final JTable table;
    private final Runnable onBooksChanged; // callback to refresh BooksPanel after issue/return

    private final List<String> rowIds = new ArrayList<>();

    private static final String[] COLUMNS = {
            "#", "Book ID", "Member ID", "Issue Date", "Due Date", "Return Date", "Status", "Fine"
    };

    public LoansPanel(ApiClient api, Runnable onBooksChanged) {
        this.api = api;
        this.onBooksChanged = onBooksChanged;
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

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(new BorderLayout(0, 10));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(UITheme.sectionTitle("All Loans"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(UITheme.BACKGROUND);

        JButton refreshBtn = UITheme.button("Refresh", UITheme.PRIMARY);
        JButton issueBtn = UITheme.button("Issue Book", UITheme.SUCCESS);
        JButton returnBtn = UITheme.button("Return Selected", UITheme.ACCENT);
        JButton overdueBtn = UITheme.button("Show Overdue", UITheme.WARNING);

        refreshBtn.addActionListener(e -> refresh());
        issueBtn.addActionListener(e -> openIssueDialog());
        returnBtn.addActionListener(e -> returnSelected());
        overdueBtn.addActionListener(e -> showOverdue());

        panel.add(refreshBtn);
        panel.add(issueBtn);
        panel.add(returnBtn);
        panel.add(overdueBtn);
        return panel;
    }

    public void refresh() {
        runAsync(api::getAllLoans, this::populateTable, "Failed to load loans");
    }

    private void showOverdue() {
        runAsync(() -> api.getAllLoans().stream()
                        .filter(l -> "ISSUED".equals(l.status) && l.dueDate != null && l.dueDate.isBefore(java.time.LocalDate.now()))
                        .toList(),
                this::populateTable, "Failed to load overdue loans");
    }

    private void populateTable(List<LoanDto> loans) {
        model.setRowCount(0);
        rowIds.clear();
        int serial = 1;
        for (LoanDto l : loans) {
            model.addRow(new Object[]{
                    serial++, UITheme.truncateId(l.bookId), UITheme.truncateId(l.memberId),
                    l.issueDate, l.dueDate, l.returnDate, l.status,
                    String.format("%.2f", l.fineAmount)
            });
            rowIds.add(l.id);
        }
    }

    private void openIssueDialog() {
        SwingWorker<Object[], Void> loader = new SwingWorker<>() {
            ApiException error;
            List<BookDto> books;
            List<MemberDto> members;

            @Override protected Object[] doInBackground() {
                try {
                    books = api.getAllBooks();
                    members = api.getAllMembers();
                } catch (ApiException e) {
                    error = e;
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    showError("Failed to load books/members", error);
                    return;
                }
                if (books.isEmpty() || members.isEmpty()) {
                    UITheme.showWarning(LoansPanel.this, "Nothing to Issue",
                            "You need at least one book and one member before issuing a loan.");
                    return;
                }
                JComboBox<BookDto> bookBox = new JComboBox<>(books.toArray(new BookDto[0]));
                JComboBox<MemberDto> memberBox = new JComboBox<>(members.toArray(new MemberDto[0]));

                JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
                form.setOpaque(false);
                form.add(formLabel("Book:")); form.add(bookBox);
                form.add(formLabel("Member:")); form.add(memberBox);

                boolean confirmed = UITheme.showForm(LoansPanel.this, form, "Issue Book", "Issue");
                if (!confirmed) return;

                BookDto book = (BookDto) bookBox.getSelectedItem();
                MemberDto member = (MemberDto) memberBox.getSelectedItem();
                runAsync(() -> api.issueBook(book.id, member.id), v -> {
                    refresh();
                    onBooksChanged.run();
                    UITheme.showSuccess(LoansPanel.this, "Book Issued",
                            "'" + book.title + "' was issued to " + member.name + ".");
                }, "Failed to issue book");
            }
        };
        loader.execute();
    }

    private void returnSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UITheme.showWarning(this, "No Selection", "Select a loan first.");
            return;
        }
        String loanId = rowIds.get(row);
        String status = (String) model.getValueAt(row, 6);
        if ("RETURNED".equals(status)) {
            UITheme.showInfo(this, "Already Returned", "This loan has already been returned.");
            return;
        }
        runAsync(() -> api.returnBook(loanId), (LoanDto loan) -> {
            refresh();
            onBooksChanged.run();
            if (loan.fineAmount > 0) {
                UITheme.showWarning(this, "Returned With Fine",
                        String.format("Book returned late. Fine due: $%.2f", loan.fineAmount));
            } else {
                UITheme.showSuccess(this, "Return Successful", "The book has been returned successfully.");
            }
        }, "Failed to return book");
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT);
        return label;
    }

    private interface ApiCall<T> { T call() throws ApiException; }

    private <T> void runAsync(ApiCall<T> call, Consumer<T> onSuccess, String errorTitle) {
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

    private void showError(String title, ApiException e) {
        String suffix = e.isConnectivityFailure() ? " (Is the backend running on the configured URL?)" : "";
        UITheme.showError(this, title, e.getMessage() + suffix);
    }
}
