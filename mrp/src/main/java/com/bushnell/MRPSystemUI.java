package com.bushnell;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Main GUI class for the MRP (Material Requirements Planning) System.
 * 
 * This class builds a graphical interface using Java Swing. The layout consists of:
 * - A header with the company logo and system title.
 * - A vertical navigation menu with buttons.
 * - A main content area that switches views based on which button is pressed.
 *
 * Swing is chosen for its ease of deployment and compatibility with Java desktop apps.
 */
public class MRPSystemUI extends JFrame {

    // Dimensions for the main application window (1280x720 pixels)
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;

    // Size and position of the logo image (top-left corner)
    private static final int LOGO_WIDTH = 180;
    private static final int LOGO_HEIGHT = 51;
    private static final int LOGO_X = 10;
    private static final int LOGO_Y = 10;

    // Title label (text below the logo)
    private static final int TITLE_X = 10;
    private static final int TITLE_Y = 70;
    private static final int TITLE_WIDTH = 200;
    private static final int TITLE_HEIGHT = 30;

    // Button layout (vertical list on the left-hand side)
    private static final int BUTTON_START_Y = 110; // Starting Y position of first button
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_SPACING = 50; // Space between buttons
    private static final int BUTTON_FONT_SIZE = 14;

    // Font size for the system title
    private static final int TITLE_FONT_SIZE = 20;

    // Font size for labels inside each card panel
    private static final int CARD_FONT_SIZE = 24;

    // Size and position of the dynamic content area (right side of the window)
    private static final int CONTENT_X = 200;
    private static final int CONTENT_Y = 10;
    private static final int CONTENT_WIDTH = 1060;
    private static final int CONTENT_HEIGHT = 660;

    // Card layout lets us switch between multiple content panels ("cards")
    private CardLayout cardLayout;
    private JPanel cardPanel;

    /**
     * Constructor: Sets up the entire user interface.
     * Initializes the frame, adds branding elements, navigation, and content cards.
     */
    public MRPSystemUI() {
        // ----- FRAME SETUP -----
        setTitle("MRP System");  // Set the text shown in the window title bar
        setSize(FRAME_WIDTH, FRAME_HEIGHT); // Set window size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit the application on close
        setLocationRelativeTo(null); // Center the window on the screen
        setLayout(new BorderLayout()); // Use a border layout for the top-level frame (though it's mostly overridden)

        // ----- MAIN PANEL -----
        // This is the container for everything visible inside the frame.
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK); // Branding: black background
        mainPanel.setLayout(null); // Use absolute positioning for precise control
        add(mainPanel, BorderLayout.CENTER); // Add main panel to the frame

        // ----- LOGO (TOP-LEFT CORNER) -----
        JLabel logoLabel = new JLabel(); // Empty label to hold the logo image
        try {
            // Attempt to read logo image from the project’s resource folder
            BufferedImage logoImage = ImageIO.read(
                getClass().getClassLoader().getResource("VisualRoboticsLogo.png")
            );

            // Smoothly scale the image to fit within defined logo dimensions
            Image scaledLogo = logoImage.getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledLogo));
        } catch (IOException e) {
            // Handle case where image resource is not found or unreadable
            e.printStackTrace();
        }

        // Position the logo using absolute coordinates
        logoLabel.setBounds(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
        mainPanel.add(logoLabel);

        // ----- SYSTEM TITLE -----
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE); // White text for visibility against black background
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE)); // Set font and size
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);
        mainPanel.add(titleLabel);

        // ----- NAVIGATION BUTTONS (LEFT SIDE MENU) -----
        // List of section names to generate buttons and corresponding cards
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066"); // Custom green color to match brand identity
        int yOffset = BUTTON_START_Y;

        // Dynamically generate one button for each section name
        for (String text : buttons) {
            JButton btn = new JButton(text); // Create button with section name
            btn.setBackground(vrGreen);      // Set background color to match theme
            btn.setForeground(Color.WHITE);  // Set text color to white
            btn.setOpaque(true);             // Required for custom background colors
            btn.setBorderPainted(false);     // Remove button border for modern look
            btn.setFocusPainted(false);      // Disable focus highlight box when clicked
            btn.setFont(new Font("Arial", Font.BOLD, BUTTON_FONT_SIZE));
            btn.setBounds(LOGO_X, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT); // Position the button

            // Associate button with its matching card panel
            btn.setActionCommand(text);
            btn.addActionListener(e -> showCard(e.getActionCommand())); // Switch content on click

            mainPanel.add(btn);
            yOffset += BUTTON_SPACING; // Move next button down
        }

        // ----- DYNAMIC CONTENT PANEL (RIGHT SIDE) -----
        // This panel uses CardLayout to show one content panel at a time.
        cardLayout = new CardLayout();       // CardLayout manages switching between views
        cardPanel = new JPanel(cardLayout);  // Content area container
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        mainPanel.add(cardPanel);

        // ----- ADD INDIVIDUAL CARDS TO CONTENT PANEL -----
        // For each section, create a simple placeholder panel with a label
        for (String name : buttons) {
            JPanel card = new JPanel();               // A panel for each section
            card.setBackground(Color.WHITE);          // Clean white background
            JLabel label = new JLabel(name);          // Placeholder title
            label.setFont(new Font("Arial", Font.BOLD, CARD_FONT_SIZE));
            card.add(label);                          // Add title to card
            cardPanel.add(card, name);                // Add card to cardLayout with key name
        }
    }

    /**
     * Switches the visible content panel inside the card panel.
     * This function is triggered by navigation button clicks.
     *
     * @param name The identifier (action command) of the card to show.
     */
    private void showCard(String name) {
        cardLayout.show(cardPanel, name); // Tell the layout manager to show the panel with the matching name
    }

    /**
     * Main method – the program's entry point.
     * Ensures GUI components are created on the Swing event dispatch thread (EDT),
     * which is required for thread safety in all Swing apps.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MRPSystemUI ui = new MRPSystemUI(); // Create the UI instance
            ui.setVisible(true);                // Display the window
        });
    }
}
