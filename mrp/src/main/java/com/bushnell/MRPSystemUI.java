package com.bushnell;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.net.URL;

import com.bushnell.DemandAnalysis;

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
    private StockReport stockReportPanel;
    private BundlePanel bundlePanel;
    private DemandAnalysis demandAnalysisPanel;

    public MRPSystemUI() {
        setTitle("MRP System");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        URL iconURL = getClass().getClassLoader().getResource("VisualRoboticsIcon.png");
        if (iconURL != null) {
            setIconImage(new ImageIcon(iconURL).getImage());
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.BLACK);
        mainPanel.setLayout(null);
        add(mainPanel, BorderLayout.CENTER);

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
        mainPanel.add(logoLabel);

        JLabel titleLabel = new JLabel("MRP System");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBounds(TITLE_X, TITLE_Y, TITLE_WIDTH, TITLE_HEIGHT);
        mainPanel.add(titleLabel);

        String[] buttons = {"Update Stock", "Stock Report", "Bundle", "Demand Analysis"};
        Color vrGreen = Color.decode("#6DC066");
        int yOffset = BUTTON_START_Y;

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

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBounds(CONTENT_X, CONTENT_Y, CONTENT_WIDTH, CONTENT_HEIGHT);
        mainPanel.add(cardPanel);

        for (String name : buttons) {
            switch (name) {
                case "Update Stock":
                    cardPanel.add(new UpdateStockPanel(), name);
                    break;
                case "Stock Report":
                    stockReportPanel = new StockReport();
                    cardPanel.add(stockReportPanel, name);
                    break;
                case "Bundle":
                    bundlePanel = new BundlePanel();
                    cardPanel.add(bundlePanel, name);
                    break;
                case "Demand Analysis":
                    demandAnalysisPanel = new DemandAnalysis();
                    cardPanel.add(demandAnalysisPanel, name);
                    break;
            }
        }

        cardLayout.show(cardPanel, buttons[0]);
    }

    private void showCard(String name) {
        if ("Update Stock".equals(name)) {
            cardPanel.getComponent(0).revalidate();
            cardPanel.getComponent(0).repaint();
        }
        if ("Demand Analysis".equals(name) && demandAnalysisPanel != null) {
    cardPanel.remove(demandAnalysisPanel);
    demandAnalysisPanel = new DemandAnalysis();
    cardPanel.add(demandAnalysisPanel, "Demand Analysis");
}
        if ("Stock Report".equals(name) && stockReportPanel != null) {
            stockReportPanel.updateReport();
        }
        if ("Bundle".equals(name) && bundlePanel != null) {
            bundlePanel.resetSelection();
        }
        cardLayout.show(cardPanel, name);
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MRPSystemUI ui = new MRPSystemUI();
            ui.setVisible(true);
        });
    }
}
