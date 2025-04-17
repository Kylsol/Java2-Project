package com.bushnell;

// Import necessary libraries for GUI and image handling
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.net.URL;

public class MRPSystemUI extends JFrame {
    // Constants for frame and component dimensions
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;
    private static final int LOGO_WIDTH = 180;
    private static final int LOGO_HEIGHT = 51;
    private static final int LOGO_X = 10;
    private static final int LOGO_Y = 10;
    private static final int TITLE_X = 10;
    private static final int TITLE_Y = 70;
    private static final int TITLE_WIDTH = 200;
    private static final int TITLE_HEIGHT = 30;
    private static final int BUTTON_START_Y = 110;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_SPACING = 50;
    private static final int BUTTON_FONT_SIZE = 14;
    private static final int TITLE_FONT_SIZE = 20;
    private static final int CARD_FONT_SIZE = 24;
    private static final int CONTENT_X = 200;
    private static final int CONTENT_Y = 10;
    private static final int CONTENT_WIDTH = 1060;
    private static final int CONTENT_HEIGHT = 660;

    // CardLayout is used for switching between different panels
    private CardLayout cardLayout;
    private JPanel cardPanel; // Panel that holds all feature panels
    private StockReport stockReportPanel; // Reference to StockReport panel
    private BundlePanel bundlePanel; // Reference to Bundle panel

    public MRPSystemUI() {
        // Configure the main JFrame
        setTitle("MRP System");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setLayout(new BorderLayout());

        // Try to set application icon from resources
        URL iconURL = getClass().getClassLoader().getResource("VisualRoboticsIcon.png");
        if (iconURL != null) {
            setIconImage(new ImageIcon(iconURL).getImage());
        } else {
            System.out.println("Icon not found!");
        }
        

        // Create main panel and configure layout and background
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setLayout(null); // Absolute positioning
        add(mainPanel, BorderLayout.CENTER);

        // Load and add the company logo image to the top left
        JLabel logoLabel = new JLabel();
        try {
            BufferedImage logoImage = ImageIO.read(getClass().getClassLoader().getResource("VisualRoboticsLogo.png"));
            if (logoImage != null) {
                Image scaledLogo = logoImage.getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logoLabel.setBounds(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
        mainPanel.add(logoLabel); // Add logo to panel

        // Create and add the title label below the logo
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE); // White text for contrast
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);
        mainPanel.add(titleLabel);

        // Define button labels for navigation
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066"); // Brand green color
        int yOffset = BUTTON_START_Y; // Initial Y-position for the first button

        // Create sidebar buttons and add them to the main panel
        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setBackground(vrGreen); // Set background color
            btn.setForeground(Color.WHITE); // Set text color
            btn.setOpaque(true); // Make background color visible
            btn.setBorderPainted(false); // Remove button border
            btn.setFocusPainted(false); // Remove focus border on click
            btn.setFont(new Font("Arial", Font.BOLD, BUTTON_FONT_SIZE));
            btn.setBounds(LOGO_X, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT); // Set button position and size
            btn.setActionCommand(text); // Use button text as action command
            btn.addActionListener(e -> showCard(e.getActionCommand())); // Show associated card on click
            mainPanel.add(btn); // Add button to panel
            yOffset += BUTTON_SPACING; // Move down for next button
        }

        // Initialize the card layout and card panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        mainPanel.add(cardPanel); // Add card panel to main panel

        // Add each feature panel to the card layout
        for (String name : buttons) {
            if ("Update Stock".equals(name)) {
                cardPanel.add(new UpdateStockPanel(), name);
            } else if ("Stock Report".equals(name)) {
                stockReportPanel = new StockReport();
                cardPanel.add(stockReportPanel, name);
            } else if ("Bundle".equals(name)) {
                bundlePanel = new BundlePanel();
                cardPanel.add(bundlePanel, name);
            } else {
                // For placeholder panels like Demand Analysis
                JPanel card = new JPanel();
                card.setBackground(Color.WHITE);
                JLabel label = new JLabel(name);
                label.setFont(new Font("Arial", Font.BOLD, CARD_FONT_SIZE));
                card.add(label);
                cardPanel.add(card, name);
            }
        }

        // Show the first card by default (Update Stock)
        cardLayout.show(cardPanel, buttons[0]);
    }

    // Method to handle switching between feature panels
    private void showCard(String name) {
        // If Stock Report is selected, refresh its content
        if ("Stock Report".equals(name) && stockReportPanel != null) {
            stockReportPanel.updateReport();
        }
        // If Bundle is selected, reset the selection form
        if ("Bundle".equals(name) && bundlePanel != null) {
            bundlePanel.resetSelection();
        }
        // Show the selected panel using CardLayout
        cardLayout.show(cardPanel, name);
    }

    // Main method to launch the GUI application
    public static void main(String[] args) {
        // Ensure UI runs on the Swing event-dispatching thread
        SwingUtilities.invokeLater(() -> {
            MRPSystemUI ui = new MRPSystemUI();
            ui.setVisible(true); // Show the main window
        });
    }
}
