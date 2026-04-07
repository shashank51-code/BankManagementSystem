import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String accountHolder;
    private final String accountType;
    private final String pinHash;
    private final LocalDateTime createdAt;
    private double balance;
    private boolean active;

    public Account(String accountNumber, String accountHolder, String accountType, double initialDeposit, String pin) {
        if (accountHolder == null || accountHolder.trim().length() < 3) {
            throw new IllegalArgumentException("Account holder name must be at least 3 characters.");
        }
        if (!"Savings".equalsIgnoreCase(accountType) && !"Current".equalsIgnoreCase(accountType)) {
            throw new IllegalArgumentException("Account type must be Savings or Current.");
        }
        if (initialDeposit < 500) {
            throw new IllegalArgumentException("Initial deposit must be at least Rs. 500.");
        }
        if (!isValidPin(pin)) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }

        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder.trim();
        this.accountType = normalizeType(accountType);
        this.balance = initialDeposit;
        this.pinHash = hashPin(accountNumber, pin);
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public String getAccountType() { return accountType; }
    public double getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }

    public boolean validatePin(String inputPin) {
        return isValidPin(inputPin) && pinHash.equals(hashPin(accountNumber, inputPin));
    }

    public void deposit(double amount) {
        ensureActive();
        validateAmount(amount);
        balance += amount;
    }

    public void withdraw(double amount) {
        ensureActive();
        validateAmount(amount);
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        balance -= amount;
    }

    public void freeze() {
        active = false;
    }

    public void unfreeze() {
        active = true;
    }

    public void displayAccount() {
        System.out.println("====================================");
        System.out.println("  Account Number : " + accountNumber);
        System.out.println("  Account Holder : " + accountHolder);
        System.out.println("  Account Type   : " + accountType);
        System.out.println("  Status         : " + (active ? "Active" : "Frozen"));
        System.out.printf("  Balance        : Rs. %.2f%n", balance);
        System.out.println("====================================");
    }

    public boolean matches(String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        return accountNumber.toLowerCase().contains(query)
                || accountHolder.toLowerCase().contains(query)
                || accountType.toLowerCase().contains(query);
    }

    private void ensureActive() {
        if (!active) {
            throw new IllegalStateException("Account is frozen.");
        }
    }

    private static void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    private static boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }

    private static String normalizeType(String type) {
        return type.equalsIgnoreCase("Current") ? "Current" : "Savings";
    }

    private static String hashPin(String accountNumber, String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((accountNumber + ":" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
