package com.bushnell;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import javax.swing.*;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatterFactory;

public class DemandAnalysis extends JPanel {
    private JComboBox<String> skuComboBox;
    private JLabel descLabel;
    private JSpinner spinner;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private final String DB_PATH = "jdbc:sqlite:VR-Factory.db";

    public DemandAnalysis() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Demand Analysis", SwingConstants.CENTER);
        title.setFont(new java.awt.Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        skuComboBox = new JComboBox<>();
        skuComboBox.setSelectedItem(null);
        descLabel = new JLabel();
        spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        spinner.addChangeListener(e -> {
            if (skuComboBox.getSelectedItem() != null && spinner.getValue() != null) {
                calculateNeeds();
            }
        });
        spinner.setPreferredSize(new Dimension(50, 20));
        JFormattedTextField spinnerTextField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        spinnerTextField.setColumns(2);
        spinnerTextField.setHorizontalAlignment(JTextField.CENTER);
        DefaultFormatterFactory factory = (DefaultFormatterFactory) spinnerTextField.getFormatterFactory();
        NumberFormatter formatter = (NumberFormatter) factory.getDefaultFormatter();
        formatter.setAllowsInvalid(false);
        spinner.setPreferredSize(new Dimension(60, 24));

        JPanel skuRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skuRow.add(new JLabel("SKU:"));
        skuRow.add(skuComboBox);
        inputPanel.add(skuRow);
        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descRow.add(new JLabel("Description:"));
        descRow.add(descLabel);
        inputPanel.add(descRow);
        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        qtyRow.add(new JLabel("Desired Quantity:"));
        qtyRow.add(spinner);
        inputPanel.add(qtyRow);

        add(inputPanel, BorderLayout.WEST);

        tableModel = new DefaultTableModel(new String[]{"SKU", "Need", "Stock", "Description"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 1 || column == 2) {
                    try {
                        int need = Integer.parseInt(table.getValueAt(row, 1).toString());
                        int stock = Integer.parseInt(table.getValueAt(row, 2).toString());
                        if (stock < need) {
                            c.setBackground(new Color(255, 102, 102));
                        } else {
                            c.setBackground(new Color(204, 255, 204));
                        }
                    } catch (Exception e) {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });
        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        loadSubSKUs();
        skuComboBox.addActionListener(e -> {
            if (skuComboBox.getSelectedItem() != null && spinner.getValue() != null) {
                onSKUSelected(e);
                calculateNeeds();
            }
        });

        JButton exportBtn = new JButton("Export PDF");
        exportBtn.addActionListener(e -> exportPDF());
        JPanel btnPanel = new JPanel();
                btnPanel.add(exportBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadSubSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT sku FROM part WHERE sku LIKE 'SUB-%'");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                skuComboBox.addItem(rs.getString("sku"));
            }
        } catch (SQLException e) {
            showError("Failed to load SUB SKUs", e);
        }
    }

