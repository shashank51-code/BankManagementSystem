import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BankWebServer {
    private final Bank bank;
    private final int port;
    private final Path uiDirectory;
    private HttpServer server;

    public BankWebServer(Bank bank, int port, Path uiDirectory) {
        this.bank = bank;
        this.port = port;
        this.uiDirectory = uiDirectory;
    }

    public static BankWebServer createOnAvailablePort(Bank bank, int preferredPort, Path uiDirectory) throws IOException {
        for (int candidatePort = preferredPort; candidatePort < preferredPort + 20; candidatePort++) {
            if (isPortAvailable(candidatePort)) {
                return new BankWebServer(bank, candidatePort, uiDirectory);
            }
        }
        throw new IOException("No available local port found between " + preferredPort + " and " + (preferredPort + 19) + ".");
    }

    public void start(boolean openBrowser) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::serveStaticFile);
        server.createContext("/api/summary", this::summary);
        server.createContext("/api/accounts", this::accounts);
        server.createContext("/api/transactions", this::transactions);
        server.createContext("/api/statement", this::statement);
        server.createContext("/api/deposit", exchange -> operation(exchange, this::deposit));
        server.createContext("/api/withdraw", exchange -> operation(exchange, this::withdraw));
        server.createContext("/api/transfer", exchange -> operation(exchange, this::transfer));
        server.createContext("/api/freeze", exchange -> operation(exchange, this::freeze));
        server.createContext("/api/unfreeze", exchange -> operation(exchange, this::unfreeze));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        String url = "http://localhost:" + port + "/";
        System.out.println("Advanced Bank Management System is running at " + url);
        System.out.println("Press Ctrl+C to stop the server.");
        if (openBrowser) {
            openBrowser(url);
        }
    }

    private void serveStaticFile(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain", "Method not allowed");
            return;
        }

        String requestedPath = exchange.getRequestURI().getPath();
        if ("/".equals(requestedPath)) {
            requestedPath = "/index.html";
        }

        Path file = uiDirectory.resolve(requestedPath.substring(1)).normalize();
        if (!file.startsWith(uiDirectory.normalize()) || !Files.exists(file) || Files.isDirectory(file)) {
            send(exchange, 404, "text/plain", "Not found");
            return;
        }

        send(exchange, 200, contentType(file), Files.readAllBytes(file));
    }

    private void summary(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        Bank.BankSummary summary = bank.getSummary();
        String json = "{"
                + "\"totalAccounts\":" + summary.totalAccounts() + ","
                + "\"activeAccounts\":" + summary.activeAccounts() + ","
                + "\"frozenAccounts\":" + summary.frozenAccounts() + ","
                + "\"transactionCount\":" + summary.transactionCount() + ","
                + "\"totalBalance\":" + summary.totalBalance()
                + "}";
        sendJson(exchange, 200, json);
    }

    private void accounts(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 200, accountsJson());
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            Map<String, String> form = readForm(exchange);
            try {
                String accountNumber = bank.createAccount(
                        form.get("name"),
                        form.getOrDefault("type", "Savings"),
                        parseAmount(form.get("deposit")),
                        form.get("pin")
                );
                sendJson(exchange, 201, okJson(bank.getLastMessage(), "\"account\":" + accountJson(bank.getAccount(accountNumber))));
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, errorJson(e.getMessage()));
            }
            return;
        }
        send(exchange, 405, "text/plain", "Method not allowed");
    }

    private void transactions(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "GET")) return;
        sendJson(exchange, 200, transactionsJson(bank.getAllTransactions()));
    }

    private void statement(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        Map<String, String> form = readForm(exchange);
        String accountNumber = form.get("accountNumber");
        String pin = form.get("pin");
        if (!bank.validateAccountPin(accountNumber, pin)) {
            sendJson(exchange, 400, errorJson(bank.getLastMessage()));
            return;
        }

        Account account = bank.getAccount(accountNumber);
        String json = okJson("Statement loaded.", "\"account\":" + accountJson(account)
                + ",\"transactions\":" + transactionsJson(bank.getTransactionsForAccount(accountNumber)));
        sendJson(exchange, 200, json);
    }

    private String deposit(Map<String, String> form) {
        boolean ok = bank.deposit(form.get("accountNumber"), parseAmount(form.get("amount")));
        return resultJson(ok);
    }

    private String withdraw(Map<String, String> form) {
        boolean ok = bank.withdraw(form.get("accountNumber"), form.get("pin"), parseAmount(form.get("amount")));
        return resultJson(ok);
    }

    private String transfer(Map<String, String> form) {
        boolean ok = bank.transfer(form.get("fromAccount"), form.get("toAccount"), form.get("pin"), parseAmount(form.get("amount")));
        return resultJson(ok);
    }

    private String freeze(Map<String, String> form) {
        boolean ok = bank.freezeAccount(form.get("accountNumber"), form.get("pin"));
        return resultJson(ok);
    }

    private String unfreeze(Map<String, String> form) {
        boolean ok = bank.unfreezeAccount(form.get("accountNumber"), form.get("pin"));
        return resultJson(ok);
    }

    private void operation(HttpExchange exchange, ApiOperation apiOperation) throws IOException {
        if (!requireMethod(exchange, "POST")) return;
        try {
            String json = apiOperation.handle(readForm(exchange));
            sendJson(exchange, json.contains("\"success\":true") ? 200 : 400, json);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, errorJson(e.getMessage()));
        }
    }

    private String resultJson(boolean success) {
        if (success) {
            return okJson(bank.getLastMessage(), "\"summary\":" + summaryJson());
        }
        return errorJson(bank.getLastMessage());
    }

    private String accountsJson() {
        StringBuilder builder = new StringBuilder("[");
        List<Account> accounts = bank.getAllAccounts();
        for (int i = 0; i < accounts.size(); i++) {
            if (i > 0) builder.append(",");
            builder.append(accountJson(accounts.get(i)));
        }
        return builder.append("]").toString();
    }

    private String accountJson(Account account) {
        if (account == null) return "null";
        return "{"
                + "\"accountNumber\":\"" + escape(account.getAccountNumber()) + "\","
                + "\"accountHolder\":\"" + escape(account.getAccountHolder()) + "\","
                + "\"accountType\":\"" + escape(account.getAccountType()) + "\","
                + "\"balance\":" + account.getBalance() + ","
                + "\"active\":" + account.isActive()
                + "}";
    }

    private String transactionsJson(List<Transaction> transactions) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);
            if (i > 0) builder.append(",");
            builder.append("{")
                    .append("\"transactionId\":\"").append(escape(transaction.getTransactionId())).append("\",")
                    .append("\"accountNumber\":\"").append(escape(transaction.getAccountNumber())).append("\",")
                    .append("\"type\":\"").append(escape(transaction.getType())).append("\",")
                    .append("\"amount\":").append(transaction.getAmount()).append(",")
                    .append("\"balanceAfter\":").append(transaction.getBalanceAfter()).append(",")
                    .append("\"description\":\"").append(escape(transaction.getDescription())).append("\",")
                    .append("\"dateTime\":\"").append(escape(transaction.getDateTime().toString())).append("\"")
                    .append("}");
        }
        return builder.append("]").toString();
    }

    private String summaryJson() {
        Bank.BankSummary summary = bank.getSummary();
        return "{"
                + "\"totalAccounts\":" + summary.totalAccounts() + ","
                + "\"activeAccounts\":" + summary.activeAccounts() + ","
                + "\"frozenAccounts\":" + summary.frozenAccounts() + ","
                + "\"transactionCount\":" + summary.transactionCount() + ","
                + "\"totalBalance\":" + summary.totalBalance()
                + "}";
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new LinkedHashMap<>();
        if (body.isBlank()) return form;

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            form.put(key, value);
        }
        return form;
    }

    private boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equals(exchange.getRequestMethod())) {
            return true;
        }
        send(exchange, 405, "text/plain", "Method not allowed");
        return false;
    }

    private double parseAmount(String value) {
        try {
            return Double.parseDouble(value == null ? "" : value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }
    }

    private String okJson(String message, String extraJson) {
        return "{\"success\":true,\"message\":\"" + escape(message) + "\"," + extraJson + "}";
    }

    private String errorJson(String message) {
        return "{\"success\":false,\"message\":\"" + escape(message) + "\"}";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        send(exchange, statusCode, "application/json; charset=UTF-8", json);
    }

    private void send(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        send(exchange, statusCode, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=UTF-8";
        if (name.endsWith(".css")) return "text/css; charset=UTF-8";
        if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (IOException | UnsupportedOperationException e) {
            System.out.println("Open this URL in your browser: " + url);
        }
    }

    private static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @FunctionalInterface
    private interface ApiOperation {
        String handle(Map<String, String> form);
    }
}
