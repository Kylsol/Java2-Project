package com.bushnell;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

/**
 * BundlePanel is a Swing panel that allows users to select a "SUB SKU" and view
 * its components (child SKUs), stock levels, and perform a bundling operation
 * if sufficient stock is available.
 */
public class BundlePanel extends JPanel {
    // UI components
    private JComboBox<String> skuComboBox;      // Dropdown to select SUB SKU
    private JLabel descLabel, stockLabel;       // Labels to show description and stock of selected SKU
    private JTable componentsTable;             // Table to list required components (child SKUs)
    private DefaultTableModel tableModel;       // Model backing the table
    private JButton bundleButton;               // Button to trigger the bundling process
    private static final String DB_PATH = "jdbc:sqlite:VR-Factory.db"; // Path to SQLite database

    // Constructor sets up UI
    public BundlePanel() {
        setLayout(new BorderLayout(10, 10)); // Main layout with spacing

        // Title at the top
        JLabel title = new JLabel("Bundle Assembly", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Panel containing SKU dropdown and info labels (left side)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // SKU selection row
        JPanel skuRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skuRow.add(new JLabel("Select SUB SKU:"));
        skuComboBox = new JComboBox<>();
        loadSubSKUs(); // Load SKUs from DB
        skuComboBox.addActionListener(this::onSKUSelected); // Event when SKU is selected
        skuRow.add(skuComboBox);
        topPanel.add(skuRow);

        // Description row
        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descLabel = new JLabel("Description: ");
        descRow.add(descLabel);
        topPanel.add(descRow);

        // Stock row
        JPanel stockRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stockLabel = new JLabel("Stock: ");
        stockRow.add(stockLabel);
        topPanel.add(stockRow);

        add(topPanel, BorderLayout.WEST); // Add the side panel to the left

        // Table setup for displaying components
        tableModel = new DefaultTableModel(new String[]{"SKU", "Description", "Qty Required", "Stock"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing
            }
        };
        componentsTable = new JTable(tableModel);
        componentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        componentsTable.setRowHeight(24);

        // Set preferred column widths
        componentsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // SKU
        componentsTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Description
        componentsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Qty Required
        componentsTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Stock

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        componentsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        componentsTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Add table inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(componentsTable);
        scrollPane.setPreferredSize(new Dimension(740, 400));
        add(scrollPane, BorderLayout.CENTER);

        // Bundle button at bottom
        bundleButton = new JButton("Bundle");
        updateBundleButtonState(false); // Initially disabled with gray background
        bundleButton.addActionListener(e -> bundle()); // Trigger bundle logic
        add(bundleButton, BorderLayout.SOUTH);
    }

    /**
     * Resets all UI components to their initial state.
     */
    public void resetSelection() {
        skuComboBox.setSelectedItem(null);
        descLabel.setText("Description: ");
        stockLabel.setText("Stock: ");
        tableModel.setRowCount(0);
        updateBundleButtonState(false); // Disable button and make it gray
    }

    /**
     * Loads all SUB-SKUs from the database and populates the combo box.
     */
    private void loadSubSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT sku FROM part WHERE sku LIKE 'SUB-%'");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                skuComboBox.addItem(rs.getString("sku")); // Add each SUB SKU to dropdown
            }
        } catch (SQLException e) {
            showError("Failed to load SUB SKUs", e);
        }
    }

    /**
 * Triggered when a new SKU is selected from the dropdown.
 * 
 * This method:
 * 1. Retrieves the selected SKU's description and stock.
 * 2. Loads its associated child components from the BOM (Bill of Materials).
 * 3. For each child SKU, fetches the description and current stock.
 * 4. Populates the component table with this data.
 * 5. Enables or disables the bundle button based on stock availability.
 */
private void onSKUSelected(ActionEvent e) {
    String selectedSKU = (String) skuComboBox.getSelectedItem();
    if (selectedSKU == null) return; // Exit if no SKU is selected

    try (Connection conn = DriverManager.getConnection(DB_PATH)) {
        // === Step 1: Get main part info ===
        // SQL: SELECT description, stock FROM part WHERE sku = ?
        // Explanation:
        // - Retrieves the description and stock for the selected parent SKU
        PreparedStatement partStmt = conn.prepareStatement(
            "SELECT description, stock FROM part WHERE sku = ?"
        );
        partStmt.setString(1, selectedSKU);
        ResultSet partRs = partStmt.executeQuery();

        // Display the description and stock in the UI if the SKU exists
        if (partRs.next()) {
            descLabel.setText("Description: " + partRs.getString("description"));
            stockLabel.setText("Stock: " + partRs.getInt("stock"));
        }

        // === Step 2: Clear previous component rows ===
        tableModel.setRowCount(0); // Clears the table for fresh data

        // === Step 3: Get child components from BOM ===
        // SQL: SELECT sku, quantity FROM bom WHERE parent_sku = ?
        // Explanation:
        // - Retrieves all child SKUs and quantities needed to assemble the selected parent SKU
        PreparedStatement bomStmt = conn.prepareStatement(
            "SELECT sku, quantity FROM bom WHERE parent_sku = ?"
        );
        bomStmt.setString(1, selectedSKU);
        ResultSet bomRs = bomStmt.executeQuery();

        boolean canBundle = true; // Flag to determine if bundling is possible

        // === Step 4: For each child, get description and stock ===
        while (bomRs.next()) {
            String childSKU = bomRs.getString("sku");        // SKU of component part
            int qtyRequired = bomRs.getInt("quantity");      // Quantity needed for bundle

            // SQL: SELECT description, stock FROM part WHERE sku = ?
            // Explanation:
            // - Retrieves the child part's description and stock from the parts table
            PreparedStatement childStmt = conn.prepareStatement(
                "SELECT description, stock FROM part WHERE sku = ?"
            );
            childStmt.setString(1, childSKU);
            ResultSet childRs = childStmt.executeQuery();

            if (childRs.next()) {
                String desc = childRs.getString("description");
                int stock = childRs.getInt("stock");

                // Check if there's enough stock of this component for bundling
                if (stock < qtyRequired) canBundle = false;

                // Add a row to the table: SKU | Description | Quantity Needed | Current Stock
                tableModel.addRow(new Object[]{childSKU, desc, qtyRequired, stock});
            }
        }

        // === Step 5: Enable or disable bundle button ===
        // If any child part doesn't have enough stock, disable the button
        updateBundleButtonState(canBundle);
        
    } catch (SQLException ex) {
        // Show a user-friendly error message if database query fails
        showError("Failed to load SKU details", ex);
    }
}


