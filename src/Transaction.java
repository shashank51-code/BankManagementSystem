import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String transactionId;
    private final String accountNumber;
    private final String type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final LocalDateTime dateTime;

    public Transaction(String transactionId, String accountNumber, String type, double amount, double balanceAfter, String description) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description == null ? "-" : description;
        this.dateTime = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountNumber() { return accountNumber; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public LocalDateTime getDateTime() { return dateTime; }

    public void display() {
        System.out.printf("| %-10s | %-10s | %-13s | Rs.%-10.2f | Rs.%-10.2f | %-19s | %-22s |%n",
                transactionId, accountNumber, type, amount, balanceAfter, formattedDate(), description);
    }

    public String toCsvRow() {
        return String.format("%s,%s,%s,%.2f,%.2f,%s,%s",
                transactionId, accountNumber, type, amount, balanceAfter, formattedDate(), escape(description));
    }

    private String formattedDate() {
        return dateTime.format(DISPLAY_FORMAT);
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
