package com.bushnell;

// Import necessary libraries for GUI, database access, and PDF export
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.itextpdf.text.Document;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;

public class DemandAnalysis extends JPanel {
    // UI components and table model
    private JComboBox<String> skuComboBox;
    private JLabel descLabel;
    private JSpinner spinner;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    // Path to SQLite database
    private final String DB_PATH = "jdbc:sqlite:VR-Factory.db";

    public DemandAnalysis() {
        // Layout configuration
        setLayout(new BorderLayout(10, 10));

        // Title label
        JLabel title = new JLabel("Demand Analysis", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Input panel: SKU dropdown, description, quantity spinner
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        skuComboBox = new JComboBox<>();
        descLabel = new JLabel();
        spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        // SKU row
        JPanel skuRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skuRow.add(new JLabel("SKU:"));
        skuRow.add(skuComboBox);
        inputPanel.add(skuRow);

        // Description row
        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        descRow.add(new JLabel("Description:"));
        descRow.add(descLabel);
        inputPanel.add(descRow);

        // Quantity row
        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        qtyRow.add(new JLabel("Desired Quantity:"));
        qtyRow.add(spinner);
        inputPanel.add(qtyRow);

        add(inputPanel, BorderLayout.WEST);

        // Table to display analysis results
        tableModel = new DefaultTableModel(new String[]{"SKU", "Need", "Stock", "Description"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);

        // Cell coloring based on stock sufficiency
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
                            c.setBackground(new Color(255, 102, 102)); // Red for insufficient stock
                        } else {
                            c.setBackground(new Color(204, 255, 204)); // Green for sufficient
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

        // Export button
        JButton exportBtn = new JButton("Export PDF");
        exportBtn.addActionListener(e -> exportPDF());
        JPanel btnPanel = new JPanel();
        btnPanel.add(exportBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Populate SKU list and hook up events
        loadSubSKUs();
        skuComboBox.addActionListener(e -> {
            spinner.setValue(1);
            runAnalysis();
        });
        spinner.addChangeListener(e -> runAnalysis());
    }

    // Trigger analysis when SKU or quantity changes
    private void runAnalysis() {
        if (skuComboBox.getSelectedItem() == null || spinner.getValue() == null) return;
        calculateNeeds();
    }

    // Load SUB SKUs from the database into the dropdown
    private void loadSubSKUs() {
        try (Connection conn = DriverManager.getConnection(DB_PATH);
             PreparedStatement stmt = conn.prepareStatement("SELECT sku FROM part WHERE sku LIKE 'SUB-%'")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                skuComboBox.addItem(rs.getString("sku"));
            }
        } catch (SQLException e) {
            showError("Failed to load SUB SKUs", e);
        }
    }

    // Calculate what components and quantities are needed
    private void calculateNeeds() {
        tableModel.setRowCount(0);
        String sku = (String) skuComboBox.getSelectedItem();
        int quantity = (Integer) spinner.getValue();

        try (Connection conn = DriverManager.getConnection(DB_PATH)) {
            // Get description and current stock of selected SKU
            PreparedStatement stmt = conn.prepareStatement("SELECT description, stock FROM part WHERE sku = ?");
            stmt.setString(1, sku);
            ResultSet rs = stmt.executeQuery();
            int availableStock = 0;
            String desc = "";
            if (rs.next()) {
                desc = rs.getString("description");
                availableStock = rs.getInt("stock");
            }
            descLabel.setText(desc);

            int missingQty = quantity - availableStock;
            Map<String, Integer> needed = new HashMap<>();

            // If we don't have enough, figure out what's needed
            if (missingQty > 0) {
                getRawComponents(conn, sku, missingQty, needed);
            }

            // Add top-level SKU to the table
            tableModel.addRow(new Object[]{sku, Math.max(missingQty, 0), availableStock, desc});

            // Add all required raw components
            for (String rawSku : needed.keySet()) {
                PreparedStatement rawStmt = conn.prepareStatement("SELECT description, stock FROM part WHERE sku = ?");
                rawStmt.setString(1, rawSku);
                ResultSet rawRs = rawStmt.executeQuery();
                String rawDesc = "";
                int rawStock = 0;
                if (rawRs.next()) {
                    rawDesc = rawRs.getString("description");
                    rawStock = rawRs.getInt("stock");
                }
                tableModel.addRow(new Object[]{rawSku, needed.get(rawSku), rawStock, rawDesc});
            }

        } catch (SQLException e) {
            showError("Failed to calculate needs", e);
        }
    }

    // Recursive method to calculate what raw parts are needed to build a given SKU
    private void getRawComponents(Connection conn, String sku, int qty, Map<String, Integer> result) throws SQLException {
        if (qty <= 0) return;

        // Check if this part is raw (has no children)
        PreparedStatement rawCheck = conn.prepareStatement("SELECT COUNT(*) FROM bom WHERE parent_sku = ?");
        rawCheck.setString(1, sku);
        ResultSet rawResult = rawCheck.executeQuery();
        boolean isRaw = rawResult.next() && rawResult.getInt(1) == 0;

        if (isRaw) {
            result.put(sku, result.getOrDefault(sku, 0) + qty);
            return;
        }

        // Check stock for this component
        PreparedStatement stockStmt = conn.prepareStatement("SELECT stock FROM part WHERE sku = ?");
        stockStmt.setString(1, sku);
        ResultSet stockRs = stockStmt.executeQuery();
        int available = 0;
        if (stockRs.next()) available = stockRs.getInt("stock");

        if (available >= qty) return; // we already have enough

        int toBuild = qty - available;

        // For each child component, calculate its need recursively
        PreparedStatement bomStmt = conn.prepareStatement("SELECT sku, quantity FROM bom WHERE parent_sku = ?");
        bomStmt.setString(1, sku);
        ResultSet bomRs = bomStmt.executeQuery();

        while (bomRs.next()) {
            String childSku = bomRs.getString("sku");
            int qtyPerUnit = bomRs.getInt("quantity");
            getRawComponents(conn, childSku, toBuild * qtyPerUnit, result);
        }
    }

    // Generate a PDF report of the current table
    private void exportPDF() {
        try {
            String timestamp = new SimpleDateFormat("yyyy.MM.dd-HH.mm").format(new Date());
            String filename = "DemandAnalysis-" + timestamp + ".pdf";
            File file = new File(filename);
            String fullPath = file.getAbsolutePath();

            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            String sku = (String) skuComboBox.getSelectedItem();
            int qty = (Integer) spinner.getValue();

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

            JOptionPane.showMessageDialog(this, "PDF saved to:\n" + fullPath);
            System.out.println("PDF actually saved at: " + fullPath);

            if (Desktop.isDesktopSupported() && file.getParentFile() != null) {
                Desktop.getDesktop().open(file.getParentFile());
            }

        } catch (Exception e) {
            showError("Failed to export PDF", e);
        }
    }

    // Show error message dialog and print stack trace
    private void showError(String msg, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, msg + ":\n" + e.getMessage());
    }
}
