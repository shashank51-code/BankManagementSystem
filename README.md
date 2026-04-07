# Advanced Bank Management System - Java Full Stack

A full-stack banking project built with Core Java, an embedded Java HTTP server, and a browser-based dashboard. Running the Java main class starts the backend, serves the frontend, and opens the website so every banking operation is reflected in both the UI and backend data store.

## Features

- Java main class starts the web app automatically
- Frontend dashboard connected to Java backend APIs
- Create Savings and Current accounts
- Deposit, withdraw, and transfer money
- Secure balance check and mini statement with PIN verification
- PINs are stored as SHA-256 hashes instead of plain text
- Persistent data storage using Java serialization
- Account freeze and unfreeze workflow
- Search accounts by account number, holder name, or account type
- Bank summary report with total balance and account counts
- CSV statement export for individual accounts
- Demo accounts are seeded on first run
- Account and transaction dashboard
- Professional GitHub-ready structure with CI compile check

## Demo Accounts

| Account | Holder | Type | PIN |
|---|---|---|---|
| ACC1001 | Ravi Kumar | Savings | 1234 |
| ACC1002 | Priya Sharma | Current | 5678 |
| ACC1003 | Kiran Rao | Savings | 4321 |

## Project Structure

```text
BankManagementSystem/
|-- .github/
|   `-- workflows/
|       `-- java-ci.yml
|-- src/
|   |-- Account.java
|   |-- Bank.java
|   |-- BankData.java
|   |-- BankDataStore.java
|   |-- BankWebServer.java
|   |-- BankManagementSystem.java
|   `-- Transaction.java
|-- ui/
|   `-- index.html
|-- HOW_TO_RUN.txt
|-- README.md
`-- .gitignore
```

## Run The Full-Stack Web App

From the project root:

```powershell
cd "C:\Users\shash\Documents\New project\BankManagementSystem"
javac src\*.java
java -cp src BankManagementSystem
```

The app starts on the first free port from `8080` to `8099` and opens the browser automatically.

If you do not want the browser to open automatically:

```powershell
java -cp src BankManagementSystem --no-browser
```

## Run The Console App

```powershell
java -cp src BankManagementSystem --console
```

The backend automatically creates `data/bank-data.ser` when it saves bank data.

## Open The Web UI

Do not open `ui/index.html` directly for backend-connected mode. Start the Java app and use the URL it prints, for example:

```text
http://localhost:8080/
```

The frontend calls Java API endpoints such as `/api/accounts`, `/api/deposit`, `/api/transfer`, and `/api/statement`.

## Generated Files

These are intentionally ignored by Git:

- `*.class`
- `data/`
- `exports/`
- IDE metadata such as `.idea/`

## Suggested Next-Level Additions

- Convert the console backend into a REST API using Spring Boot
- Replace Java serialization with SQLite or MySQL
- Add JUnit tests for banking operations
- Add admin login and role-based access control
