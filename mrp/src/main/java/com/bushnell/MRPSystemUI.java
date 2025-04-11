package com.bushnell;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
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
import java.awt.MediaTracker;
import java.net.URL;

/**
 * Main GUI class for the MRP (Material Requirements Planning) System.
 * This class is responsible for creating and displaying the main user interface window, 
 * adding components such as buttons, labels, and panels, and managing navigation between different panels.
 */
public class MRPSystemUI extends JFrame {

    // Constants for window size and positioning
    private static final int FRAME_WIDTH = 1280;  // Width of the main window
    private static final int FRAME_HEIGHT = 720;  // Height of the main window

    // Constants for logo dimensions and position
    private static final int LOGO_WIDTH = 180;  // Width of the logo
    private static final int LOGO_HEIGHT = 51;  // Height of the logo
    private static final int LOGO_X = 10;  // X-position of the logo on the window
    private static final int LOGO_Y = 10;  // Y-position of the logo on the window

    // Constants for title label dimensions and position
    private static final int TITLE_X = 10;  // X-position of the title
    private static final int TITLE_Y = 70;  // Y-position of the title
    private static final int TITLE_WIDTH = 200;  // Width of the title label
    private static final int TITLE_HEIGHT = 30;  // Height of the title label

    // Constants for button dimensions, positioning, and font size
    private static final int BUTTON_START_Y = 110;  // Starting Y-position for buttons
    private static final int BUTTON_WIDTH = 160;  // Width of each button
    private static final int BUTTON_HEIGHT = 40;  // Height of each button
    private static final int BUTTON_SPACING = 50;  // Vertical spacing between buttons
    private static final int BUTTON_FONT_SIZE = 14;  // Font size for buttons

    // Constants for the title and card font sizes
    private static final int TITLE_FONT_SIZE = 20;  // Font size for the main title
    private static final int CARD_FONT_SIZE = 24;  // Font size for content cards

    // Constants for content panel position and size
    private static final int CONTENT_X = 200;  // X-position for the content panel
    private static final int CONTENT_Y = 10;  // Y-position for the content panel
    private static final int CONTENT_WIDTH = 1060;  // Width of the content panel
    private static final int CONTENT_HEIGHT = 660;  // Height of the content panel

    // Instance variables for CardLayout and the content panel
    private CardLayout cardLayout;  // CardLayout object to manage panel switching
    private JPanel cardPanel;  // JPanel to hold the content cards
    private StockReport stockReportPanel;
    

    /**
     * Constructs the main system UI.
     * Initializes the JFrame, sets its properties, and adds components including 
     * the logo, title, buttons, and content panels for navigation.
     * Constructs the main system UI.
     * Initializes the JFrame, sets its properties, and adds components including 
     * the logo, title, buttons, and content panels for navigation.
     */
    public MRPSystemUI() {
        // Set the window properties: title, size, close operation, and location
        setTitle("MRP System");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);  // Set the size of the main window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Close the application when the window is closed
        setLocationRelativeTo(null);  // Center the window on the screen
        setLayout(new BorderLayout());  // Set the layout of the main window to BorderLayout

        // Set custom icon for the application window
        // Try to load the application icon from resources
        URL iconURL = getClass().getClassLoader().getResource("VisualRoboticsIcon.png");
        if (iconURL != null) {
            System.out.println("Icon found: " + iconURL);  // Print the icon path to the console
            setIconImage(new ImageIcon(iconURL).getImage());  // Set the icon for the window
        } else {
            System.out.println("Icon not found!");  // Print an error if the icon is not found
        }

