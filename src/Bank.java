import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Bank {
    private final BankDataStore dataStore;
    private final BankData data;
    private final Map<String, Account> accounts;
    private final List<Transaction> transactions;
    private String lastMessage = "";

    public Bank() {
        this(Path.of("data", "bank-data.ser"));
    }

    public Bank(Path dataFile) {
        this.dataStore = new BankDataStore(dataFile);
        this.data = dataStore.load();
        this.accounts = data.getAccounts();
        this.transactions = data.getTransactions();

        if (accounts.isEmpty()) {
            seedDemoAccounts();
            save();
        }
    }

    public String createAccount(String name, String type, double deposit, String pin) {
        String accNo = nextAccountNumber();
        Account account = new Account(accNo, name, type, deposit, pin);
        accounts.put(accNo, account);
        logTransaction(accNo, "OPENING", deposit, account.getBalance(), "Account opened");
        save();
        lastMessage = "Account created successfully.";
        System.out.println("\nAccount created successfully.");
        System.out.println("Your account number: " + accNo);
        return accNo;
    }

    public Account getAccount(String accNo) {
        return accounts.get(normalizeAccountNumber(accNo));
    }

    public boolean deposit(String accNo, double amount) {
        Account account = getAccount(accNo);
        if (account == null) {
            return fail("Account not found.");
        }
        try {
            account.deposit(amount);
            logTransaction(account.getAccountNumber(), "DEPOSIT", amount, account.getBalance(), "Cash deposit");
            save();
            lastMessage = "Deposit completed successfully.";
            System.out.printf("Rs. %.2f deposited successfully.%n", amount);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return fail(e.getMessage());
        }
    }

    public boolean withdraw(String accNo, String pin, double amount) {
        Account account = getAccount(accNo);
        if (!isAuthorized(account, pin)) {
            return false;
        }
        try {
            account.withdraw(amount);
            logTransaction(account.getAccountNumber(), "WITHDRAW", amount, account.getBalance(), "Cash withdrawal");
            save();
            lastMessage = "Withdrawal completed successfully.";
            System.out.printf("Rs. %.2f withdrawn successfully.%n", amount);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return fail(e.getMessage());
        }
    }

    public boolean transfer(String fromAccNo, String toAccNo, String pin, double amount) {
        Account from = getAccount(fromAccNo);
        Account to = getAccount(toAccNo);
        if (!isAuthorized(from, pin)) {
            return false;
        }
        if (to == null) {
            return fail("Destination account not found.");
        }
        if (from.getAccountNumber().equals(to.getAccountNumber())) {
            return fail("Source and destination accounts must be different.");
        }

        try {
            from.withdraw(amount);
            to.deposit(amount);
            logTransaction(from.getAccountNumber(), "TRANSFER OUT", amount, from.getBalance(), "To " + to.getAccountNumber());
            logTransaction(to.getAccountNumber(), "TRANSFER IN", amount, to.getBalance(), "From " + from.getAccountNumber());
            save();
            lastMessage = "Transfer completed successfully.";
            System.out.printf("Rs. %.2f transferred to %s successfully.%n", amount, to.getAccountNumber());
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return fail(e.getMessage());
        }
    }

    public void checkBalance(String accNo, String pin) {
        Account account = getAccount(accNo);
        if (isAuthorized(account, pin)) {
            account.displayAccount();
        }
    }

    public void showMiniStatement(String accNo, String pin) {
        Account account = getAccount(accNo);
        if (!isAuthorized(account, pin)) {
            return;
        }

        System.out.println("\n========== Mini Statement for " + account.getAccountNumber() + " ==========");
        printTransactionHeader();

        List<Transaction> accountTransactions = getTransactionsFor(account.getAccountNumber());
        if (accountTransactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            accountTransactions.stream()
                    .sorted(Comparator.comparing(Transaction::getDateTime).reversed())
                    .limit(10)
                    .forEach(Transaction::display);
        }
        System.out.println("=".repeat(122));
    }

    public void listAllAccounts() {
        System.out.println("\n========== All Bank Accounts ==========");
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        accounts.values().forEach(Account::displayAccount);
    }

    public void searchAccounts(String keyword) {
        System.out.println("\n========== Search Results ==========");
        List<Account> matches = accounts.values().stream()
                .filter(account -> account.matches(keyword))
                .toList();

        if (matches.isEmpty()) {
            System.out.println("No accounts matched your search.");
            return;
        }
        matches.forEach(Account::displayAccount);
    }

    public boolean freezeAccount(String accNo, String pin) {
        Account account = getAccount(accNo);
        if (!isAuthorized(account, pin)) {
            return false;
        }
        account.freeze();
        save();
        lastMessage = "Account frozen successfully.";
        System.out.println("Account frozen successfully.");
        return true;
    }

    public boolean unfreezeAccount(String accNo, String pin) {
        Account account = getAccount(accNo);
        if (account == null) {
            return fail("Account not found.");
        }
        if (!account.validatePin(pin)) {
            return fail("Incorrect PIN.");
        }
        account.unfreeze();
        save();
        lastMessage = "Account reactivated successfully.";
        System.out.println("Account reactivated successfully.");
        return true;
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactionsForAccount(String accNo) {
        Account account = getAccount(accNo);
        if (account == null) {
            return List.of();
        }
        return new ArrayList<>(getTransactionsFor(account.getAccountNumber()));
    }

    public boolean validateAccountPin(String accNo, String pin) {
        Account account = getAccount(accNo);
        if (account == null) {
            return fail("Account not found.");
        }
        if (!account.validatePin(pin)) {
            return fail("Incorrect PIN.");
        }
        lastMessage = "PIN verified successfully.";
        return true;
    }

    public BankSummary getSummary() {
        double totalBalance = accounts.values().stream().mapToDouble(Account::getBalance).sum();
        long activeAccounts = accounts.values().stream().filter(Account::isActive).count();
        long frozenAccounts = accounts.size() - activeAccounts;
        return new BankSummary(accounts.size(), activeAccounts, frozenAccounts, transactions.size(), totalBalance);
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void showBankSummary() {
        double totalBalance = accounts.values().stream().mapToDouble(Account::getBalance).sum();
        long activeAccounts = accounts.values().stream().filter(Account::isActive).count();
        long frozenAccounts = accounts.size() - activeAccounts;

        System.out.println("\n========== Bank Summary ==========");
        System.out.println("Total accounts      : " + accounts.size());
        System.out.println("Active accounts     : " + activeAccounts);
        System.out.println("Frozen accounts     : " + frozenAccounts);
        System.out.println("Transactions logged : " + transactions.size());
        System.out.printf("Total bank balance  : Rs. %.2f%n", totalBalance);
    }

    public boolean exportStatement(String accNo, String pin, Path exportFile) {
        Account account = getAccount(accNo);
        if (!isAuthorized(account, pin)) {
            return false;
        }

        List<String> rows = getTransactionsFor(account.getAccountNumber()).stream()
                .sorted(Comparator.comparing(Transaction::getDateTime))
                .map(Transaction::toCsvRow)
                .toList();

        try {
            Files.createDirectories(exportFile.toAbsolutePath().getParent());
            Files.write(exportFile, buildCsv(rows));
            System.out.println("Statement exported to: " + exportFile.toAbsolutePath());
            return true;
        } catch (IOException e) {
            return fail("Unable to export statement: " + e.getMessage());
        }
    }

    private List<String> buildCsv(List<String> rows) {
        List<String> csv = new java.util.ArrayList<>();
        csv.add("Transaction ID,Account Number,Type,Amount,Balance After,Date Time,Description");
        csv.addAll(rows);
        return csv;
    }

    private List<Transaction> getTransactionsFor(String accountNumber) {
        return transactions.stream()
                .filter(transaction -> transaction.getAccountNumber().equals(accountNumber))
                .toList();
    }

    private boolean isAuthorized(Account account, String pin) {
        if (account == null) {
            return fail("Account not found.");
        }
        if (!account.validatePin(pin)) {
            return fail("Incorrect PIN.");
        }
        return true;
    }

    private String nextAccountNumber() {
        int next = data.getAccountCounter() + 1;
        data.setAccountCounter(next);
        return "ACC" + next;
    }

    private void logTransaction(String accNo, String type, double amount, double balance, String description) {
        String txnId = "T" + String.format("%04d", data.getTransactionCounter());
        data.setTransactionCounter(data.getTransactionCounter() + 1);
        transactions.add(new Transaction(txnId, accNo, type, amount, balance, description));
    }

    private void printTransactionHeader() {
        System.out.printf("| %-10s | %-10s | %-13s | %-13s | %-13s | %-19s | %-22s |%n",
                "Txn ID", "Acc No", "Type", "Amount", "Balance", "Date & Time", "Description");
        System.out.println("-".repeat(122));
    }

    private void seedDemoAccounts() {
        createAccountSilently("Ravi Kumar", "Savings", 10000, "1234");
        createAccountSilently("Priya Sharma", "Current", 50000, "5678");
        createAccountSilently("Kiran Rao", "Savings", 25000, "4321");
    }

    private void createAccountSilently(String name, String type, double deposit, String pin) {
        String accNo = nextAccountNumber();
        Account account = new Account(accNo, name, type, deposit, pin);
        accounts.put(accNo, account);
        logTransaction(accNo, "OPENING", deposit, account.getBalance(), "Demo account opened");
    }

    private void save() {
        dataStore.save(data);
    }

    private boolean fail(String message) {
        lastMessage = message;
        System.out.println("Error: " + message);
        return false;
    }

    private String normalizeAccountNumber(String accNo) {
        return accNo == null ? "" : accNo.trim().toUpperCase();
    }

    public record BankSummary(int totalAccounts, long activeAccounts, long frozenAccounts, int transactionCount, double totalBalance) {}
}
