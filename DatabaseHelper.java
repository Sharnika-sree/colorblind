import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:C:/ColorAidClean/colorAid.db";

    public static void saveResult(String username, String answers, String result) {
        String insertSQL = "INSERT INTO test_results(username, test_date, answers, result) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            pstmt.setString(1, username);
            pstmt.setString(2, formattedTime);
            pstmt.setString(3, answers);
            pstmt.setString(4, result);

            pstmt.executeUpdate();
            System.out.println("✅ Test result saved successfully for user: " + username + " at " + formattedTime);

        } catch (SQLException e) {
            System.out.println("❌ Error saving result: " + e.getMessage());
        }
    }


    public static void showAllResults() {
        String query = "SELECT * FROM test_results ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\n=== 🧠 All Saved Test Results ===");
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

            while (rs.next()) {
                String rawDate = rs.getString("test_date");
                String formattedDate = "";
                try {
                    formattedDate = LocalDateTime.parse(rawDate, inputFormat).format(displayFormat);
                } catch (Exception e) {
                    formattedDate = rawDate; 
                }

                System.out.println("#" + rs.getInt("id") + " | " + formattedDate);
                System.out.println("👤 User: " + rs.getString("username"));
                System.out.println("🧩 Answers: " + rs.getString("answers"));
                System.out.println("🎯 Result: " + rs.getString("result"));
                System.out.println("-----------------------------");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error retrieving results: " + e.getMessage());
        }
    }
}
