import java.sql.*;

public class TestDB {
    private static final String DB_URL = "jdbc:sqlite:C:/ColorAidClean/colorAid.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            if (conn != null) {
                System.out.println("✅ Connected to SQLite successfully!");

                String createTableSQL = """
                CREATE TABLE IF NOT EXISTS test_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    test_date TEXT NOT NULL,
                    answers TEXT,
                    result TEXT
                );
                """;

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSQL);
                    System.out.println("Table 'test_results' created or already exists.");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
        }
    }
}
