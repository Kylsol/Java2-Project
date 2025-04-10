package com.bushnell;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Panel for updating stock information.
 */
public class UpdateStockPanel extends JPanel {

    private static final int FONT_SIZE = 20;
    private static final int COMBOBOX_WIDTH = 200;
    private static final int DECIMAL_PLACES = 3;

    private static final String DB_PATH = "jdbc:sqlite:VR-Factory.db";

    private JComboBox<String> skuComboBox;
    private JTextField descriptionField;
    private JTextField priceField;
    private JTextField stockField;
    private JButton updateButton;
    private JTable recordTable;

    private DefaultTableModel tableModel;

    /**
     * Constructs the UpdateStockPanel UI.
     * Initializes the layout, components (labels, text fields, combo box, buttons), 
     * and loads SKU data into the combo box.
     */
    public UpdateStockPanel() {
        setLayout(new BorderLayout(10, 10));

        // Panel to hold input fields and labels
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBackground(Color.WHITE);
        add(panel, BorderLayout.CENTER);

        // Title label at the top
        JLabel titleLabel = new JLabel("Update Stock");
        titleLabel.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Set constraints for the labels and fields
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // SKU Label and Combo Box
        JLabel skuLabel = new JLabel("SKU:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(skuLabel, gbc);

        skuComboBox = new JComboBox<>();
        gbc.gridx = 1;
        panel.add(skuComboBox, gbc);
        skuComboBox.addActionListener(this::onSKUSelected); // SKU selection listener

        // Description Label and Field
        JLabel descLabel = new JLabel("Description:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(descLabel, gbc);

        descriptionField = new JTextField(20);
        descriptionField.setEditable(false);  // Set description field to read-only
        gbc.gridx = 1;
        panel.add(descriptionField, gbc);

        // Price Label and Field
        JLabel priceLabel = new JLabel("Price:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(priceLabel, gbc);

        priceField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(priceField, gbc);

        // Stock Label and Field
        JLabel stockLabel = new JLabel("Stock:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(stockLabel, gbc);

        stockField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(stockField, gbc);

        // Update Record Button - Centered
        updateButton = new JButton("Update Record");
        updateButton.addActionListener(this::onUpdateClicked);  // Action listener for the update button
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;  // Makes the button span across both columns
        gbc.anchor = GridBagConstraints.CENTER; // Centers the button
        panel.add(updateButton, gbc);

        // Table to display full record of the selected SKU
        String[] columnNames = {"SKU", "Description", "Price", "Stock"};
        tableModel = new DefaultTableModel(columnNames, 0);
        recordTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(recordTable);
        add(scrollPane, BorderLayout.SOUTH);

        loadSKUs();  // Load SKU data when the panel is initialized
    }

    /**
     * Loads the available SKUs from the database into the combo box.
     * Fetches the SKU values from the 'part' table and adds them to the combo box.
     */
    private void loadSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sku FROM part")) {

            // Populate combo box with SKUs
            while (rs.next()) {
                skuComboBox.addItem(rs.getString("sku"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading SKUs: " + e.getMessage());
        }
    }

    /**
     * Handles the SKU selection event.
     * When the user selects an SKU from the combo box, the details of the SKU 
     * (description, price, stock) are loaded into their respective fields and the table is updated.
     */
    private void onSKUSelected(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();
        if (selectedSKU == null) {
            return;  // Exit if no SKU is selected
        }

        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT description, price, stock FROM part WHERE sku = ?")) {

            stmt.setString(1, selectedSKU);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Populate the fields with the selected SKU's details
                descriptionField.setText(rs.getString("description"));
                priceField.setText(String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")));
                stockField.setText(String.valueOf(rs.getInt("stock")));

                // Update table with SKU data
                tableModel.setRowCount(0);  // Clear previous data
                tableModel.addRow(new Object[]{
                        selectedSKU,
                        rs.getString("description"),
                        String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")),
                        rs.getInt("stock")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading SKU data: " + ex.getMessage());
        }
    }

    /**
     * Handles the click event for the update button.
     * When the user clicks 'Update Record', the system confirms the update 
     * and then updates the price and stock for the selected SKU in the database.
     */
    private void onUpdateClicked(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();
        if (selectedSKU == null) {
            return;  // Exit if no SKU is selected
        }

        // Confirmation dialog before updating the record
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to update the record?", "Confirm Update", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            try {
                // Parse new price and stock values
                double newPrice = Double.parseDouble(priceField.getText());
                int newStock = Integer.parseInt(stockField.getText());

                // Prepare and execute the SQL update query
                try (Connection conn = DriverManager.getConnection(DB_PATH);
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE part SET price = ?, stock = ? WHERE sku = ?")) {

                    stmt.setDouble(1, newPrice);
                    stmt.setInt(2, newStock);
                    stmt.setString(3, selectedSKU);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Stock updated successfully.");
                        refreshTable(selectedSKU);  // Refresh the table after update
                    } else {
                        JOptionPane.showMessageDialog(this, "No changes made.");
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid number format.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        }
    }

    /**
     * Refreshes the table with updated information for the selected SKU.
     * Fetches the latest data for the SKU and updates the table model.
     */
    private void refreshTable(String selectedSKU) {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM part WHERE sku = ?")) {

            stmt.setString(1, selectedSKU);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Update the table model with the new data
                tableModel.setValueAt(rs.getString("sku"), 0, 0);
                tableModel.setValueAt(rs.getString("description"), 0, 1);
                tableModel.setValueAt(rs.getDouble("price"), 0, 2);
                tableModel.setValueAt(rs.getInt("stock"), 0, 3);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error refreshing the table: " + ex.getMessage());
        }
    }
}
