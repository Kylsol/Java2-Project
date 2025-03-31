package com.bushnell;

// Import libraries for image handling, UI components, layout managers, event handling, and image buffers
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Main class for the MRP System UI using Java Swing.
 * Displays a logo, system title, navigation buttons, and a dynamic content panel.
 */
public class MRPSystemUI extends JFrame {
    // Layout manager used to switch between different views (cards)
    private CardLayout cardLayout;

    // Main content area that changes based on the selected button
    private JPanel cardPanel;

    /**
     * Constructor to set up the UI elements, layout, and functionality
     */
    public MRPSystemUI() {
        // Set window properties
        setTitle("MRP System");                         // Window title
        setSize(1280, 720);                      // Window dimensions
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);       // Exit app on close
        setLocationRelativeTo(null);                        // Center window on screen
        setLayout(new BorderLayout());                        // Use BorderLayout for frame

        // Create main panel with black background and absolute positioning
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK); // Set background color to black
        mainPanel.setLayout(null);            // Use null layout for manual positioning
        add(mainPanel, BorderLayout.CENTER);  // Add panel to center of frame

        // ---------- Load and Display Logo ----------

        // JLabel to hold and display the logo image
        JLabel logoLabel = new JLabel();
        try {
            // Load image from resources folder using class loader
            BufferedImage logoImage = ImageIO.read(getClass().getClassLoader().getResource("VisualRoboticsLogo.png"));
            // Scale logo to fit desired dimensions
            Image scaledLogo = logoImage.getScaledInstance(180, 51, Image.SCALE_SMOOTH);
            // Set scaled image as icon on label
            logoLabel.setIcon(new ImageIcon(scaledLogo));
        } catch (IOException e) {
            // Print error if loading fails
            e.printStackTrace();
        }

        // Position the logo on the screen
        logoLabel.setBounds(10, 10, 180, 51);
        mainPanel.add(logoLabel);

        // ---------- Display App Title ----------

        // Create and style label for "MRP System" text
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);                                         // Set text color to white
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));                // Set font style and size
        titleLabel.setBounds(10, 70, 200, 30);                        // Position below the logo
        mainPanel.add(titleLabel);

        // ---------- Create Navigation Buttons ----------

        // Array of button names to create
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};

        // Define custom green color (same as VR logo)
        Color vrGreen = Color.decode("#6DC066");

        // Starting Y position for the first button
        int yOffset = 110;

        // Loop through button names and create styled buttons
        for (String text : buttons) {
            JButton btn = new JButton(text);                              // Create button with label
            btn.setBackground(vrGreen);                                   // Set button background color
            btn.setForeground(Color.WHITE);                               // Set button text color
            btn.setOpaque(true);                                 // Make sure background color is visible (important on macOS)
            btn.setBorderPainted(false);                                // Remove 3D border look
            btn.setFocusPainted(false);                                 // Remove blue focus ring
            btn.setFont(new Font("Arial", Font.BOLD, 14));      // Set button font style
            btn.setBounds(10, yOffset, 160, 40);           // Set button position and size
            btn.setActionCommand(text);                                   // Set action command to identify button later
            btn.addActionListener(e -> showCard(e.getActionCommand()));   // Add event to show corresponding card
            mainPanel.add(btn);                                           // Add button to the panel
            yOffset += 50;                                                // Move Y position down for the next button
        }

        // ---------- Create Dynamic Content Area ----------

        // Initialize CardLayout and apply it to a new panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);                                 // Panel to hold swappable cards
        cardPanel.setBackground(Color.WHITE);                               // White background for card area
        cardPanel.setBounds(200, 10, 1060, 660);           // Position next to buttons
        mainPanel.add(cardPanel);                                           // Add card panel to main panel

        // Create one card for each button, add it to card panel
        for (String name : buttons) {
            JPanel card = new JPanel();                                       // Create a new card panel
            card.setBackground(Color.WHITE);                                  // Set card background
            JLabel label = new JLabel(name);                                  // Create label for the card
            label.setFont(new Font("Arial", Font.BOLD, 24));        // Style the label
            card.add(label);                                                  // Add label to card
            cardPanel.add(card, name);                                        // Add card to layout with a name
        }
    }

    /**
     * Switches the visible card in the card panel to match the selected button.
     * @param name The name of the card to display (must match the action command)
     */
    private void showCard(String name) {
        cardLayout.show(cardPanel, name); // Show the selected card by name
    }

    /**
     * Main method to launch the MRP System UI using the Swing event dispatch thread.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MRPSystemUI().setVisible(true));
    }
}