/**
 * Performs the bundling operation for a selected parent SKU.
 * 
 * This method:
 * 1. Subtracts the required quantity of each component (child SKU) from inventory.
 * 2. Increases the inventory of the parent SKU (the bundled product) by 1.
 * 3. Executes all changes in a single database transaction to ensure data integrity.
 */
private void bundle() {
    // Retrieve the selected parent SKU (the bundled product)
    String parentSKU = (String) skuComboBox.getSelectedItem();
    if (parentSKU == null) return; // Exit if no SKU is selected

    try (Connection conn = DriverManager.getConnection(DB_PATH)) {
        // Start a transaction to ensure all-or-nothing update behavior
        conn.setAutoCommit(false);

        // Prepare a SQL statement to subtract stock from each component
        // SQL: UPDATE part SET stock = stock - ? WHERE sku = ?
        // Explanation:
        // - "part" is the table that contains all SKUs
        // - "stock = stock - ?" subtracts the required quantity from current stock
        // - "WHERE sku = ?" ensures only the specific child SKU is updated
        PreparedStatement updateChild = conn.prepareStatement(
            "UPDATE part SET stock = stock - ? WHERE sku = ?"
        );

        // Loop through the component list (from the table model) to get each child SKU and required quantity
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String childSKU = (String) tableModel.getValueAt(i, 0); // Column 0: SKU of the child component
            int qty = (int) tableModel.getValueAt(i, 2);            // Column 2: Quantity needed for the bundle

            updateChild.setInt(1, qty);         // Set the quantity to subtract
            updateChild.setString(2, childSKU); // Set the child SKU to update
            updateChild.addBatch();             // Add to batch for efficient execution
        }
        updateChild.executeBatch(); // Execute all updates in a single batch call

        // Prepare a SQL statement to increase stock for the parent SKU
        // SQL: UPDATE part SET stock = stock + 1 WHERE sku = ?
        // Explanation:
        // - This adds 1 to the stock of the bundled (parent) product
        PreparedStatement updateParent = conn.prepareStatement(
            "UPDATE part SET stock = stock + 1 WHERE sku = ?"
        );
        updateParent.setString(1, parentSKU); // Set the parent SKU to update
        updateParent.executeUpdate();         // Apply the stock increment

        // Commit the transaction to permanently save changes
        conn.commit();

        // Inform the user that bundling was successful using a custom icon
        ImageIcon icon = new ImageIcon(getClass().getResource("/VisualRoboticsIcon.png"));
        JOptionPane.showMessageDialog(
            this,
            "Bundling successful!",
            "Message",
            JOptionPane.INFORMATION_MESSAGE,
            icon
        );

        // Refresh the UI to reflect the updated stock values
        onSKUSelected(null);

    } catch (SQLException ex) {
        // If any SQL error occurs, rollback is automatic with try-with-resources,
        // and we notify the user of the failure
        showError("Bundling failed", ex);
    }
}



    /**
     * Shows an error dialog and prints stack trace for debugging.
     */
    private void showError(String msg, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ":\n" + e.getMessage());
    }

    /**
     * Enables or disables the bundle button and updates its background color.
     * Green when enabled, gray when disabled.
     */
    private void updateBundleButtonState(boolean enabled) {
        bundleButton.setEnabled(enabled);
        bundleButton.setOpaque(true);
        bundleButton.setContentAreaFilled(true);
        bundleButton.setBorderPainted(false);
    
        if (enabled) {
            bundleButton.setBackground(new Color(0x6DC066)); // VR green
            bundleButton.setForeground(Color.WHITE);
        } else {
            bundleButton.setBackground(Color.LIGHT_GRAY);    // Disabled gray
            bundleButton.setForeground(Color.DARK_GRAY);
        }
    }
    
}
