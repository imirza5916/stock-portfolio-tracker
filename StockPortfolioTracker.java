import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class StockPortfolioTracker extends JFrame {
    
    // Colors - Dark theme
    private static final Color BG_PRIMARY = new Color(15, 15, 20);
    private static final Color BG_CARD = new Color(30, 30, 42);
    private static final Color BG_INPUT = new Color(20, 20, 28);
    private static final Color BORDER_COLOR = new Color(50, 50, 65);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(156, 163, 175);
    private static final Color ACCENT = new Color(0, 212, 170);
    private static final Color ACCENT_SECONDARY = new Color(0, 168, 204);
    private static final Color POSITIVE = new Color(0, 212, 170);
    private static final Color NEGATIVE = new Color(255, 71, 87);
    
    // Data
    private List<Stock> portfolio = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable table;
    
    // Summary labels
    private JLabel totalValueLabel;
    private JLabel totalInvestedLabel;
    private JLabel totalPLLabel;
    private JLabel totalPLPctLabel;
    private JLabel positionCountLabel;
    
    // Input fields
    private JTextField symbolField;
    private JTextField sharesField;
    private JTextField priceField;
    private JButton addButton;
    
    // Allocation panel
    private AllocationChart allocationChart;
    
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new StockPortfolioTracker().setVisible(true);
        });
    }
    
    public StockPortfolioTracker() {
        setTitle("📈 Stock Portfolio Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setBackground(BG_PRIMARY);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(BG_PRIMARY);
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        
        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        
        // Center content
        JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.setBackground(BG_PRIMARY);
        
        // Summary cards
        centerPanel.add(createSummaryCards(), BorderLayout.NORTH);
        
        // Main content area
        JPanel contentArea = new JPanel(new BorderLayout(20, 0));
        contentArea.setBackground(BG_PRIMARY);
        
        // Left - Table
        contentArea.add(createTablePanel(), BorderLayout.CENTER);
        
        // Right - Form + Chart
        contentArea.add(createRightPanel(), BorderLayout.EAST);
        
        centerPanel.add(contentArea, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PRIMARY);
        
        JLabel title = new JLabel("📈 Portfolio Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(ACCENT);
        
        JButton refreshBtn = createButton("↻ Refresh Prices", false);
        refreshBtn.addActionListener(e -> refreshAllPrices());
        
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createSummaryCards() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 20, 0));
        cards.setBackground(BG_PRIMARY);
        cards.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        // Total Value
        JPanel valueCard = createSummaryCard("TOTAL VALUE", true);
        totalValueLabel = (JLabel) ((JPanel) valueCard.getComponent(0)).getComponent(1);
        
        // Total Invested
        JPanel investedCard = createSummaryCard("TOTAL INVESTED", false);
        totalInvestedLabel = (JLabel) ((JPanel) investedCard.getComponent(0)).getComponent(1);
        
        // Total P/L
        JPanel plCard = createSummaryCard("TOTAL P/L", false);
        JPanel plContent = (JPanel) plCard.getComponent(0);
        totalPLLabel = (JLabel) plContent.getComponent(1);
        totalPLPctLabel = new JLabel("--");
        totalPLPctLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        totalPLPctLabel.setForeground(TEXT_SECONDARY);
        plContent.add(totalPLPctLabel);
        
        // Positions
        JPanel positionsCard = createSummaryCard("POSITIONS", false);
        positionCountLabel = (JLabel) ((JPanel) positionsCard.getComponent(0)).getComponent(1);
        
        cards.add(valueCard);
        cards.add(investedCard);
        cards.add(plCard);
        cards.add(positionsCard);
        
        return cards;
    }
    
    private JPanel createSummaryCard(String title, boolean highlight) {
        JPanel card = new RoundedPanel(16, highlight ? new Color(10, 42, 42) : BG_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel valueLabel = new JLabel("$0.00");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        
        content.add(titleLabel);
        content.add(valueLabel);
        
        card.add(content, BorderLayout.CENTER);
        return card;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new RoundedPanel(16, BG_CARD);
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel panelTitle = new JLabel("💼 Holdings");
        panelTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        panelTitle.setForeground(TEXT_PRIMARY);
        
        // Table
        String[] columns = {"Stock", "Shares", "Price", "Value", "P/L", ""};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only delete button column
            }
        };
        
        table = new JTable(tableModel);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(60);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(40, 40, 55));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_CARD);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setReorderingAllowed(false);
        
        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(50);
        
        // Custom renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new StockCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new PriceCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new PLCellRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new DeleteButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new DeleteButtonEditor());
        
        // Default renderer for other columns
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(isSelected ? new Color(40, 40, 55) : BG_CARD);
                setForeground(TEXT_PRIMARY);
                setFont(new Font("SansSerif", Font.BOLD, 14));
                setBorder(new EmptyBorder(0, 16, 0, 16));
                return this;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(defaultRenderer);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_CARD);
        
        panel.add(panelTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PRIMARY);
        panel.setPreferredSize(new Dimension(350, 0));
        
        // Add Stock Form
        JPanel formPanel = new RoundedPanel(16, BG_CARD);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        formPanel.setMaximumSize(new Dimension(350, 280));
        
        JLabel formTitle = new JLabel("➕ Add Position");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        formTitle.setForeground(TEXT_PRIMARY);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        symbolField = createStyledTextField("e.g. AAPL, TSLA, GOOGL");
        sharesField = createStyledTextField("Number of shares");
        priceField = createStyledTextField("Purchase price");
        
        JPanel symbolGroup = createFormGroup("STOCK SYMBOL", symbolField);
        
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(createFormGroup("SHARES", sharesField));
        row.add(createFormGroup("AVG COST", priceField));
        
        addButton = createButton("Add to Portfolio", true);
        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        addButton.addActionListener(e -> addStock());
        
        // Enter key support
        symbolField.addActionListener(e -> sharesField.requestFocus());
        sharesField.addActionListener(e -> priceField.requestFocus());
        priceField.addActionListener(e -> addStock());
        
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(16));
        formPanel.add(symbolGroup);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(row);
        formPanel.add(Box.createVerticalStrut(16));
        formPanel.add(addButton);
        
        // Allocation Chart
        JPanel chartPanel = new RoundedPanel(16, BG_CARD);
        chartPanel.setLayout(new BorderLayout());
        chartPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel chartTitle = new JLabel("🥧 Allocation");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        chartTitle.setForeground(TEXT_PRIMARY);
        
        allocationChart = new AllocationChart();
        allocationChart.setPreferredSize(new Dimension(300, 250));
        
        chartPanel.add(chartTitle, BorderLayout.NORTH);
        chartPanel.add(allocationChart, BorderLayout.CENTER);
        
        panel.add(formPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(chartPanel);
        
        return panel;
    }
    
    private JPanel createFormGroup(String label, JTextField field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        
        group.add(lbl);
        group.add(Box.createVerticalStrut(6));
        group.add(field);
        
        return group;
    }
    
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(74, 74, 90));
                    g2.setFont(getFont());
                    g2.drawString(placeholder, getInsets().left, 
                        (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, BORDER_COLOR),
            new EmptyBorder(12, 14, 12, 14)
        ));
        return field;
    }
    
    private JButton createButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (primary) {
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT, getWidth(), 0, ACCENT_SECONDARY);
                    g2.setPaint(gp);
                } else {
                    g2.setColor(BG_CARD);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(primary ? BG_PRIMARY : TEXT_PRIMARY);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(primary ? BG_PRIMARY : TEXT_PRIMARY);
        btn.setPreferredSize(new Dimension(150, 45));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    private void addStock() {
        String symbol = symbolField.getText().toUpperCase().trim();
        String sharesText = sharesField.getText().trim();
        String priceText = priceField.getText().trim();
        
        if (symbol.isEmpty()) {
            showError("Please enter a stock symbol");
            return;
        }
        
        double shares, price;
        try {
            shares = Double.parseDouble(sharesText);
            price = Double.parseDouble(priceText);
            if (shares <= 0 || price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for shares and price");
            return;
        }
        
        addButton.setEnabled(false);
        addButton.setText("Loading...");
        
        // Fetch in background
        new SwingWorker<Stock, Void>() {
            @Override
            protected Stock doInBackground() {
                return fetchStockData(symbol, shares, price);
            }
            
            @Override
            protected void done() {
                try {
                    Stock stock = get();
                    if (stock == null) {
                        showError("Could not find stock: " + symbol);
                    } else {
                        // Check if exists
                        Stock existing = null;
                        for (Stock s : portfolio) {
                            if (s.symbol.equals(symbol)) {
                                existing = s;
                                break;
                            }
                        }
                        
                        if (existing != null) {
                            // Update existing
                            double totalShares = existing.shares + shares;
                            double totalCost = (existing.shares * existing.purchasePrice) + (shares * price);
                            existing.shares = totalShares;
                            existing.purchasePrice = totalCost / totalShares;
                            existing.currentPrice = stock.currentPrice;
                            existing.change = stock.change;
                            existing.changePct = stock.changePct;
                            existing.name = stock.name;
                        } else {
                            portfolio.add(stock);
                        }
                        
                        refreshTable();
                        updateSummary();
                        allocationChart.repaint();
                        
                        // Clear fields
                        symbolField.setText("");
                        sharesField.setText("");
                        priceField.setText("");
                        symbolField.requestFocus();
                    }
                } catch (Exception e) {
                    showError("Error fetching stock data");
                }
                
                addButton.setEnabled(true);
                addButton.setText("Add to Portfolio");
            }
        }.execute();
    }
    
    private void refreshAllPrices() {
        for (Stock stock : portfolio) {
            new SwingWorker<double[], Void>() {
                @Override
                protected double[] doInBackground() {
                    return fetchPrice(stock.symbol);
                }
                
                @Override
                protected void done() {
                    try {
                        double[] data = get();
                        if (data != null) {
                            stock.currentPrice = data[0];
                            stock.change = data[1];
                            stock.changePct = data[2];
                            refreshTable();
                            updateSummary();
                            allocationChart.repaint();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }
    }
    
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Stock stock : portfolio) {
            tableModel.addRow(new Object[]{
                stock,
                String.format("%.2f", stock.shares),
                stock,
                String.format("$%,.2f", stock.getCurrentValue()),
                stock,
                "✕"
            });
        }
    }
    
    private void updateSummary() {
        double totalValue = 0;
        double totalInvested = 0;
        
        for (Stock stock : portfolio) {
            totalValue += stock.getCurrentValue();
            totalInvested += stock.shares * stock.purchasePrice;
        }
        
        double totalPL = totalValue - totalInvested;
        double totalPLPct = totalInvested > 0 ? (totalPL / totalInvested) * 100 : 0;
        
        totalValueLabel.setText(String.format("$%,.2f", totalValue));
        totalInvestedLabel.setText(String.format("$%,.2f", totalInvested));
        positionCountLabel.setText(String.valueOf(portfolio.size()));
        
        String sign = totalPL >= 0 ? "+" : "";
        totalPLLabel.setText(String.format("%s$%,.2f", sign, Math.abs(totalPL)));
        totalPLLabel.setForeground(totalPL >= 0 ? POSITIVE : NEGATIVE);
        totalPLPctLabel.setText(String.format("%s%.2f%%", sign, Math.abs(totalPLPct)));
        totalPLPctLabel.setForeground(totalPL >= 0 ? POSITIVE : NEGATIVE);
    }
    
    private Stock fetchStockData(String symbol, double shares, double price) {
        try {
            String urlString = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1d";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject json = new JSONObject(response.toString());
            JSONObject chart = json.getJSONObject("chart");
            
            if (chart.has("result") && !chart.isNull("result")) {
                JSONObject result = chart.getJSONArray("result").getJSONObject(0);
                JSONObject meta = result.getJSONObject("meta");
                
                double currentPrice = meta.getDouble("regularMarketPrice");
                double prevClose = meta.optDouble("previousClose", currentPrice);
                double change = currentPrice - prevClose;
                double changePct = prevClose > 0 ? (change / prevClose) * 100 : 0;
                String name = meta.optString("shortName", symbol);
                
                Stock stock = new Stock(symbol, shares, price);
                stock.currentPrice = currentPrice;
                stock.change = change;
                stock.changePct = changePct;
                stock.name = name;
                return stock;
            }
        } catch (Exception e) {
            System.err.println("Error fetching " + symbol + ": " + e.getMessage());
        }
        return null;
    }
    
    private double[] fetchPrice(String symbol) {
        try {
            String urlString = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1d";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject json = new JSONObject(response.toString());
            JSONObject meta = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta");
            
            double currentPrice = meta.getDouble("regularMarketPrice");
            double prevClose = meta.optDouble("previousClose", currentPrice);
            double change = currentPrice - prevClose;
            double changePct = prevClose > 0 ? (change / prevClose) * 100 : 0;
            
            return new double[]{currentPrice, change, changePct};
        } catch (Exception e) {
            return null;
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // ==================== Inner Classes ====================
    
    // Stock class
    static class Stock {
        String symbol;
        String name;
        double shares;
        double purchasePrice;
        double currentPrice;
        double change;
        double changePct;
        
        Stock(String symbol, double shares, double purchasePrice) {
            this.symbol = symbol;
            this.name = symbol;
            this.shares = shares;
            this.purchasePrice = purchasePrice;
        }
        
        double getCurrentValue() {
            return shares * currentPrice;
        }
        
        double getProfitLoss() {
            return getCurrentValue() - (shares * purchasePrice);
        }
        
        double getProfitLossPct() {
            double invested = shares * purchasePrice;
            return invested > 0 ? (getProfitLoss() / invested) * 100 : 0;
        }
    }
    
    // Rounded panel
    static class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;
        
        RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    // Rounded border
    static class RoundedBorder extends AbstractBorder {
        private int radius;
        private Color color;
        
        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }
    
    // Stock cell renderer
    class StockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(isSelected ? new Color(40, 40, 55) : BG_CARD);
            panel.setBorder(new EmptyBorder(10, 16, 10, 16));
            
            if (value instanceof Stock) {
                Stock stock = (Stock) value;
                
                JLabel symbolLabel = new JLabel(stock.symbol);
                symbolLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
                symbolLabel.setForeground(TEXT_PRIMARY);
                
                JLabel nameLabel = new JLabel(stock.name);
                nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
                nameLabel.setForeground(TEXT_SECONDARY);
                
                panel.add(symbolLabel);
                panel.add(nameLabel);
            }
            
            return panel;
        }
    }
    
    // Price cell renderer
    class PriceCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(isSelected ? new Color(40, 40, 55) : BG_CARD);
            panel.setBorder(new EmptyBorder(10, 16, 10, 16));
            
            if (value instanceof Stock) {
                Stock stock = (Stock) value;
                
                JLabel priceLabel = new JLabel(String.format("$%.2f", stock.currentPrice));
                priceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
                priceLabel.setForeground(TEXT_PRIMARY);
                
                String sign = stock.change >= 0 ? "+" : "";
                JLabel changeLabel = new JLabel(String.format("%s%.2f (%.2f%%)", sign, stock.change, stock.changePct));
                changeLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
                changeLabel.setForeground(stock.change >= 0 ? POSITIVE : NEGATIVE);
                
                panel.add(priceLabel);
                panel.add(changeLabel);
            }
            
            return panel;
        }
    }
    
    // P/L cell renderer
    class PLCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(isSelected ? new Color(40, 40, 55) : BG_CARD);
            panel.setBorder(new EmptyBorder(10, 16, 10, 16));
            
            if (value instanceof Stock) {
                Stock stock = (Stock) value;
                double pl = stock.getProfitLoss();
                double plPct = stock.getProfitLossPct();
                
                String sign = pl >= 0 ? "+" : "";
                JLabel plLabel = new JLabel(String.format("%s$%.2f", sign, Math.abs(pl)));
                plLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
                plLabel.setForeground(pl >= 0 ? POSITIVE : NEGATIVE);
                
                JLabel pctLabel = new JLabel(String.format("%s%.2f%%", sign, Math.abs(plPct)));
                pctLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
                pctLabel.setForeground(pl >= 0 ? POSITIVE : NEGATIVE);
                
                panel.add(plLabel);
                panel.add(pctLabel);
            }
            
            return panel;
        }
    }
    
    // Delete button renderer
    class DeleteButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JButton btn = new JButton("✕");
            btn.setFont(new Font("SansSerif", Font.BOLD, 14));
            btn.setForeground(TEXT_SECONDARY);
            btn.setBackground(BG_CARD);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
        }
    }
    
    // Delete button editor
    class DeleteButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int row;
        
        DeleteButtonEditor() {
            super(new JCheckBox());
            button = new JButton("✕");
            button.setFont(new Font("SansSerif", Font.BOLD, 14));
            button.setForeground(NEGATIVE);
            button.setBackground(BG_CARD);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.addActionListener(e -> {
                fireEditingStopped();
                if (row >= 0 && row < portfolio.size()) {
                    portfolio.remove(row);
                    refreshTable();
                    updateSummary();
                    allocationChart.repaint();
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }
    }
    
    // Allocation pie chart
    class AllocationChart extends JPanel {
        private final Color[] COLORS = {
            new Color(0, 212, 170),
            new Color(0, 168, 204),
            new Color(102, 126, 234),
            new Color(118, 75, 162),
            new Color(240, 147, 251),
            new Color(245, 87, 108),
            new Color(255, 236, 210),
            new Color(252, 182, 159)
        };
        
        AllocationChart() {
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (portfolio.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_SECONDARY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String msg = "No positions yet";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                g2.dispose();
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            double total = 0;
            for (Stock s : portfolio) total += s.getCurrentValue();
            
            int size = Math.min(getWidth(), getHeight()) - 80;
            int x = 20;
            int y = (getHeight() - size) / 2;
            
            double startAngle = 90;
            int i = 0;
            for (Stock stock : portfolio) {
                double value = stock.getCurrentValue();
                double angle = (value / total) * 360;
                
                g2.setColor(COLORS[i % COLORS.length]);
                g2.fillArc(x, y, size, size, (int) startAngle, (int) -angle);
                
                startAngle -= angle;
                i++;
            }
            
            // Center circle (donut hole)
            int holeSize = (int) (size * 0.6);
            int holeX = x + (size - holeSize) / 2;
            int holeY = y + (size - holeSize) / 2;
            g2.setColor(BG_CARD);
            g2.fillOval(holeX, holeY, holeSize, holeSize);
            
            // Legend
            int legendX = x + size + 20;
            int legendY = y + 20;
            i = 0;
            for (Stock stock : portfolio) {
                g2.setColor(COLORS[i % COLORS.length]);
                g2.fillRoundRect(legendX, legendY + i * 25, 12, 12, 3, 3);
                
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.drawString(stock.symbol, legendX + 20, legendY + i * 25 + 10);
                
                i++;
                if (i > 6) break; // Max 7 items in legend
            }
            
            g2.dispose();
        }
    }
}
