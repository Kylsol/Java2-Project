package com.bushnell;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.sql.*;

public class StockReport extends JPanel {

    private final String dbPath;
    private JTable table;
    private DefaultTableModel tableModel;

    public StockReport() {
        this.dbPath = java.nio.file.Paths.get("")
            .toAbsolutePath()
            .resolve("VR-Factory.db")
            .toString();
        setLayout(new BorderLayout());

        // ===== Top panel with title and buttons =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Stock Report");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        topPanel.add(title, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export Report as PDF");
        exportButton.addActionListener(e -> exportToPDF(dbPath));


        JButton printButton = new JButton("Print Report");
        printButton.addActionListener(e -> printReport());

        JButton quickSaveButton = new JButton("Save Report");
        quickSaveButton.addActionListener(e -> quickSavePDF(dbPath));


        buttonPanel.add(exportButton);
        buttonPanel.add(printButton);
        buttonPanel.add(quickSaveButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // ===== Table setup =====
        String[] columnNames = {"SKU", "Description", "Price", "Stock"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Menlo", Font.PLAIN, 13));
        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JComponent && value != null) {
                    ((JComponent) c).setToolTipText(value.toString());
                }
                return c;
            }
        });

        // ===== Right-click to copy cell value =====
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy Cell Value");
        popupMenu.add(copyItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                table.setColumnSelectionInterval(col, col);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        copyItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            if (row != -1 && col != -1) {
                Object value = table.getValueAt(row, col);
                StringSelection selection = new StringSelection(value.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        updateReport();
    }

    private void printReport() {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintable(table.getPrintable(JTable.PrintMode.FIT_WIDTH, null, null));
    
        if (job.printDialog()) {
            try {
                job.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage());
            }
        }
    }
    
    
    private void quickSavePDF(String dbPath) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy.MM.dd-HH.mm").format(new java.util.Date());
            String fileName = "VR-StockReport-" + timestamp + ".pdf";
            java.io.File pdfFile = new java.io.File(new java.io.File(dbPath).getParentFile(), fileName);
    
            // Same PDF logic reused from exportToPDF
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(pdfFile));
            document.open();
    
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11);
    
            int rowsPerPage = 40;
            int totalRows = table.getRowCount();
            int totalPages = (int) Math.ceil((double) totalRows / rowsPerPage);
    
            for (int page = 0; page < totalPages; page++) {
                if (page > 0) document.newPage();
    
                com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Visual Robotics Stock Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(title);
    
                String dateStr = new java.text.SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new java.util.Date());
                com.itextpdf.text.Paragraph meta = new com.itextpdf.text.Paragraph("Generated: " + dateStr + "   |   Page " + (page + 1) + " of " + totalPages, bodyFont);
                meta.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                meta.setSpacingAfter(10);
                document.add(meta);
    
                com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(table.getColumnCount());
                pdfTable.setWidthPercentage(100);
    
                for (int i = 0; i < table.getColumnCount(); i++) {
                    pdfTable.addCell(new com.itextpdf.text.Phrase(table.getColumnName(i), headerFont));
                }
    
                com.itextpdf.text.pdf.PdfPCell underline = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(" "));
                underline.setColspan(table.getColumnCount());
                underline.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
                pdfTable.addCell(underline);
    
                int startRow = page * rowsPerPage;
                int endRow = Math.min(startRow + rowsPerPage, totalRows);
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object value = table.getValueAt(row, col);
                        pdfTable.addCell(new com.itextpdf.text.Phrase(value != null ? value.toString() : "", bodyFont));
                    }
                }
    
                document.add(pdfTable);
            }
    
            document.close();
            JOptionPane.showMessageDialog(this, "Report saved as:\n" + pdfFile.getAbsolutePath());
    
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save PDF: " + e.getMessage());
        }
    }
    
    
    private void exportToPDF(String dbPath) {
        try {
            String defaultFileName = "VR-StockReport.pdf";
    
            // === Get database directory ===
            java.io.File dbDir = new java.io.File(dbPath).getParentFile();
    
            // === Setup JFileChooser to that directory ===
            JFileChooser fileChooser = new JFileChooser(dbDir);
            fileChooser.setSelectedFile(new java.io.File(dbDir, defaultFileName));
            fileChooser.setDialogTitle("Save Stock Report PDF");
    
            int result = fileChooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;
    
            // === Get selected file and ensure it ends in .pdf ===
            java.io.File pdfFile = fileChooser.getSelectedFile();
            if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
                pdfFile = new java.io.File(pdfFile.getAbsolutePath() + ".pdf");
            }
    
            // === Begin PDF creation ===
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(pdfFile));
            document.open();
    
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font bodyFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11);
    
            int rowsPerPage = 40;
            int totalRows = table.getRowCount();
            int totalPages = (int) Math.ceil((double) totalRows / rowsPerPage);
    
            for (int page = 0; page < totalPages; page++) {
                if (page > 0) document.newPage();
    
                // Title
                com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Visual Robotics Stock Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(title);
    
                // Timestamp & Page number
                String dateStr = new java.text.SimpleDateFormat("MMMM dd, yyyy HH:mm").format(new java.util.Date());
                com.itextpdf.text.Paragraph meta = new com.itextpdf.text.Paragraph("Generated: " + dateStr + "   |   Page " + (page + 1) + " of " + totalPages, bodyFont);
                meta.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                meta.setSpacingAfter(10);
                document.add(meta);
    
                // Table headers
                com.itextpdf.text.pdf.PdfPTable pdfTable = new com.itextpdf.text.pdf.PdfPTable(table.getColumnCount());
                pdfTable.setWidthPercentage(100);
    
                for (int i = 0; i < table.getColumnCount(); i++) {
                    pdfTable.addCell(new com.itextpdf.text.Phrase(table.getColumnName(i), headerFont));
                }
    
                // Underline
                com.itextpdf.text.pdf.PdfPCell underline = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(" "));
                underline.setColspan(table.getColumnCount());
                underline.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
                pdfTable.addCell(underline);
    
                // Rows
                int startRow = page * rowsPerPage;
                int endRow = Math.min(startRow + rowsPerPage, totalRows);
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object value = table.getValueAt(row, col);
                        pdfTable.addCell(new com.itextpdf.text.Phrase(value != null ? value.toString() : "", bodyFont));
                    }
                }
    
                document.add(pdfTable);
            }
    
            document.close();
            JOptionPane.showMessageDialog(this, "PDF saved to:\n" + pdfFile.getAbsolutePath());
    
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export PDF: " + e.getMessage());
        }
    }
    
    
     

    public void updateReport() {
        tableModel.setRowCount(0);
    
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sku, description, price, stock FROM part")) {
    
            while (rs.next()) {
                String sku = rs.getString("sku");
                String desc = rs.getString("description");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");
    
                tableModel.addRow(new Object[]{sku, desc, String.format("%.3f", price), stock});
            }
    
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading stock report: " + e.getMessage());
        }
    }
    
}
