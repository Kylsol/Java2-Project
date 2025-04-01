package com.bushnell;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Main class for the MRP System UI using Java Swing.
 * It displays a logo, system title, navigation buttons, and a dynamic content panel.
 */
public class MRPSystemUI extends JFrame {

    // Constants for layout dimensions and styling
    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;

    // Logo positioning and size
    private static final int LOGO_WIDTH = 180;
    private static final int LOGO_HEIGHT = 51;
    private static final int LOGO_X = 10;
    private static final int LOGO_Y = 10;

    // Title positioning and size
    private static final int TITLE_X = 10;
    private static final int TITLE_Y = 70;
    private static final int TITLE_WIDTH = 200;
    private static final int TITLE_HEIGHT = 30;

    // Button layout settings
    private static final int BUTTON_START_Y = 110;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_SPACING = 50;
    private static final int BUTTON_FONT_SIZE = 14;

    // Font sizes for other elements
    private static final int TITLE_FONT_SIZE = 20;
    private static final int CARD_FONT_SIZE = 24;

    // Content panel positioning and size
    private static final int CONTENT_X = 200;
    private static final int CONTENT_Y = 10;
    private static final int CONTENT_WIDTH = 1060;
    private static final int CONTENT_HEIGHT = 660;

    // Swing layout and panel to manage dynamic content swapping
    private CardLayout cardLayout;
    private JPanel cardPanel;

    /**
     * Constructor to set up the UI elements, layout, and functionality.
     */
    public MRPSystemUI() {
        // Set the window title and default configurations
        setTitle("MRP System");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close app when window is closed
        setLocationRelativeTo(null); // Center the window on the screen
        setLayout(new BorderLayout());

        // Main container panel with absolute positioning and black background
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setLayout(null); // Use absolute positioning
        add(mainPanel, BorderLayout.CENTER);

        // Logo setup: load image from resources and scale it
        JLabel logoLabel = new JLabel();
        try {
            BufferedImage logoImage = ImageIO.read(
                getClass().getClassLoader().getResource("VisualRoboticsLogo.png")
            );
            Image scaledLogo = logoImage.getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledLogo));
        } catch (IOException e) {
            e.printStackTrace(); // Print error if logo can't be loaded
        }

        // Position the logo on the panel
        logoLabel.setBounds(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
        mainPanel.add(logoLabel);

        // Add a title label under the logo
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);
        mainPanel.add(titleLabel);

        // Create and configure navigation buttons
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066"); // Custom green color for branding
        int yOffset = BUTTON_START_Y;

        // Generate buttons dynamically based on the names above
        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setBackground(vrGreen);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Arial", Font.BOLD, BUTTON_FONT_SIZE));
            btn.setBounds(LOGO_X, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT);
            btn.setActionCommand(text); // Used to switch cards later
            btn.addActionListener(e -> showCard(e.getActionCommand())); // Handle button clicks
            mainPanel.add(btn);
            yOffset += BUTTON_SPACING; // Position next button lower
        }

        // Set up card panel for switching between content views
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        mainPanel.add(cardPanel);

        // Create one panel per section and add to card layout
        for (String name : buttons) {
            JPanel card = new JPanel();
            card.setBackground(Color.WHITE);
            JLabel label = new JLabel(name);
            label.setFont(new Font("Arial", Font.BOLD, CARD_FONT_SIZE));
            card.add(label); // Each card just displays a label for now
            cardPanel.add(card, name); // Add the card to the layout with the name as identifier
        }
    }

    /**
     * Switches the visible card in the card panel to match the selected button.
     *
     * @param name The name of the card to display (must match the action command).
     */
    private void showCard(String name) {
        cardLayout.show(cardPanel, name); // Show the card matching the button clicked
    }

    /**
     * Main method to launch the MRP System UI using the Swing event dispatch thread.
     */
    public static void main(String[] args) {
        // Ensure UI runs on the Swing event thread
        SwingUtilities.invokeLater(() -> new MRPSystemUI().setVisible(true));
    }
}
