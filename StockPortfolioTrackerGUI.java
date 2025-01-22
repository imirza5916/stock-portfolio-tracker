import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class StockPortfolioTrackerGUI {
    private JFrame frame;
    private JTextField symbolField, sharesField, priceField;
    private DefaultTableModel tableModel;

    public StockPortfolioTrackerGUI() {
        frame = new JFrame("Stock Portfolio Tracker");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2));

        inputPanel.add(new JLabel("Stock Symbol:"));
        symbolField = new JTextField();
        inputPanel.add(symbolField);

        inputPanel.add(new JLabel("Number of Shares:"));
        sharesField = new JTextField();
        inputPanel.add(sharesField);

        inputPanel.add(new JLabel("Purchase Price:"));
        priceField = new JTextField();
        inputPanel.add(priceField);

        JButton addButton = new JButton("Add Stock");
        inputPanel.add(addButton);
        frame.add(inputPanel, BorderLayout.NORTH);

        // Table for portfolio
        String[] columns = {"Symbol", "Shares", "Purchase Price", "Current Price", "Profit/Loss"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Button Click Event
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addStock();
            }
        });

        frame.setVisible(true);
    }

    private void addStock() {
        try {
            String symbol = symbolField.getText().toUpperCase();
            double shares = Double.parseDouble(sharesField.getText()); // Now allows decimals
            double purchasePrice = Double.parseDouble(priceField.getText());
            double currentPrice = fetchStockPrice(symbol);
            
            if (currentPrice == 0.0) {
                JOptionPane.showMessageDialog(frame, "Error fetching stock price. Please try again.", "API Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            double profitLoss = (currentPrice - purchasePrice) * shares;
            
            tableModel.addRow(new Object[]{symbol, String.format("%.6f", shares), String.format("%.6f", purchasePrice), String.format("%.6f", currentPrice), String.format("%.6f", profitLoss)});
            
            // Clear input fields after successful entry
            symbolField.setText("");
            sharesField.setText("");
            priceField.setText("");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid input! Please enter valid numbers for shares and price.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double fetchStockPrice(String symbol) {
        try {
            String apiKey = "9GPIGE66V03SQCAM";
            String urlString = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (!jsonResponse.has("Global Quote")) {
                return 0.0;
            }
            JSONObject globalQuote = jsonResponse.getJSONObject("Global Quote");
            return Double.parseDouble(globalQuote.optString("05. price", "0.0"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error fetching stock price: " + e.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
            return 0.0;
        }
    }

    public static void main(String[] args) {
        new StockPortfolioTrackerGUI();
    }
}
