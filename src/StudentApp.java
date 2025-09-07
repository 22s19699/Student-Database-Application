import java.sql.*;
import java.util.Scanner;

public class StudentApp {

    private static final String DB_URL  =
            "jdbc:mysql://localhost:3306/StudentInformationDatabase?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "kavi";

    private static final Scanner SC = new Scanner(System.in);

    // Load driver once
    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found. Add mysql-connector-j to classpath.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        createTableIfNotExists(); // safe even if already created
        while (true) {
            printMenu();
            String choice = SC.nextLine().trim();
            switch (choice) {
                case "1": addStudent();       break;
                case "2": viewAll();          break;
                case "3": updateByRegNo();    break;
                case "4": deleteByRegNo();    break;
                case "5": searchByName();     break;
                case "0": System.out.println("Goodbye!"); return;
                default:  System.out.println("Invalid option. Try again.");
            }
        }
    }

    // --- Utilities ---
    private static Connection getConn() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private static void printMenu() {
        System.out.println("\n=== Student Database App ===");
        System.out.println("1. Add student");
        System.out.println("2. View all students");
        System.out.println("3. Update student (by Reg No)");
        System.out.println("4. Delete student (by Reg No)");
        System.out.println("5. Search students by name");
        System.out.println("0. Exit");
        System.out.print("Choose: ");
    }

    private static void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS students (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "reg_no VARCHAR(20) NOT NULL UNIQUE," +
                "name VARCHAR(100) NOT NULL," +
                "email VARCHAR(100)," +
                "phone VARCHAR(20)," +
                "gpa DECIMAL(3,2))";
        try (Connection con = getConn(); Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.out.println("Table init error: " + e.getMessage());
        }
    }

    private static String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("This field is required.");
        }
    }

    private static Double readGpaOrNull() {
        System.out.print("GPA (0–4, blank = skip): ");
        String s = SC.nextLine().trim();
        if (s.isEmpty()) return null;
        try {
            double g = Double.parseDouble(s);
            if (g < 0 || g > 4) { System.out.println("GPA must be 0–4. Leaving empty."); return null; }
            return g;
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid number. Leaving empty.");
            return null;
        }
    }

    // --- CRUD + Search ---
    private static void addStudent() {
        String reg   = readRequired("Registration No: ");
        String name  = readRequired("Full Name      : ");
        System.out.print("Email (optional): ");
        String email = SC.nextLine().trim();
        System.out.print("Phone (optional): ");
        String phone = SC.nextLine().trim();
        Double gpa   = readGpaOrNull();

        String sql = "INSERT INTO students(reg_no,name,email,phone,gpa) VALUES(?,?,?,?,?)";
        try (Connection con = getConn(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reg);
            ps.setString(2, name);
            if (email.isEmpty()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, email);
            if (phone.isEmpty()) ps.setNull(4, Types.VARCHAR); else ps.setString(4, phone);
            if (gpa == null)     ps.setNull(5, Types.DECIMAL);  else ps.setDouble(5, gpa);
            ps.executeUpdate();
            System.out.println("Student saved.");
        } catch (SQLException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private static void viewAll() {
        String sql = "SELECT reg_no,name,COALESCE(email,'-') email,COALESCE(phone,'-') phone,COALESCE(gpa,0) gpa " +
                "FROM students ORDER BY name";
        try (Connection con = getConn(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            System.out.println("\nRegNo\tName\tEmail\tPhone\tGPA");
            while (rs.next()) {
                System.out.printf("%s\t%s\t%s\t%s\t%.2f%n",
                        rs.getString("reg_no"), rs.getString("name"),
                        rs.getString("email"), rs.getString("phone"),
                        rs.getDouble("gpa"));
            }
        } catch (SQLException e) {
            System.out.println("View failed: " + e.getMessage());
        }
    }

    private static void updateByRegNo() {
        String reg = readRequired("Enter Reg No to update: ");
        System.out.println("Leave field empty to keep current value.");
        System.out.print("New Name : ");  String name  = SC.nextLine().trim();
        System.out.print("New Email: ");  String email = SC.nextLine().trim();
        System.out.print("New Phone: ");  String phone = SC.nextLine().trim();
        System.out.print("New GPA  : ");  String gpaS  = SC.nextLine().trim();

        String sql = "UPDATE students SET " +
                "name  = COALESCE(NULLIF(?,''), name), " +
                "email = CASE WHEN ?='' THEN email ELSE ? END, " +
                "phone = CASE WHEN ?='' THEN phone ELSE ? END, " +
                "gpa   = CASE WHEN ?='' THEN gpa ELSE ? END " +
                "WHERE reg_no = ?";
        try (Connection con = getConn(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email); ps.setString(3, email);
            ps.setString(4, phone); ps.setString(5, phone);
            ps.setString(6, gpaS);
            if (gpaS.isEmpty()) ps.setNull(7, Types.DECIMAL);
            else {
                try {
                    double g = Double.parseDouble(gpaS);
                    if (g < 0 || g > 4) { System.out.println("Invalid GPA."); return; }
                    ps.setDouble(7, g);
                } catch (NumberFormatException nfe) { System.out.println("Invalid GPA."); return; }
            }
            ps.setString(8, reg);
            int rows = ps.executeUpdate();
            System.out.println(rows == 0 ? "No student found." : "Student updated.");
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private static void deleteByRegNo() {
        String reg = readRequired("Enter Reg No to delete: ");
        String sql = "DELETE FROM students WHERE reg_no = ?";
        try (Connection con = getConn(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reg);
            int rows = ps.executeUpdate();
            System.out.println(rows == 0 ? "No student found." : "Student deleted.");
        } catch (SQLException e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    private static void searchByName() {
        System.out.print("Name contains: ");
        String q = SC.nextLine().trim();
        String sql = "SELECT reg_no,name,COALESCE(gpa,0) gpa FROM students WHERE name LIKE ? ORDER BY name";
        try (Connection con = getConn(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + q + "%");
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%s - %s (GPA: %.2f)%n",
                            rs.getString("reg_no"), rs.getString("name"), rs.getDouble("gpa"));
                }
                if (!any) System.out.println("No matches.");
            }
        } catch (SQLException e) {
            System.out.println("Search failed: " + e.getMessage());
        }
    }
}
