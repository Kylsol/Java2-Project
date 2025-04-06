package com.bushnell;

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
 */
public class MRPSystemUI extends JFrame {

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

    private CardLayout cardLayout;
    private JPanel cardPanel;

    /**
     * Constructs the main system UI.
     * Initializes the JFrame, sets its properties, and adds components including 
     * the logo, title, buttons, and content panels for navigation.
     */
    public MRPSystemUI() {
        // Set the title, size, close operation, and location of the window
        setTitle("MRP System");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set custom icon for the application window
        URL iconURL = getClass().getClassLoader().getResource("VisualRoboticsIcon.png");
        if (iconURL != null) {
            System.out.println("Icon found: " + iconURL);
            setIconImage(new ImageIcon(iconURL).getImage());
        } else {
            System.out.println("Icon not found!");
        }

        // Set up main panel to contain other components
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setLayout(null);
        add(mainPanel, BorderLayout.CENTER);

        // Add logo to the main panel
        JLabel logoLabel = new JLabel();
        try {
            BufferedImage logoImage = ImageIO.read(
                getClass().getClassLoader().getResource("VisualRoboticsLogo.png")
            );
            if (logoImage != null) {
                Image scaledLogo = logoImage.getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));
            } else {
                System.err.println("Logo image not found in resources.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Position the logo on the main panel
        logoLabel.setBounds(LOGO_X, LOGO_Y, LOGO_WIDTH, LOGO_HEIGHT);
        mainPanel.add(logoLabel);

        // Add the title label to the main panel
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);
        mainPanel.add(titleLabel);

        // Array of button labels
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066");
        int yOffset = BUTTON_START_Y;

        // Add buttons to the main panel
        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setBackground(vrGreen);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Arial", Font.BOLD, BUTTON_FONT_SIZE));
            btn.setBounds(LOGO_X, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT);
            btn.setActionCommand(text);
            btn.addActionListener(e -> showCard(e.getActionCommand()));
            mainPanel.add(btn);
            yOffset += BUTTON_SPACING;
        }

        // Create a card layout and panel to manage different content sections
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        mainPanel.add(cardPanel);

        // Add individual content panels to the card layout
        for (String name : buttons) {
            if ("Update Stock".equals(name)) {
                cardPanel.add(new UpdateStockPanel(), name);
            } else {
                JPanel card = new JPanel();
                card.setBackground(Color.WHITE);
                JLabel label = new JLabel(name);
                label.setFont(new Font("Arial", Font.BOLD, CARD_FONT_SIZE));
                card.add(label);
                cardPanel.add(card, name);
            }
        }

        // Show the first card by default
        cardLayout.show(cardPanel, buttons[0]);
    }

    /**
     * Switches the visible content panel inside the card panel.
     * @param name The identifier of the card to show.
     * This method updates the view within the cardPanel based on the button pressed.
     */
    private void showCard(String name) {
        cardLayout.show(cardPanel, name);
    }

    /**
     * Main method – the program's entry point.
     * This method initializes the MRPSystemUI and sets it visible.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MRPSystemUI ui = new MRPSystemUI();
            ui.setVisible(true);
        });
    }
}
