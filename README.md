# Basic Bank Management System

A full-stack banking project built with Core Java, an embedded Java HTTP server, and a browser-based dashboard. Running the Java main class starts the backend, serves the frontend, and opens the website. Users should perform all banking work from the website; every operation is saved by the Java backend data store.

## Features

- Java main class starts the web app automatically
- Frontend dashboard connected to Java backend APIs
- Create Savings and Current accounts
- Deposit, withdraw, and transfer money
- Secure balance check and mini statement with PIN verification
- Search accounts from the website
- Download account statement CSV from the website
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


- `*.class`
- `data/`
- `exports/`
- IDE metadata such as `.idea/`

