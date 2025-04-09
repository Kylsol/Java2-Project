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

    /**
     * Loads the available SKUs from the database into the combo box.
     * Fetches the SKU values from the 'part' table and adds them to the combo box.
     */
    private void loadSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);  // Establish a connection to the database
             Statement stmt = conn.createStatement();  // Create a statement for SQL queries
             ResultSet rs = stmt.executeQuery("SELECT sku FROM part")) {  // Execute a query to get SKUs

            // Populate combo box with SKUs
            while (rs.next()) {  // Iterate over the result set
                skuComboBox.addItem(rs.getString("sku"));  // Add each SKU to the combo box
            }
        } catch (SQLException e) {
            e.printStackTrace();  // Print any SQL errors to the console
            JOptionPane.showMessageDialog(this, "Error loading SKUs: " + e.getMessage());  // Show an error message
        }
    }

    /**
     * Handles the SKU selection event.
     * When the user selects an SKU from the combo box, the details of the SKU 
     * (description, price, stock) are loaded into their respective fields and the table is updated.
     */
    private void onSKUSelected(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();  // Get the selected SKU from the combo box
        if (selectedSKU == null) {
            return;  // Exit if no SKU is selected
        }

        // Query the database to fetch the details of the selected SKU
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT description, price, stock FROM part WHERE sku = ?")) {  // Prepare a statement
            stmt.setString(1, selectedSKU);  // Set the SKU parameter in the query
            ResultSet rs = stmt.executeQuery();  // Execute the query

            if (rs.next()) {  // If a result is found for the selected SKU
                // Populate the fields with the selected SKU's details
                descriptionField.setText(rs.getString("description"));
                priceField.setText(String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")));
                stockField.setText(String.valueOf(rs.getInt("stock")));

                // Update the table with the SKU data
                tableModel.setRowCount(0);  // Clear previous data from the table
                tableModel.addRow(new Object[]{
                        selectedSKU,
                        rs.getString("description"),
                        String.format("%." + DECIMAL_PLACES + "f", rs.getDouble("price")),
                        rs.getInt("stock")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();  // Print any SQL errors to the console
            JOptionPane.showMessageDialog(this, "Error loading SKU data: " + ex.getMessage());  // Show an error message
        }
    }

    /**
     * Handles the click event for the update button.
     * When the user clicks 'Update Record', the system confirms the update 
     * and then updates the price and stock for the selected SKU in the database.
     */
    private void onUpdateClicked(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();  // Get the selected SKU
        if (selectedSKU == null) {
            return;  // Exit if no SKU is selected
        }

        // Confirmation dialog before updating the record
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to update the record?", "Confirm Update", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            try {
                // Parse the new price and stock values from the text fields
                double newPrice = Double.parseDouble(priceField.getText());
                int newStock = Integer.parseInt(stockField.getText());

                // Prepare and execute the SQL update query
                try (Connection conn = DriverManager.getConnection(DB_PATH);
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE part SET price = ?, stock = ? WHERE sku = ?")) {

                    stmt.setDouble(1, newPrice);  // Set the new price
                    stmt.setInt(2, newStock);  // Set the new stock quantity
                    stmt.setString(3, selectedSKU);  // Set the SKU to update

                    int rows = stmt.executeUpdate();  // Execute the update query
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Stock updated successfully.");  // Show success message
                        refreshTable(selectedSKU);  // Refresh the table after the update
                    } else {
                        JOptionPane.showMessageDialog(this, "No changes made.");  // Show message if no rows were updated
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid number format.");  // Show error for invalid input
            } catch (SQLException ex) {
                ex.printStackTrace();  // Print any SQL errors to the console
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());  // Show an error message
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
                     "SELECT * FROM part WHERE sku = ?")) {  // Prepare a statement to fetch the SKU data
            stmt.setString(1, selectedSKU);  // Set the SKU parameter in the query
            ResultSet rs = stmt.executeQuery();  // Execute the query

            if (rs.next()) {  // If a result is found
                // Update the table model with the new data
                tableModel.setValueAt(rs.getString("sku"), 0, 0);  // Update SKU in the table
                tableModel.setValueAt(rs.getString("description"), 0, 1);  // Update description
                tableModel.setValueAt(rs.getDouble("price"), 0, 2);  // Update price
                tableModel.setValueAt(rs.getInt("stock"), 0, 3);  // Update stock
            }
        } catch (SQLException ex) {
            ex.printStackTrace();  // Print any SQL errors to the console
            JOptionPane.showMessageDialog(this, "Error refreshing the table: " + ex.getMessage());  // Show an error message
        }
    }
}
