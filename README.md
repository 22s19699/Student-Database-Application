# Student-Database-App
Console-based Java (JDBC + MySQL) Student Database — single-class CLI app with CRUD + search, using PreparedStatements, simple validation, and try-with-resources.

Student Database (Java + MySQL) — Minimal README
Requirements

Java 8+ (JDK)

MySQL 5.7/8.x installed and running

MySQL Connector/J JAR (e.g., mysql-connector-j-8.4.0.jar)

1) Create the Database

Run queries.sql (already in the project):

Option A – Workbench

Open Workbench → File → Open SQL Script → select queries.sql → Run.

Option B – Terminal

mysql -u root -p < queries.sql


This creates the DB StudentInformationDatabase and the students table.

2) Configure the App

Open StudentApp.java and set your credentials:

private static final String DB_URL  = "jdbc:mysql://localhost:3306/StudentInformationDatabase?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String DB_USER = "root";  
private static final String DB_PASS = "1234";      

3) Add the MySQL Driver

IntelliJ: File → Project Structure → Modules → Dependencies → + JARs → pick mysql-connector-j-*.jar → Apply.

4) Run the Program

In IntelliJ: Right-click StudentApp.java → Run 'StudentApp.main()'.

5) How to Use

When the menu appears, type a number and press Enter:

1 Add student
2 View all
3 Update by Reg No
4 Delete by Reg No
5 Search by name
0 Exit

Use Registration No (e.g., IT3003-001) for update/delete. GPA accepts 0–4.



