package com.bushnell;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MRPSystemUI extends JFrame {
    private CardLayout cardLayout; // Layout manager for swapping panels
    private JPanel cardPanel;      // The main content area that changes with each button

    public MRPSystemUI() {
        setTitle("MRP System");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create the main panel with black background and absolute positioning
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setLayout(null);
        add(mainPanel, BorderLayout.CENTER);

        // Load and scale logo image
        JLabel logoLabel = new JLabel();
        try {
            BufferedImage logoImage = ImageIO.read(getClass().getClassLoader().getResource("VisualRoboticsLogo.png"));
            Image scaledLogo = logoImage.getScaledInstance(180, 51, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledLogo));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logoLabel.setBounds(10, 10, 180, 51);
        mainPanel.add(logoLabel);

        // Title label
        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(10, 70, 200, 30);
        mainPanel.add(titleLabel);

        // Define buttons and layout
        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066");
        int yOffset = 110;

        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setBackground(vrGreen);
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);                    // Ensures background color is visible on macOS
            btn.setBorderPainted(false);            // Removes default 3D border
            btn.setFocusPainted(false);
            btn.setFont(new Font("Arial", Font.BOLD, 14));
            btn.setBounds(10, yOffset, 160, 40);
            btn.setActionCommand(text);
            btn.addActionListener(e -> showCard(e.getActionCommand()));
            mainPanel.add(btn);
            yOffset += 50; // Move the next button down
        }

        // Panel on the right side using CardLayout to swap views
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(200, 10, 1060, 660);
        mainPanel.add(cardPanel);

        // Add cards for each button view
        for (String name : buttons) {
            JPanel card = new JPanel();
            card.setBackground(Color.WHITE);
            JLabel label = new JLabel(name);
            label.setFont(new Font("Arial", Font.BOLD, 24));
            card.add(label);
            cardPanel.add(card, name);
        }
    }

    // Switch visible card in the right panel
    private void showCard(String name) {
        cardLayout.show(cardPanel, name);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MRPSystemUI().setVisible(true));
    }
}
