package com.library.gui.theme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Arrays;

/**
 * Central place for colors, fonts, icons, and custom-painted components
 * used across the GUI. Everything here is pure Java2D / Swing — no extra
 * dependency required.
 */
public final class UITheme {

    public static final Color PRIMARY       = new Color(0x4F46E5); // indigo — neutral primary actions
    public static final Color PRIMARY_DARK  = new Color(0x3730A3);
    public static final Color ACCENT        = new Color(0x0891B2); // teal/cyan — edit actions
    public static final Color SUCCESS       = new Color(0x16A34A); // green — add / save / confirm
    public static final Color DANGER        = new Color(0xDC2626); // red — delete / cancel-destructive
    public static final Color WARNING       = new Color(0xD97706); // amber — caution actions
    public static final Color SECONDARY     = new Color(0x6B7280); // grey — neutral / cancel
    public static final Color BACKGROUND    = new Color(0xF3F4F6); // light grey app background
    public static final Color SURFACE       = Color.WHITE;
    public static final Color BORDER        = new Color(0xE5E7EB);
    public static final Color TEXT          = new Color(0x1F2937);
    public static final Color ROW_ALT       = new Color(0xF5F6FF); // light indigo stripe
    public static final Color AVAILABLE_BG  = new Color(0xDCFCE7);
    public static final Color AVAILABLE_FG  = new Color(0x15803D);
    public static final Color UNAVAILABLE_BG = new Color(0xFEE2E2);
    public static final Color UNAVAILABLE_FG = new Color(0xB91C1C);

    private static final int RADIUS = 10; // px
    private static final Font BASE_FONT   = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BOLD_FONT   = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font HINT_FONT   = new Font("Segoe UI", Font.PLAIN, 11);

    private UITheme() { }

