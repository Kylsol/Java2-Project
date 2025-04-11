package com.bushnell;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.sql.*;

public class StockReport extends JPanel {

    private static final String DB_PATH = "jdbc:sqlite:VR-Factory.db";
    private JTable table;
    private DefaultTableModel tableModel;

    public StockReport() {
        setLayout(new BorderLayout());

        // ===== Top panel with title and buttons =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Stock Report");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        topPanel.add(title, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("Export to PDF");
        JButton printButton = new JButton("Print Report");

        buttonPanel.add(exportButton);
        buttonPanel.add(printButton);
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

    public void updateReport() {
        tableModel.setRowCount(0);

        try (Connection conn = DriverManager.getConnection(DB_PATH);
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
