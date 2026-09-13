package com.library.gui.panels;

import com.library.gui.model.MemberDto;
import com.library.gui.net.ApiClient;
import com.library.gui.net.ApiException;
import com.library.gui.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MembersPanel extends JPanel {

    private final ApiClient api;
    private final DefaultTableModel model;
    private final JTable table;

    private final List<String> rowIds = new ArrayList<>();

    private static final String[] COLUMNS = { "#", "Name", "Email", "Phone", "Membership Date", "Active" };

    public MembersPanel(ApiClient api) {
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

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(new BorderLayout(0, 10));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(UITheme.sectionTitle("All Members"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(UITheme.BACKGROUND);

        JButton refreshBtn = UITheme.button("Refresh", UITheme.PRIMARY);
        JButton addBtn = UITheme.button("Add Member", UITheme.SUCCESS);
        JButton editBtn = UITheme.button("Edit Selected", UITheme.ACCENT);
        JButton deleteBtn = UITheme.button("Delete Selected", UITheme.DANGER);

        refreshBtn.addActionListener(e -> refresh());
        addBtn.addActionListener(e -> openMemberDialog(null));
        editBtn.addActionListener(e -> {
            MemberDto selected = getSelectedMember();
            if (selected == null) {
                UITheme.showWarning(this, "No Selection", "Select a member first.");
                return;
            }
            openMemberDialog(selected);
        });
        deleteBtn.addActionListener(e -> deleteSelected());

        panel.add(refreshBtn);
        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        return panel;
    }

    public void refresh() {
        runAsync(api::getAllMembers, this::populateTable, "Failed to load members");
    }

    private void populateTable(List<MemberDto> members) {
        model.setRowCount(0);
        rowIds.clear();
        int serial = 1;
        for (MemberDto m : members) {
            model.addRow(new Object[]{ serial++, m.name, m.email, m.phone, m.membershipDate, m.active });
            rowIds.add(m.id);
        }
    }

    private MemberDto getSelectedMember() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        MemberDto m = new MemberDto();
        m.id = rowIds.get(row);
        m.name = (String) model.getValueAt(row, 1);
        m.email = (String) model.getValueAt(row, 2);
        m.phone = (String) model.getValueAt(row, 3);
        Object date = model.getValueAt(row, 4);
        m.membershipDate = date instanceof LocalDate ? (LocalDate) date : null;
        m.active = (boolean) model.getValueAt(row, 5);
        return m;
    }

    private void deleteSelected() {
        MemberDto selected = getSelectedMember();
        if (selected == null) {
            UITheme.showWarning(this, "No Selection", "Select a member first.");
            return;
        }
        boolean confirmed = UITheme.confirm(this, "Delete member '" + selected.name + "'?", "Confirm Delete",
                "Delete", UITheme.DANGER);
        if (!confirmed) return;

        runAsync(() -> { api.deleteMember(selected.id); return null; }, v -> {
            refresh();
            UITheme.showSuccess(this, "Deleted", "'" + selected.name + "' was deleted successfully.");
        }, "Failed to delete member");
    }

    private void openMemberDialog(MemberDto existing) {
        JTextField name = UITheme.textField(existing != null ? existing.name : "");
        JTextField email = UITheme.textField(existing != null ? existing.email : "");
        UITheme.NumericField phone = UITheme.numericFieldWithHint(existing != null ? existing.phone : "");
        JCheckBox active = new JCheckBox("Active", existing == null || existing.active);
        active.setOpaque(false);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setOpaque(false);
        form.add(formLabel("Name:")); form.add(name);
        form.add(formLabel("Email:")); form.add(email);
        form.add(formLabel("Phone:")); form.add(phone.component);
        form.add(formLabel("Status:")); form.add(active);

        String dialogTitle = existing == null ? "Add Member" : "Edit Member";
        boolean saved = UITheme.showForm(this, form, dialogTitle, existing == null ? "Add" : "Save");
        if (!saved) return;

        MemberDto dto = new MemberDto();
        dto.name = name.getText().trim();
        dto.email = email.getText().trim();
        dto.phone = phone.field.getText().trim();
        dto.active = active.isSelected();

        if (existing == null) {
            runAsync(() -> api.createMember(dto), v -> {
                refresh();
                UITheme.showSuccess(this, "Member Added", "'" + dto.name + "' was added successfully.");
            }, "Failed to create member");
        } else {
            runAsync(() -> api.updateMember(existing.id, dto), v -> {
                refresh();
                UITheme.showSuccess(this, "Member Updated", "'" + dto.name + "' was updated successfully.");
            }, "Failed to update member");
        }
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UITheme.TEXT);
        return label;
    }

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
                    String suffix = error.isConnectivityFailure() ? " (Is the backend running on the configured URL?)" : "";
                    UITheme.showError(MembersPanel.this, errorTitle, error.getMessage() + suffix);
                } else {
                    try { onSuccess.accept(get()); } catch (Exception ignored) { }
                }
            }
        };
        worker.execute();
    }
}
