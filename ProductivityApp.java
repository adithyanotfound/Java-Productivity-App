import java.sql.*;
import java.util.*;

/**
 * Single‑file Productivity Calculator CLI App
 */
public class ProductivityApp {
    // ─────────────────── CONFIGURATION ───────────────────
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/my_productivity_app";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "1234";
    private static final double TOTAL_DAY_HOURS = 24.0;

    // ─────────────────────── MODELS ───────────────────────
    static class User {
        int id;
        String username, password;
        User(int id, String u, String p) { this.id=id; this.username=u; this.password=p; }
    }

    static class Task {
        int id, userId;
        String description, category;
        double hours;
        Task(int id, int uid, String d, String c, double h) {
            this.id=id; this.userId=uid; this.description=d; this.category=c; this.hours=h;
        }
    }

    // ─────────────────────── DAOs ─────────────────────────
    static class UserDao {
        public User findByUsername(String username) throws SQLException {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new User(
                      rs.getInt("id"),
                      rs.getString("username"),
                      rs.getString("password_hash")
                    );
                }
                return null;
            }
        }
        public boolean insertUser(String username, String passwordHash) throws SQLException {
            String sql = "INSERT INTO users(username,password_hash) VALUES(?,?)";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                return ps.executeUpdate()==1;
            }
        }
    }

    static class TaskDao {
        public List<Task> findByUser(int userId) throws SQLException {
            String sql = "SELECT * FROM tasks WHERE user_id = ?";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                List<Task> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Task(
                      rs.getInt("id"),
                      rs.getInt("user_id"),
                      rs.getString("description"),
                      rs.getString("category"),
                      rs.getDouble("hours")
                    ));
                }
                return list;
            }
        }
        public boolean add(Task t) throws SQLException {
            String sql = "INSERT INTO tasks(user_id,description,category,hours) VALUES(?,?,?,?)";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, t.userId);
                ps.setString(2, t.description);
                ps.setString(3, t.category);
                ps.setDouble(4, t.hours);
                return ps.executeUpdate()==1;
            }
        }
        public boolean update(Task t) throws SQLException {
            String sql = "UPDATE tasks SET description=?,category=?,hours=? WHERE id=?";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, t.description);
                ps.setString(2, t.category);
                ps.setDouble(3, t.hours);
                ps.setInt(4, t.id);
                return ps.executeUpdate()==1;
            }
        }
        public boolean delete(int taskId) throws SQLException {
            String sql = "DELETE FROM tasks WHERE id=?";
            try (Connection c = DriverManager.getConnection(DB_URL,DB_USER,DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, taskId);
                return ps.executeUpdate()==1;
            }
        }
    }

    // ─────────────────── SERVICES ─────────────────────────
    static class AuthService {
        private final UserDao userDao = new UserDao();
        private User currentUser;
        public User getCurrentUser() { return currentUser; }

        public boolean login(Scanner in) {
            System.out.println("=== Welcome to Productivity Calculator ===");
            System.out.print("Username: ");  String u = in.nextLine().trim();
            System.out.print("Password: ");  String p = in.nextLine().trim();
            try {
                User usr = userDao.findByUsername(u);
                if (usr!=null && p.equals(usr.password)) {  // replace with hash check!
                    currentUser = usr;
                    return true;
                }
                System.out.println("Invalid credentials.");
            } catch (SQLException ex) {
                System.err.println("DB error: "+ex.getMessage());
            }
            return false;
        }
    }

    static class TaskService {
        private final TaskDao dao = new TaskDao();
        public List<Task> list(int userId) { 
            try { return dao.findByUser(userId); }
            catch(SQLException e){ return Collections.emptyList(); }
        }
        public void add(Task t) {
            try { dao.add(t); System.out.println("Added."); }
            catch(SQLException e){ System.err.println("Error: "+e.getMessage()); }
        }
        public void update(Task t) {
            try { dao.update(t); System.out.println("Updated."); }
            catch(SQLException e){ System.err.println("Error: "+e.getMessage()); }
        }
        public void delete(int id) {
            try { dao.delete(id); System.out.println("Deleted."); }
            catch(SQLException e){ System.err.println("Error: "+e.getMessage()); }
        }
    }

    static class ReportService {
        public void showRemaining(List<Task> tasks) {
            double nonProd = tasks.stream()
                .filter(t->!"Productive".equalsIgnoreCase(t.category))
                .mapToDouble(t->t.hours).sum();
            System.out.printf("You have %.2f productive hours left today.%n",
                TOTAL_DAY_HOURS - nonProd);
        }
    }

    // ──────────────────── CLI MENU ────────────────────────
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        try { Class.forName("org.postgresql.Driver"); }
        catch(Exception e){ /* ignore */ }

        AuthService auth = new AuthService();
        if (!auth.login(in)) {
            System.out.println("Exiting."); return;
        }

        TaskService taskSvc     = new TaskService();
        ReportService reportSvc = new ReportService();
        User user = auth.getCurrentUser();

        while (true) {
            System.out.println("\n--- Menu ---");
            System.out.println("1) View Tasks");
            System.out.println("2) Add Task");
            System.out.println("3) Update Task");
            System.out.println("4) Delete Task");
            System.out.println("5) Show Productive Hours Left");
            System.out.println("0) Exit");
            System.out.print("Choice: ");
            String choice = in.nextLine().trim();

            switch (choice) {
                case "1":
                    List<Task> list = taskSvc.list(user.id);
                    if (list.isEmpty()) {
                        System.out.println("No tasks found.");
                    } else {
                        System.out.printf("%-4s %-20s %-12s %5s%n",
                          "ID", "Description", "Category", "Hours");
                        for (Task t : list) {
                            System.out.printf("%-4d %-20s %-12s %5.2f%n",
                              t.id, t.description, t.category, t.hours);
                        }
                    }
                    break;

                case "2":
                    System.out.print("Description: ");
                    String desc = in.nextLine();
                    System.out.print("Category [Productive/Non-Productive]: ");
                    String cat  = in.nextLine();
                    System.out.print("Hours: ");
                    double hrs  = Double.parseDouble(in.nextLine());
                    taskSvc.add(new Task(0, user.id, desc, cat, hrs));
                    break;

                case "3":
                    System.out.print("Task ID to update: ");
                    int updId = Integer.parseInt(in.nextLine());
                    System.out.print("New Description: ");
                    String nd    = in.nextLine();
                    System.out.print("New Category: ");
                    String nc    = in.nextLine();
                    System.out.print("New Hours: ");
                    double nh    = Double.parseDouble(in.nextLine());
                    taskSvc.update(new Task(updId, user.id, nd, nc, nh));
                    break;

                case "4":
                    System.out.print("Task ID to delete: ");
                    int delId = Integer.parseInt(in.nextLine());
                    taskSvc.delete(delId);
                    break;

                case "5":
                    reportSvc.showRemaining(taskSvc.list(user.id));
                    break;

                case "0":
                    System.out.println("Goodbye!"); return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
