import java.nio.file.Path;
import java.util.Scanner;

public class BankManagementSystem {
    private static final Bank bank = new Bank();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        if (args.length == 0 || !"--console".equalsIgnoreCase(args[0])) {
            boolean openBrowser = args.length == 0 || !"--no-browser".equalsIgnoreCase(args[0]);
            startWebApplication(openBrowser);
            return;
        }

        printWelcome();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1 -> createAccount();
                case 2 -> deposit();
                case 3 -> withdraw();
                case 4 -> transfer();
                case 5 -> checkBalance();
                case 6 -> miniStatement();
                case 7 -> bank.listAllAccounts();
                case 8 -> searchAccounts();
                case 9 -> freezeOrUnfreeze();
                case 10 -> bank.showBankSummary();
                case 11 -> exportStatement();
                case 12 -> {
                    System.out.println("\nThank you for using Java Bank System. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please select 1-12.");
            }
        }
        sc.close();
    }

    private static void startWebApplication(boolean openBrowser) {
        try {
            Path uiPath = Path.of("ui");
            BankWebServer server = BankWebServer.createOnAvailablePort(bank, 8080, uiPath);
            server.start(openBrowser);
        } catch (Exception e) {
            System.out.println("Unable to start web application: " + e.getMessage());
            System.out.println("Tip: make sure you run from the project root.");
        }
    }

    private static void printWelcome() {
        System.out.println("========================================");
        System.out.println("      ADVANCED JAVA BANK SYSTEM");
        System.out.println("========================================");
        System.out.println("Data is saved automatically in data/bank-data.ser");
    }

    private static void printMenu() {
        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("  1. Create New Account");
        System.out.println("  2. Deposit Money");
        System.out.println("  3. Withdraw Money");
        System.out.println("  4. Transfer Money");
        System.out.println("  5. Check Balance");
        System.out.println("  6. Mini Statement");
        System.out.println("  7. View All Accounts");
        System.out.println("  8. Search Accounts");
        System.out.println("  9. Freeze / Unfreeze Account");
        System.out.println(" 10. Bank Summary");
        System.out.println(" 11. Export Statement CSV");
        System.out.println(" 12. Exit");
        System.out.println("================================");
    }

    private static void createAccount() {
        System.out.println("\n--- Create New Account ---");
        String name = readText("Enter account holder name: ");

        System.out.println("Account Type: 1. Savings  2. Current");
        String typeChoice = readText("Choose (1/2): ");
        String type = typeChoice.equals("2") ? "Current" : "Savings";

        double deposit = readDouble("Initial deposit amount (minimum Rs. 500): ");
        String pin = readText("Set 4-digit PIN: ");

        try {
            bank.createAccount(name, type, deposit, pin);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void deposit() {
        System.out.println("\n--- Deposit Money ---");
        String accNo = readText("Enter account number: ");
        double amount = readDouble("Enter amount to deposit: ");
        bank.deposit(accNo, amount);
    }

    private static void withdraw() {
        System.out.println("\n--- Withdraw Money ---");
        String accNo = readText("Enter account number: ");
        String pin = readText("Enter PIN: ");
        double amount = readDouble("Enter amount to withdraw: ");
        bank.withdraw(accNo, pin, amount);
    }

    private static void transfer() {
        System.out.println("\n--- Transfer Money ---");
        String from = readText("Enter your account number: ");
        String to = readText("Enter recipient account number: ");
        String pin = readText("Enter your PIN: ");
        double amount = readDouble("Enter amount to transfer: ");
        bank.transfer(from, to, pin, amount);
    }

    private static void checkBalance() {
        System.out.println("\n--- Check Balance ---");
        String accNo = readText("Enter account number: ");
        String pin = readText("Enter PIN: ");
        bank.checkBalance(accNo, pin);
    }

    private static void miniStatement() {
        System.out.println("\n--- Mini Statement ---");
        String accNo = readText("Enter account number: ");
        String pin = readText("Enter PIN: ");
        bank.showMiniStatement(accNo, pin);
    }

    private static void searchAccounts() {
        System.out.println("\n--- Search Accounts ---");
        String keyword = readText("Enter name, account number, or account type: ");
        bank.searchAccounts(keyword);
    }

    private static void freezeOrUnfreeze() {
        System.out.println("\n--- Freeze / Unfreeze Account ---");
        String accNo = readText("Enter account number: ");
        String pin = readText("Enter PIN: ");
        System.out.println("1. Freeze account");
        System.out.println("2. Unfreeze account");
        int choice = readInt("Choose (1/2): ");

        if (choice == 1) {
            bank.freezeAccount(accNo, pin);
        } else if (choice == 2) {
            bank.unfreezeAccount(accNo, pin);
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void exportStatement() {
        System.out.println("\n--- Export Statement CSV ---");
        String accNo = readText("Enter account number: ");
        String pin = readText("Enter PIN: ");
        String fileName = readText("Output file name (default: statement.csv): ");
        if (fileName.isBlank()) {
            fileName = "statement.csv";
        }
        bank.exportStatement(accNo, pin, Path.of("exports", fileName));
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readText(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                double value = Double.parseDouble(readText(prompt));
                if (Double.isFinite(value)) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid amount. Please enter a valid number.");
        }
    }
}
