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
     * Called when a SKU is selected. Loads its description, stock,
     * and component information from the database.
     */
    private void onSKUSelected(ActionEvent e) {
        String selectedSKU = (String) skuComboBox.getSelectedItem();
        if (selectedSKU == null) return;

        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            // Get main part info
            PreparedStatement partStmt = conn.prepareStatement("SELECT description, stock FROM part WHERE sku = ?");
            partStmt.setString(1, selectedSKU);
            ResultSet partRs = partStmt.executeQuery();
            if (partRs.next()) {
                descLabel.setText("Description: " + partRs.getString("description"));
                stockLabel.setText("Stock: " + partRs.getInt("stock"));
            }

            // Clear previous table content
            tableModel.setRowCount(0);

            // Load components (children) from BOM
            PreparedStatement bomStmt = conn.prepareStatement("SELECT sku, quantity FROM bom WHERE parent_sku = ?");
            bomStmt.setString(1, selectedSKU);
            ResultSet bomRs = bomStmt.executeQuery();

            boolean canBundle = true; // Assume it's possible until proven otherwise

            while (bomRs.next()) {
                String childSKU = bomRs.getString("sku");
                int qtyRequired = bomRs.getInt("quantity");

                // Get child part stock and description
                PreparedStatement childStmt = conn.prepareStatement("SELECT description, stock FROM part WHERE sku = ?");
                childStmt.setString(1, childSKU);
                ResultSet childRs = childStmt.executeQuery();
                if (childRs.next()) {
                    String desc = childRs.getString("description");
                    int stock = childRs.getInt("stock");
                    if (stock < qtyRequired) canBundle = false; // Not enough stock for this component
                    tableModel.addRow(new Object[]{childSKU, desc, qtyRequired, stock});
                }
            }

            updateBundleButtonState(canBundle); // Enable/disable with color change
        } catch (SQLException ex) {
            showError("Failed to load SKU details", ex);
        }
    }

    /**
     * Performs the bundling operation: reduces child stock, increases parent stock.
     * Uses transactions and batch updates to ensure consistency.
     */
    private void bundle() {
        String parentSKU = (String) skuComboBox.getSelectedItem();
        if (parentSKU == null) return;

        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            conn.setAutoCommit(false); // Start transaction

            // Subtract stock from all child SKUs
            PreparedStatement updateChild = conn.prepareStatement("UPDATE part SET stock = stock - ? WHERE sku = ?");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String childSKU = (String) tableModel.getValueAt(i, 0);
                int qty = (int) tableModel.getValueAt(i, 2);
                updateChild.setInt(1, qty);
                updateChild.setString(2, childSKU);
                updateChild.addBatch(); // Batch update for performance
            }
            updateChild.executeBatch();

            // Add one to the parent SKU's stock
            PreparedStatement updateParent = conn.prepareStatement("UPDATE part SET stock = stock + 1 WHERE sku = ?");
            updateParent.setString(1, parentSKU);
            updateParent.executeUpdate();

            conn.commit(); // Commit all changes
            JOptionPane.showMessageDialog(this, "Bundling successful!");

            onSKUSelected(null); // Refresh data
        } catch (SQLException ex) {
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