        // Set up main panel to contain other components
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);  // Set the background color of the panel
        mainPanel.setLayout(null);  // Use absolute positioning for components in this panel
        add(mainPanel, BorderLayout.CENTER);  // Add the panel to the center of the window

        // Add logo to the main panel
        JLabel logoLabel = new JLabel();  // Create a label to display the logo
        try {
            // Try to read the logo image from resources
            BufferedImage logoImage = ImageIO.read(getClass().getClassLoader().getResource("VisualRoboticsLogo.png"));
            if (logoImage != null) {
                // Scale the logo to the specified dimensions
                Image scaledLogo = logoImage.getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));  // Set the scaled logo as the label icon
            } else {
                System.err.println("Logo image not found in resources.");  // Print error if logo is not found
            }
        } catch (IOException e) {
            e.printStackTrace();  // Print any IO exceptions that occur while loading the image
        }

        // Position the logo on the main panel
        logoLabel.setBounds(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
        mainPanel.add(logoLabel);  // Add the logo label to the main panel

        // Add the title label to the main panel
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);  // Set the title text color to white
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE));  // Set the font for the title
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);  // Position the title label
        mainPanel.add(titleLabel);  // Add the title label to the main panel

        // Array of button labels
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066");  // Define a custom green color for buttons
        int yOffset = BUTTON_START_Y;  // Set the starting Y-position for the first button

        // Add buttons to the main panel
        for (String text : buttons) {
            JButton btn = new JButton(text);  // Create a new button with the specified text
            btn.setBackground(vrGreen);  // Set the background color of the button
            btn.setForeground(Color.WHITE);  // Set the text color of the button to white
            btn.setOpaque(true);  // Make the button opaque (solid color)
            btn.setBorderPainted(false);  // Remove the button's border
            btn.setFocusPainted(false);  // Remove the button's focus ring
            btn.setFont(new Font("Arial", Font.BOLD, BUTTON_FONT_SIZE));  // Set the font for the button
            btn.setBounds(LOGO_X, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT);  // Position the button
            btn.setActionCommand(text);  // Set the action command for the button (used for event handling)
            btn.addActionListener(e -> showCard(e.getActionCommand()));  // Add an event listener for button clicks
            mainPanel.add(btn);  // Add the button to the main panel
            yOffset += BUTTON_SPACING;  // Increase the Y-position for the next button
        }

        // Create a card layout and panel to manage different content sections
        cardLayout = new CardLayout();  // Create a new CardLayout object
        cardPanel = new JPanel(cardLayout);  // Create a new JPanel with the CardLayout
        cardPanel.setBackground(Color.WHITE);  // Set the background color of the card panel
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);  // Set the size and position
        mainPanel.add(cardPanel);  // Add the card panel to the main panel

        // Add individual content panels to the card layout
        for (String name : buttons) {
            if ("Update Stock".equals(name)) {
                // If the button is for updating stock, add the UpdateStockPanel to the card layout
                    cardPanel.add(new UpdateStockPanel(), name);
                } else if ("Stock Report".equals(name)) {
                    stockReportPanel = new StockReport();
                    cardPanel.add(stockReportPanel, name);
            } else {
                // For other buttons, create a simple panel with the button's name as the label
                JPanel card = new JPanel();
                card.setBackground(Color.WHITE);  // Set the background color of the panel
                JLabel label = new JLabel(name);  // Create a label with the name of the button
                label.setFont(new Font("Arial", Font.BOLD, CARD_FONT_SIZE));  // Set the font size for the label
                card.add(label);  // Add the label to the panel
                cardPanel.add(card, name);  // Add the panel to the card layout
            }
        }

        // Show the first card by default when the UI is initialized
        cardLayout.show(cardPanel, buttons[0]);
    }

    /**
     * Switches the visible content panel inside the card panel.
     * @param name The identifier of the card to show.
     * This method updates the view within the cardPanel based on the button pressed.
     * Switches the visible content panel inside the card panel.
     * @param name The identifier of the card to show.
     * This method updates the view within the cardPanel based on the button pressed.
     */
    private void showCard(String name) {
        if ("Stock Report".equals(name) && stockReportPanel != null) {
            stockReportPanel.updateReport();  // Refresh report
        }
        cardLayout.show(cardPanel, name);
    }    

    /**
     * Main method – the program's entry point.
     * This method initializes the MRPSystemUI and sets it visible.
     * Main method – the program's entry point.
     * This method initializes the MRPSystemUI and sets it visible.
     */
    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread to ensure thread safety
        SwingUtilities.invokeLater(() -> {
            MRPSystemUI ui = new MRPSystemUI();  // Create an instance of the MRPSystemUI
            ui.setVisible(true);  // Set the UI to be visible
        });
    }
}
