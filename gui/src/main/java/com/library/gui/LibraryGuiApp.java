package com.library.gui;

import com.library.gui.net.ApiClient;
import com.library.gui.panels.BooksPanel;
import com.library.gui.panels.LoansPanel;
import com.library.gui.panels.MembersPanel;
import com.library.gui.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Desktop client for the Library Management System.
 *
 * This is a plain Java Swing application (no Spring dependency) that talks
 * to the backend purely over HTTP via {@link ApiClient}. It can be pointed
 * at any reachable instance of the backend by changing the base URL below
 * or via the "-DapiUrl=http://host:port" system property / first CLI arg.
 */
public class LibraryGuiApp extends JFrame {

    public LibraryGuiApp(String apiBaseUrl) {
        super("Library Management System");

        ApiClient api = new ApiClient(apiBaseUrl);

        BooksPanel booksPanel = new BooksPanel(api);
        MembersPanel membersPanel = new MembersPanel(api);
        LoansPanel loansPanel = new LoansPanel(api, booksPanel::refresh);

        // Card layout holds the three screens; the nav bar below switches between them.
        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.setBackground(UITheme.BACKGROUND);
        cards.add(booksPanel, "books");
        cards.add(membersPanel, "members");
        cards.add(loansPanel, "loans");

        JPanel navBar = UITheme.navTabBar(
                List.of("Books", "Members", "Loans"),
                List.of(UITheme.bookIcon(), UITheme.peopleIcon(), UITheme.loanIcon()),
                index -> {
                    String[] names = {"books", "members", "loans"};
                    cardLayout.show(cards, names[index]);
                    if (index == 0) booksPanel.refresh();
                    if (index == 1) membersPanel.refresh();
                    if (index == 2) loansPanel.refresh();
                });
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));

        JLabel statusBar = new JLabel("  Connected to: " + apiBaseUrl);
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        statusBar.setOpaque(true);
        statusBar.setBackground(UITheme.PRIMARY_DARK);
        statusBar.setForeground(Color.WHITE);
        statusBar.setFont(statusBar.getFont().deriveFont(Font.BOLD));

        JPanel top = new JPanel(new BorderLayout());
        top.add(UITheme.headerBar("Library Management System"), BorderLayout.NORTH);
        top.add(navBar, BorderLayout.SOUTH);

        getContentPane().setBackground(UITheme.BACKGROUND);
        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(750, 480));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        String apiUrl = System.getProperty("apiUrl", "http://localhost:8080");
        if (args.length > 0) {
            apiUrl = args[0];
        }
        final String finalApiUrl = apiUrl;

        // Any uncaught exception on the Event Dispatch Thread is caught here
        // so a bug in one dialog/action never silently kills the whole GUI.
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred: " + ex.getMessage(),
                    "Unexpected Error", JOptionPane.ERROR_MESSAGE);
        });

        SwingUtilities.invokeLater(() -> {
            UITheme.apply();
            new LibraryGuiApp(finalApiUrl).setVisible(true);
        });
    }
}
