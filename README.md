# ⏳ Productivity Calculator CLI (Java + PostgreSQL)

A simple CLI-based productivity tracking app built in a single Java file using OOP and JDBC. Track tasks, classify them as Productive or Non-Productive, and monitor remaining productive hours each day.

---

## 📦 Features

- User login system (simple username/password)
- Add, update, delete, and view daily tasks
- Categorize tasks as `Productive` or `Non-Productive`
- View remaining productive hours (from 24 total)


## 🧰 Requirements

- Java (JDK 8+)
- PostgreSQL
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/download.html)
- Intellij or any Java IDE

## Contributors

1. Adithya V (23)
2. Sarthak Jha (24)
3. Tanmay Negi (22)
4. Harshit Kaushik (khud add kro)
5. Rohan Bhandari (khud add kro)


## 🗄️ PostgreSQL Setup

```sql
CREATE DATABASE productivity;
CREATE USER prod_user WITH PASSWORD 'secretpassword';
GRANT ALL PRIVILEGES ON DATABASE productivity TO prod_user;
```

### Create tables

```sql
-- users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL
);

-- tasks table
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    description TEXT NOT NULL,
    category TEXT CHECK (category IN ('Productive', 'Non-Productive')),
    hours DOUBLE PRECISION NOT NULL
);
```

### Build Command
```
javac -cp ".:postgresql-42.7.3.jar" ProductivityApp.java
```

### Run Command
```
java -cp ".:postgresql-42.7.3.jar" ProductivityApp 
```