    private void onSKUSelected(ActionEvent e) {
        String sku = (String) skuComboBox.getSelectedItem();
        if (sku == null) return;

        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT description FROM part WHERE sku = ?")) {
            stmt.setString(1, sku);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) descLabel.setText(rs.getString("description"));
        } catch (SQLException ex) {
            showError("Failed to load description", ex);
        }
    }

    private void calculateNeeds() {
        tableModel.setRowCount(0);
        String sku = (String) skuComboBox.getSelectedItem();
        int quantity = (Integer) spinner.getValue();
    
        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            // Step 1: Check stock of the selected SKU
            PreparedStatement stmt = conn.prepareStatement("SELECT stock FROM part WHERE sku = ?");
            stmt.setString(1, sku);
            ResultSet rs = stmt.executeQuery();
    
            int availableStock = 0;
            if (rs.next()) {
                availableStock = rs.getInt("stock");
            }
    
            // Step 2: Determine missing quantity
            int missingQty = quantity - availableStock;
    
            Map<String, Integer> needed = new HashMap<>();
    
            if (missingQty > 0) {
                // Step 3: Only decompose the missing quantity
                getRawComponents(sku, missingQty, needed);
            }
    
            // Step 4: Add the selected SKU itself (already available)
            if (availableStock >= quantity) {
                // Enough stock, no raw materials needed
                tableModel.addRow(new Object[]{sku, 0, availableStock, descLabel.getText()});
            } else {
                // Partially available
                if (availableStock > 0) {
                    tableModel.addRow(new Object[]{sku, 0, availableStock, descLabel.getText()});
                } else {
                    tableModel.addRow(new Object[]{sku, quantity, availableStock, descLabel.getText()});
                }
            }
    
            // Step 5: Load descriptions and stock for needed raw components
            for (String rawSku : needed.keySet()) {
                PreparedStatement descStmt = conn.prepareStatement("SELECT description, stock FROM part WHERE sku = ?");
                descStmt.setString(1, rawSku);
                ResultSet descRs = descStmt.executeQuery();
                String desc = "";
                int stock = 0;
                if (descRs.next()) {
                    desc = descRs.getString("description");
                    stock = descRs.getInt("stock");
                }
                tableModel.addRow(new Object[]{rawSku, needed.get(rawSku), stock, desc});
            }
        } catch (SQLException e) {
            showError("Failed to load descriptions", e);
        }
    }
    

    private void getRawComponents(String sku, int qty, Map<String, Integer> result) {
        if (qty <= 0) return; // No need if nothing missing
    
        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            // Step 1: Check if this SKU is raw (no BOM)
            PreparedStatement check = conn.prepareStatement(
                "SELECT COUNT(*) FROM bom WHERE parent_sku = ?"
            );
            check.setString(1, sku);
            ResultSet rsCheck = check.executeQuery();
            boolean isRaw = rsCheck.next() && rsCheck.getInt(1) == 0;
    
            if (isRaw) {
                // Raw part → Add quantity needed
                result.put(sku, result.getOrDefault(sku, 0) + qty);
                return;
            }
    
            // Step 2: See if we already have stock of this part
            PreparedStatement stockStmt = conn.prepareStatement(
                "SELECT stock FROM part WHERE sku = ?"
            );
            stockStmt.setString(1, sku);
            ResultSet rsStock = stockStmt.executeQuery();
            int stock = 0;
            if (rsStock.next()) {
                stock = rsStock.getInt("stock");
            }
    
            if (stock >= qty) {
                // Stock is enough, no further decomposition needed
                return;
            } else {
                // Partially available, reduce needed qty
                qty = qty - stock;
            }
    
            // Step 3: Decompose only missing quantity
            PreparedStatement bomStmt = conn.prepareStatement(
                "SELECT sku, quantity FROM bom WHERE parent_sku = ?"
            );
            bomStmt.setString(1, sku);
            ResultSet rsBOM = bomStmt.executeQuery();
    
            while (rsBOM.next()) {
                String childSku = rsBOM.getString("sku");
                int childQty = rsBOM.getInt("quantity");
                getRawComponents(childSku, qty * childQty, result);
            }
        } catch (SQLException e) {
            showError("Error while calculating BOM", e);
        }
    }
    

    private void exportPDF() {
        try {
            String timestamp = new SimpleDateFormat("yyyy.MM.dd-HH.mm").format(new java.util.Date());
            String sku = (String) skuComboBox.getSelectedItem();
            int qty = (Integer) spinner.getValue();
            String filename = "DemandAnalysis-" + timestamp + ".pdf";

            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(filename));
            doc.open();

            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12);

            doc.add(new com.itextpdf.text.Paragraph("Demand Analysis", headerFont));
            doc.add(new com.itextpdf.text.Paragraph("SKU: " + sku, bodyFont));
            doc.add(new com.itextpdf.text.Paragraph("Desired Quantity: " + qty, bodyFont));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("SKU");
            table.addCell("Need");
            table.addCell("Stock");
            table.addCell("Description");

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    table.addCell(tableModel.getValueAt(i, j).toString());
                }
            }

            doc.add(table);
            doc.close();

            JOptionPane.showMessageDialog(this, "PDF saved: " + filename);

        } catch (Exception e) {
            showError("Failed to export PDF", e);
        }
    }

    private void showError(String msg, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ":\n" + e.getMessage());
    }
}
