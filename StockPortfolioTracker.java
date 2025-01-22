import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.JSONObject;

// Stock class to store stock details
class Stock {
    private String symbol;
    private int shares;
    private double purchasePrice;
    private double currentPrice;
    
    public Stock(String symbol, int shares, double purchasePrice) {
        this.symbol = symbol;
        this.shares = shares;
        this.purchasePrice = purchasePrice;
        this.currentPrice = fetchStockPrice(symbol);
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public int getShares() {
        return shares;
    }
    
    public double getPurchasePrice() {
        return purchasePrice;
    }
    
    public double getCurrentPrice() {
        return currentPrice;
    }
    
    public double getTotalInvestment() {
        return shares * purchasePrice;
    }
    
    public double getCurrentValue() {
        return shares * currentPrice;
    }
    
    public double getProfitLoss() {
        return getCurrentValue() - getTotalInvestment();
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
            JSONObject globalQuote = jsonResponse.getJSONObject("Global Quote");
            return Double.parseDouble(globalQuote.getString("05. price"));
        } catch (Exception e) {
            System.out.println("Error fetching stock price: " + e.getMessage());
            return 0.0;
        }
    }
}

// Portfolio class to manage stocks
class Portfolio {
    private ArrayList<Stock> stocks;
    
    public Portfolio() {
        stocks = new ArrayList<>();
    }
    
    public void addStock(String symbol, int shares, double price) {
        stocks.add(new Stock(symbol, shares, price));
        System.out.println("Stock added: " + symbol);
    }
    
    public void viewPortfolio() {
        if (stocks.isEmpty()) {
            System.out.println("Portfolio is empty.");
            return;
        }
        System.out.println("Your Portfolio:");
        for (Stock stock : stocks) {
            System.out.println(stock.getSymbol() + " - Shares: " + stock.getShares() + " - Purchase Price: $" + stock.getPurchasePrice() +
                               " - Current Price: $" + stock.getCurrentPrice() + " - P/L: $" + stock.getProfitLoss());
        }
    }
}

// Main class
public class StockPortfolioTracker {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Portfolio portfolio = new Portfolio();
        
        while (true) {
            System.out.println("\nStock Portfolio Tracker");
            System.out.println("1. Add Stock");
            System.out.println("2. View Portfolio");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            
            if (choice == 1) {
                System.out.print("Enter stock symbol: ");
                String symbol = scanner.next().toUpperCase();
                System.out.print("Enter number of shares: ");
                int shares = scanner.nextInt();
                System.out.print("Enter purchase price per share: ");
                double price = scanner.nextDouble();
                portfolio.addStock(symbol, shares, price);
            } else if (choice == 2) {
                portfolio.viewPortfolio();
            } else if (choice == 3) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid choice, try again.");
            }
        }
        scanner.close();
    }
}