    /** Call this once, before creating any Swing components. */
    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }

        UIManager.put("control", BACKGROUND);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", BORDER);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("text", TEXT);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", SURFACE);

        UIManager.put("defaultFont", BASE_FONT);
        UIManager.put("Table.font", BASE_FONT);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showGrid", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TableHeader.font", BOLD_FONT);
        UIManager.put("Button.font", BOLD_FONT);
        UIManager.put("Label.font", BASE_FONT);
        UIManager.put("TextField.font", BASE_FONT);
    }

    // ==================== Text fields ====================

    /** A regular text field with a soft rounded border, matching the app's visual style. */
    public static JTextField textField(String initial) {
        JTextField field = new JTextField(initial == null ? "" : initial);
        field.setFont(BASE_FONT);
        field.setBorder(fieldBorder());
        return field;
    }

    private static javax.swing.border.Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    /** Pairs a field with an inline red hint label that flashes on invalid input. */
    public static final class NumericField {
        public final JComponent component;
        public final JTextField field;
        private NumericField(JComponent component, JTextField field) {
            this.component = component;
            this.field = field;
        }
    }

    /**
     * A text field that only accepts digits. Typing a letter/symbol does
     * nothing to the text but pops up a small red "numbers only" hint under
     * the field for a moment, plus a beep, so the user understands why
     * nothing happened.
     */
    public static NumericField numericFieldWithHint(String initial) {
        JTextField field = new JTextField();
        field.setFont(BASE_FONT);
        field.setBorder(fieldBorder());

        // The hint label stays permanently in the layout (never setVisible(false))
        // with a fixed reserved height, so showing/hiding its text never changes
        // the surrounding form's size — that was what caused the fields to jump
        // bigger/smaller whenever a hint appeared.
        JLabel hint = new JLabel(" ");
        hint.setForeground(DANGER);
        hint.setFont(HINT_FONT);
        hint.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        hint.setPreferredSize(new Dimension(240, 16));
        hint.setMinimumSize(new Dimension(240, 16));
        hint.setMaximumSize(new Dimension(240, 16));

        // Kept on screen for 3.5s (was 1.4s) so the message is fully readable.
        Timer hideTimer = new Timer(3500, e -> hint.setText(" "));
        hideTimer.setRepeats(false);

        Runnable reject = () -> {
            hint.setText("Numbers only, please.");
            hideTimer.restart();
            Toolkit.getDefaultToolkit().beep();
        };

        PlainDocument doc = new PlainDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (text != null && text.matches("\\d*")) super.insertString(fb, offset, text, attr);
                else reject.run();
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null || text.matches("\\d*")) super.replace(fb, offset, length, text, attrs);
                else reject.run();
            }
        });
        field.setDocument(doc);
        try {
            doc.insertString(0, initial == null ? "" : initial.replaceAll("\\D", ""), null);
        } catch (BadLocationException ignored) { }

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(field);
        wrap.add(hint);
        return new NumericField(wrap, field);
    }

    /** Shortens a long database ID (e.g. Mongo ObjectId) for display in tables/combo boxes. */
    public static String truncateId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) + "…" : id;
    }

    // ==================== Buttons ====================

    /** A flat, rounded (10px), colored button — replaces the plain grey default. */
    public static RoundedButton button(String text, Color background) {
        return new RoundedButton(text, background);
    }

    public static class RoundedButton extends JButton {
        private final Color base;

        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(BOLD_FONT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = base;
            if (!isEnabled()) fill = SECONDARY;
            else if (getModel().isPressed()) fill = shade(base, -0.15f);
            else if (getModel().isRollover()) fill = shade(base, 0.12f);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2));
            g2.dispose();
            super.paintComponent(g);
        }

        private static Color shade(Color c, float amt) {
            int r = clamp((int) (c.getRed()   + (amt > 0 ? (255 - c.getRed())   * amt : c.getRed()   * amt)));
            int g = clamp((int) (c.getGreen() + (amt > 0 ? (255 - c.getGreen()) * amt : c.getGreen() * amt)));
            int b = clamp((int) (c.getBlue()  + (amt > 0 ? (255 - c.getBlue())  * amt : c.getBlue()  * amt)));
            return new Color(r, g, b);
        }

        private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    }

    // ==================== Nav tabs (Books / Members / Loans) ====================

    public static class NavTabButton extends JToggleButton {
        NavTabButton(String text, Icon icon) {
            super(text, icon);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(BOLD_FONT);
            setIconTextGap(8);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setForeground(SECONDARY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                setForeground(Color.WHITE);
                g2.setColor(PRIMARY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2));
            } else {
                setForeground(getModel().isRollover() ? PRIMARY : SECONDARY);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 25));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2));
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JPanel navTabBar(java.util.List<String> labels, java.util.List<Icon> icons,
                                    java.util.function.IntConsumer onSelect) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(SURFACE);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < labels.size(); i++) {
            NavTabButton tab = new NavTabButton(labels.get(i), icons.get(i));
            int idx = i;
            tab.addActionListener(e -> onSelect.accept(idx));
            if (i == 0) tab.setSelected(true);
            group.add(tab);
            bar.add(tab);
        }
        return bar;
    }

    // ==================== Icons ====================

    public static Icon bookIcon() { return new BookIcon(); }
    public static Icon peopleIcon() { return new PeopleIcon(); }
    public static Icon loanIcon() { return new LoanIcon(); }
    public static Icon searchIcon() { return new SearchIcon(); }

    private static class BookIcon implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(x + 1, y + 2, 15, 13, 3, 3);
            g2.drawLine(x + 9, y + 2, x + 9, y + 15);
            g2.drawLine(x + 3, y + 5, x + 7, y + 5);
            g2.drawLine(x + 11, y + 5, x + 15, y + 5);
            g2.dispose();
        }
    }

    private static class PeopleIcon implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.fillOval(x + 5, y + 1, 8, 8);
            g2.fillArc(x + 1, y + 9, 16, 12, 0, 180);
            g2.dispose();
        }
    }

    private static class LoanIcon implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawArc(x + 2, y + 2, 13, 13, 30, 260);
            g2.drawArc(x + 3, y + 3, 13, 13, 210, 260);
            int[] px = {x + 13, x + 16, x + 12}, py = {y + 2, y + 5, y + 6};
            g2.fillPolygon(px, py, 3);
            int[] qx = {x + 5, x + 2, x + 6}, qy = {y + 16, y + 13, y + 12};
            g2.fillPolygon(qx, qy, 3);
            g2.dispose();
        }
    }

    private static class SearchIcon implements Icon {
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SECONDARY);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawOval(x + 1, y + 1, 9, 9);
            g2.drawLine(x + 9, y + 9, x + 14, y + 14);
            g2.dispose();
        }
    }

    // ==================== Rounded containers / fields ====================

    public static class RoundedPanel extends JPanel {
        public RoundedPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(SURFACE);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2));
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, RADIUS * 2, RADIUS * 2));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 10, 2));
        return label;
    }

    public static JTextField roundedSearchField(int columns) {
        JTextField field = new JTextField(columns);
        field.setOpaque(false);
        field.setFont(BASE_FONT);
        field.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 8));
        return field;
    }

    public static JComponent searchBar(JTextField field, int height) {
        JPanel bar = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), height, height));
                g2.setColor(BORDER);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, height, height));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 10));
        bar.setPreferredSize(new Dimension(230, height));
        JLabel icon = new JLabel(searchIcon());
        bar.add(icon, BorderLayout.WEST);
        bar.add(field, BorderLayout.CENTER);
        return bar;
    }

    // ==================== Table styling ====================

    public static DefaultTableCellRenderer stripedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? SURFACE : ROW_ALT);
                    c.setForeground(TEXT);
                } else {
                    c.setBackground(PRIMARY);
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        };
    }

    /**
     * Renderer for a numeric "available copies"-style column: shown as a
     * soft colored pill (green if greater than zero, red if zero) so it's
     * easy to scan at a glance without being a loud, distracting highlight.
     */
    public static DefaultTableCellRenderer availabilityRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(BOLD_FONT);
                label.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
                int qty = 0;
                try { qty = Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { }
                if (isSelected) {
                    label.setBackground(PRIMARY);
                    label.setForeground(Color.WHITE);
                } else if (qty > 0) {
                    label.setBackground(AVAILABLE_BG);
                    label.setForeground(AVAILABLE_FG);
                } else {
                    label.setBackground(UNAVAILABLE_BG);
                    label.setForeground(UNAVAILABLE_FG);
                }
                return label;
            }
        };
    }

    public static void styleTable(JTable table) {
        table.setDefaultRenderer(Object.class, stripedRenderer());
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setBackground(SURFACE);

        // Nimbus paints JTableHeader itself and ignores plain setBackground/
        // setForeground on the header component, so we render header cells
        // ourselves with a custom renderer instead.
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setBackground(PRIMARY);
                label.setForeground(Color.WHITE);
                label.setFont(BOLD_FONT);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return label;
            }
        });
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().resizeAndRepaint();
    }

    // ==================== Background blur (used behind every dialog) ====================

    private static Component applyBlur(Component parent) {
        try {
            JRootPane rootPane = SwingUtilities.getRootPane(parent);
            if (rootPane == null || rootPane.getWidth() <= 0 || rootPane.getHeight() <= 0) return null;
            BufferedImage snapshot = new BufferedImage(rootPane.getWidth(), rootPane.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = snapshot.createGraphics();
            rootPane.printAll(g2);
            g2.dispose();
            BufferedImage blurred = boxBlur(snapshot, 5);
            BlurGlassPane glass = new BlurGlassPane(blurred);
            rootPane.setGlassPane(glass);
            glass.setVisible(true);
            return glass;
        } catch (Exception e) {
            return null;
        }
    }

    private static void removeBlur(Component glass) {
        if (glass != null) glass.setVisible(false);
    }

    private static BufferedImage boxBlur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];
        Arrays.fill(data, weight);
        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        op.filter(src, dst);
        return dst;
    }

    private static class BlurGlassPane extends JComponent {
        private final BufferedImage image;
        BlurGlassPane(BufferedImage image) {
            this.image = image;
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            if (image != null) g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            g.setColor(new Color(15, 15, 25, 90));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ==================== Dialogs (blurred background + colored heading) ====================

    private static JPanel dialogWrapper(String title, Color accent, JComponent body) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setBackground(SURFACE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JLabel heading = new JLabel(title);
        heading.setFont(TITLE_FONT);
        heading.setForeground(accent);
        JPanel headingPanel = new JPanel(new BorderLayout());
        headingPanel.setOpaque(false);
        headingPanel.add(heading, BorderLayout.WEST);
        headingPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, accent));
        wrapper.add(headingPanel, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    /** Yes/No confirmation with a colored heading, colored action button, and a blurred backdrop. */
    public static boolean confirm(Component parent, String message, String title,
                                   String confirmText, Color confirmColor) {
        JLabel msgLabel = new JLabel("<html><body style='width:260px'>" + message + "</body></html>");
        msgLabel.setFont(BASE_FONT);
        JPanel wrapper = dialogWrapper(title, confirmColor, msgLabel);

        RoundedButton confirmBtn = button(confirmText, confirmColor);
        RoundedButton cancelBtn = button("Cancel", SECONDARY);
        JOptionPane pane = new JOptionPane(wrapper, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{confirmBtn, cancelBtn});
        JDialog dialog = pane.createDialog(parent, title);

        Component blur = applyBlur(parent);
        final boolean[] result = {false};
        confirmBtn.addActionListener(e -> { result[0] = true; dialog.dispose(); });
        cancelBtn.addActionListener(e -> { result[0] = false; dialog.dispose(); });
        dialog.setVisible(true);
        removeBlur(blur);
        return result[0];
    }

    /** Shows a form (Add/Edit dialogs) with a colored heading, green Save + grey Cancel, and blurred backdrop. */
    public static boolean showForm(Component parent, JComponent form, String title, String saveLabel) {
        return showForm(parent, form, title, saveLabel, null);
    }

    /**
     * Same as {@link #showForm(Component, JComponent, String, String)}, but takes a
     * validator that runs when Save is clicked. If it returns a non-null message,
     * the dialog stays open and shows that message inline (in English) instead of
     * closing — so the user can fix the field(s) without reopening the form.
     * <p>
     * The error area has a fixed reserved height at all times (visible or not), so
     * the popup and its fields never resize/jump when the message appears or
     * disappears. The message stays visible for 3.5 seconds, long enough to read
     * in full, and the popup is wide enough for it to display without truncation.
     */
    public static boolean showForm(Component parent, JComponent form, String title, String saveLabel,
                                    java.util.function.Supplier<String> validator) {
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(DANGER);
        errorLabel.setFont(HINT_FONT);
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        // Fixed size reserved up front so toggling the text never changes the
        // dialog's layout size.
        errorLabel.setPreferredSize(new Dimension(340, 36));
        errorLabel.setMinimumSize(new Dimension(340, 36));
        errorLabel.setMaximumSize(new Dimension(340, 36));

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(form, BorderLayout.CENTER);
        body.add(errorLabel, BorderLayout.SOUTH);
        // Wide enough that a full validation sentence never gets clipped.
        body.setPreferredSize(new Dimension(Math.max(360, body.getPreferredSize().width), body.getPreferredSize().height));

        JPanel wrapper = dialogWrapper(title, PRIMARY, body);

        RoundedButton saveBtn = button(saveLabel, SUCCESS);
        RoundedButton cancelBtn = button("Cancel", SECONDARY);
        JOptionPane pane = new JOptionPane(wrapper, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{saveBtn, cancelBtn});
        JDialog dialog = pane.createDialog(parent, title);

        Component blur = applyBlur(parent);
        final boolean[] result = {false};

        Timer hideTimer = new Timer(3500, e -> errorLabel.setText(" "));
        hideTimer.setRepeats(false);

        saveBtn.addActionListener(e -> {
            String error = validator == null ? null : validator.get();
            if (error != null) {
                errorLabel.setText("<html><body style='width:320px'>" + error + "</body></html>");
                hideTimer.restart();
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            result[0] = true;
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> { result[0] = false; dialog.dispose(); });
        dialog.setVisible(true);
        removeBlur(blur);
        return result[0];
    }

    /** A single-button info/success/warning/error message with a colored heading and blurred backdrop. */
    public static void showMessage(Component parent, String title, String message, Color accent) {
        JLabel msgLabel = new JLabel("<html><body style='width:260px'>" + message + "</body></html>");
        msgLabel.setFont(BASE_FONT);
        JPanel wrapper = dialogWrapper(title, accent, msgLabel);

        RoundedButton okBtn = button("OK", accent);
        JOptionPane pane = new JOptionPane(wrapper, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{okBtn});
        JDialog dialog = pane.createDialog(parent, title);

        Component blur = applyBlur(parent);
        okBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
        removeBlur(blur);
    }

    public static void showSuccess(Component parent, String title, String message) { showMessage(parent, title, message, SUCCESS); }
    public static void showInfo(Component parent, String title, String message) { showMessage(parent, title, message, PRIMARY); }
    public static void showWarning(Component parent, String title, String message) { showMessage(parent, title, message, WARNING); }
    public static void showError(Component parent, String title, String message) { showMessage(parent, title, message, DANGER); }

    /** A colored header bar with a title, used at the top of the main window. */
    public static JComponent headerBar(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PRIMARY);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel label = new JLabel(title);
        label.setFont(HEADER_FONT);
        label.setForeground(Color.WHITE);
        bar.add(label, BorderLayout.WEST);
        return bar;
    }
}
