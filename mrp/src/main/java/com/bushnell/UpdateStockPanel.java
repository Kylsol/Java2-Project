package com.bushnell;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Panel for updating stock information.
 * This panel allows the user to select a SKU (Stock Keeping Unit) from a combo box,
 * view its description, price, and stock, and update the price and stock information.
 */
public class UpdateStockPanel extends JPanel {

    // Constants for font size and other UI components
    private static final int FONT_SIZE = 20;  // Font size for title and labels
    private static final int COMBOBOX_WIDTH = 200;  // Width of the combo box for SKU selection
    private static final int DECIMAL_PLACES = 3;  // Number of decimal places for the price field

    // Path to the SQLite database
    private static final String DB_PATH = "jdbc:sqlite:VR-Factory.db";

    // UI components
    private JComboBox<String> skuComboBox;  // Combo box for selecting SKU
    private JTextField descriptionField;  // Text field to display the description of the selected SKU
    private JTextField priceField;  // Text field to display and edit the price of the selected SKU
    private JTextField stockField;  // Text field to display and edit the stock quantity of the selected SKU
    private JButton updateButton;  // Button to submit the updated stock information
    private JTable recordTable;  // Table to display the full record of the selected SKU
    private DefaultTableModel tableModel;  // Model for the SKU table

    /**
     * Constructs the UpdateStockPanel UI.
     * Initializes the layout, components (labels, text fields, combo box, buttons), 
     * and loads SKU data into the combo box.
     */
    public UpdateStockPanel() {
        setLayout(new BorderLayout(10, 10));  // Set up BorderLayout with a gap between components

        // Panel to hold input fields and labels
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());  // Use GridBagLayout for flexible component positioning
        GridBagConstraints gbc = new GridBagConstraints();  // Used for positioning the components
        panel.setBackground(Color.WHITE);  // Set the background color of the panel to white
        add(panel, BorderLayout.CENTER);  // Add the panel to the center of the main layout

        // Title label at the top of the panel
        JLabel titleLabel = new JLabel("Update Stock");
        titleLabel.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));  // Set the font for the title
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);  // Center the title text
        add(titleLabel, BorderLayout.NORTH);  // Add the title to the top of the main layout

        // Set up GridBagConstraints for consistent component spacing
        gbc.insets = new Insets(5, 5, 5, 5);  // Set the padding around each component
        gbc.anchor = GridBagConstraints.WEST;  // Align components to the west (left)

        // SKU Label and Combo Box
        JLabel skuLabel = new JLabel("SKU:");
        gbc.gridx = 0;  // Position in the first column
        gbc.gridy = 0;  // Position in the first row
        panel.add(skuLabel, gbc);  // Add the SKU label to the panel

        skuComboBox = new JComboBox<>();  // Create a combo box for SKU selection
        gbc.gridx = 1;  // Position in the second column
        panel.add(skuComboBox, gbc);  // Add the combo box to the panel
        skuComboBox.addActionListener(this::onSKUSelected);  // Add an action listener to handle SKU selection

        // Description Label and Field
        JLabel descLabel = new JLabel("Description:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(descLabel, gbc);

        descriptionField = new JTextField(20);  // Create a text field for the description
        descriptionField.setEditable(false);  // Make the description field read-only
        gbc.gridx = 1;
        panel.add(descriptionField, gbc);

        // Price Label and Field
        JLabel priceLabel = new JLabel("Price:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(priceLabel, gbc);

        priceField = new JTextField(20);  // Create a text field for the price
        gbc.gridx = 1;
        panel.add(priceField, gbc);

        // Stock Label and Field
        JLabel stockLabel = new JLabel("Stock:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(stockLabel, gbc);

        stockField = new JTextField(20);  // Create a text field for the stock quantity
        gbc.gridx = 1;
        panel.add(stockField, gbc);

        // Update Record Button - Centered
        updateButton = new JButton("Update Record");  // Create the update button
        updateButton.addActionListener(this::onUpdateClicked);  // Add an action listener to handle the update
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;  // Make the button span across both columns
        gbc.anchor = GridBagConstraints.CENTER;  // Center the button
        panel.add(updateButton, gbc);

        // Table to display full record of the selected SKU
        String[] columnNames = {"SKU", "Description", "Price", "Stock"};  // Table column headers
        tableModel = new DefaultTableModel(columnNames, 0);  // Create a table model with the column names
        recordTable = new JTable(tableModel);  // Create a table with the model
        JScrollPane scrollPane = new JScrollPane(recordTable);  // Wrap the table in a scroll pane for better UX
        add(scrollPane, BorderLayout.SOUTH);  // Add the table to the bottom of the main layout

        loadSKUs();  // Load SKU data into the combo box when the panel is initialized
    }

    private ImageIcon getVRIcon() {
        return new ImageIcon(getClass().getResource("/VisualRoboticsIcon.png"));
    }

    private void loadSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sku FROM part")) {

            while (rs.next()) {
                skuComboBox.addItem(rs.getString("sku"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading SKUs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, getVRIcon());
        }
    }

    private void onSKUSelected(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();
        if (selectedSKU == null) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT description, price, stock FROM part WHERE sku = ?")) {

            stmt.setString(1, selectedSKU);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                descriptionField.setText(rs.getString("description"));
                priceField.setText(String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")));
                stockField.setText(String.valueOf(rs.getInt("stock")));

                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{
                        selectedSKU,
                        rs.getString("description"),
                        String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")),
                        rs.getInt("stock")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading SKU data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, getVRIcon());
        }
    }

    private void onUpdateClicked(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();
        if (selectedSKU == null) {
            return;
        }

        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to update the record?", "Confirm Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, getVRIcon());

        if (response == JOptionPane.YES_OPTION) {
            try {
                double newPrice = Double.parseDouble(priceField.getText());
                int newStock = Integer.parseInt(stockField.getText());

                try (Connection conn = DriverManager.getConnection(DB_PATH);
                     PreparedStatement stmt = conn.prepareStatement("UPDATE part SET price = ?, stock = ? WHERE sku = ?")) {

                    stmt.setDouble(1, newPrice);
                    stmt.setInt(2, newStock);
                    stmt.setString(3, selectedSKU);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Stock updated successfully.", "Update Successful", JOptionPane.INFORMATION_MESSAGE, getVRIcon());
                        refreshTable(selectedSKU);
                    } else {
                        JOptionPane.showMessageDialog(this, "No changes made.", "No Update", JOptionPane.WARNING_MESSAGE, getVRIcon());
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid number format.", "Invalid Input", JOptionPane.ERROR_MESSAGE, getVRIcon());
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE, getVRIcon());
            }
        }
    }

    private void refreshTable(String selectedSKU) {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM part WHERE sku = ?")) {

            stmt.setString(1, selectedSKU);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                tableModel.setValueAt(rs.getString("sku"), 0, 0);
                tableModel.setValueAt(rs.getString("description"), 0, 1);
                tableModel.setValueAt(rs.getDouble("price"), 0, 2);
                tableModel.setValueAt(rs.getInt("stock"), 0, 3);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error refreshing the table: " + ex.getMessage(), "Refresh Error", JOptionPane.ERROR_MESSAGE, getVRIcon());
        }
    }
}

