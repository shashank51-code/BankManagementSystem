import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BankData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Account> accounts;
    private final List<Transaction> transactions;
    private int accountCounter;
    private int transactionCounter;

    public BankData() {
        this.accounts = new LinkedHashMap<>();
        this.transactions = new ArrayList<>();
        this.accountCounter = 1000;
        this.transactionCounter = 1;
    }

    public Map<String, Account> getAccounts() { return accounts; }
    public List<Transaction> getTransactions() { return transactions; }
    public int getAccountCounter() { return accountCounter; }
    public int getTransactionCounter() { return transactionCounter; }
    public void setAccountCounter(int accountCounter) { this.accountCounter = accountCounter; }
    public void setTransactionCounter(int transactionCounter) { this.transactionCounter = transactionCounter; }
}
