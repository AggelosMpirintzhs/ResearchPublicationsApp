# Research Publications App

## Description

Research Publications App is a JavaFX desktop application for exploring and analyzing research publication data from journals and conferences.

The system uses a MySQL relational database, Python ETL scripts, SQL views and JavaFX visualizations. It supports searching, filtering, reporting and chart-based analysis for venues, authors, publication years and publishers.

Main functionality includes:

- journal and conference search
- venue profiles and rankings
- author profiles
- yearly publication statistics
- article reports with filters
- line charts, bar charts and scatter plots
- publisher quartile analysis
- database-backed queries using SQL files and views


## Installation

### 1. Clone the repository

```bash
git clone <GITHUB_REPOSITORY_LINK>
```

### 2. Create the MySQL database schema

Open MySQL Workbench and run the schema script:
```sql
source sql/schema.sql;
```
Alternatively, open `schema.sql` manually in MySQL Workbench and execute it.

### 3. Run the ETL pipeline

Run the full ETL script:
```bash
python run_full_etl.py
```
This script cleans and transforms the raw data and creates the final TSV files.

### 4. Load the data into the database

Run the loading script:
```sql
source sql/project_database_dump.sql;
```
This imports the generated TSV files into the MySQL database tables.

Run the script with views:
```sql
source src/main/resources/views.sql;
```
and optionally the script with indexes for faster running:
```sql
source sql/project_indexes.sql;
```
### 5. Configure the database connection

Edit the file:
```text
src/main/resources/application.properties
```
Change `db.username` and `db.password` according to your local MySQL setup.

### 6. Run the application

Run the JavaFX application from:
```text
app.Launcher
```
## Team Members

Antoniadou Maria , Liakou Olga, Mpirintzhs Aggelos

## Requirements

Before running the project, make sure the following are installed:

- Java JDK 21 or later
- Maven
- MySQL Server
- MySQL Workbench
- Python 3.10 or later

The application was developed as a JavaFX desktop application and uses JDBC with HikariCP for database access.

## Notes

- The database backup is not stored directly in the repository because of its large size.
Backup link:

```text
https://drive.google.com/file/d/1wH1c3g-lUPS18V39-5-v3O7Oz04-iJQk/view?usp=drive_link
```
- The `application.properties` file may need local changes depending on the MySQL setup.
