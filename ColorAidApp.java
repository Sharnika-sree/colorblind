import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.sql.*;

public class ColorAidApp {
    private static final String DB_URL = "jdbc:sqlite:coloraid.db";
    private static final Scanner sc = new Scanner(System.in);
    private static String currentUsername = "";
    private static final Stack<Integer> testHistory = new Stack<>();
    public static void main(String[] args) {
        createTables();
        System.out.println("=== ColorAid: Visual Aid for Colorblind People ===");

        while (true) {
            System.out.println("\n1. Sign Up\n2. Log In\n3. Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> signUp();
                case "2" -> {
                    if (login()) {
                        boolean back = false;
                        while (!back) {
                            System.out.println("1. Run Color Blindness Test");
                            System.out.println("2. Process Image");
                            System.out.println("3. View My Past Results");
                            System.out.println("4. Undo Last Test Result");
                            System.out.println("5. Logout");
                            System.out.print("Choose: ");
                            String opt = sc.nextLine().trim();
                            switch (opt) {
                            case "1" -> runColorBlindnessTest();
                            case "2" -> processImage();
                            case "3" -> viewPastResults();
                            case "4" -> undoLastTestResult();
                            case "5" -> {
                            back = true;
                            currentUsername = "";
    }
                            default -> System.out.println("Invalid option.");
}

                        }
                    }
                }
                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static Connection connectDB() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
            return null;
        }
    }

    private static void createTables() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL)";
        String resultsTable = "CREATE TABLE IF NOT EXISTS test_results ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL, "
                + "test_date TEXT NOT NULL DEFAULT (datetime('now')), "
                + "answers TEXT, "
                + "result TEXT NOT NULL)";
        try (Connection conn = connectDB();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(resultsTable);
        } catch (SQLException e) {
            System.out.println("❌ Could not create tables: " + e.getMessage());
        }
    }

    private static void signUp() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        if (u.isEmpty() || p.isEmpty()) {
            System.out.println("❌ Username or password cannot be empty.");
            return;
        }

        try (Connection conn = connectDB()) {
            String check = "SELECT username FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(check);
            ps.setString(1, u);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("⚠️ Username already exists.");
                return;
            }

            String insert = "INSERT INTO users(username, password) VALUES(?, ?)";
            ps = conn.prepareStatement(insert);
            ps.setString(1, u);
            ps.setString(2, p);
            ps.executeUpdate();
            System.out.println("✅ Account created successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Signup failed: " + e.getMessage());
        }
    }

    private static boolean login() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        try (Connection conn = connectDB()) {
            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, u);
            ps.setString(2, p);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentUsername = u;
                System.out.println("✅ Welcome, " + u + "!");
                return true;
            } else {
                System.out.println("❌ Wrong credentials.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("❌ Login error: " + e.getMessage());
            return false;
        }
    }

    private static final String[][] TESTS = {
        {"plate1.png", "12", "normal"},
        {"plate2.jpg", "8", "protanopia"},
        {"plate3.jpg", "6", "deuteranopia"},
        {"plate4.jpg", "29", "tritanopia"},
        {"plate5.jpg", "5", "protanopia"},
        {"plate6.png", "2", "deuteranopia"},
        {"plate7.jpg", "74", "protanopia"},
        {"plate8.jpg", "45", "deuteranopia"},
        {"plate9.png", "29", "tritanopia"},
        {"plate10.jpg", "15", "tritanopia"}
    };

    private static void runColorBlindnessTest() {
        SwingUtilities.invokeLater(() -> {
            final int totalPlates = TESTS.length;
            final int[] index = {0};
            final int[] protanopiaCount = {0}, deuteranopiaCount = {0}, tritanopiaCount = {0};
            final int timePerPlateSeconds = 20;
            final StringBuilder answersSB = new StringBuilder(); 

            JFrame frame = new JFrame("ColorAid - Color Vision Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(700, 600);
            frame.setLocationRelativeTo(null);

            JPanel topPanel = new JPanel(new BorderLayout());
            JLabel title = new JLabel("Color Vision Test (Ishihara-style)", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            topPanel.add(title, BorderLayout.CENTER);

            JLabel progressLabel = new JLabel("Plate 1 of " + totalPlates, SwingConstants.CENTER);
            topPanel.add(progressLabel, BorderLayout.SOUTH);

            JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(600, 400));

            JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
            JLabel timerLabel = new JLabel("Time left: " + timePerPlateSeconds + "s", SwingConstants.CENTER);
            bottomPanel.add(timerLabel, BorderLayout.NORTH);

            JTextField answerField = new JTextField();
            answerField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            answerField.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel controls = new JPanel(new FlowLayout());
            JButton nextBtn = new JButton("Next");
            JButton skipBtn = new JButton("Skip");
            controls.add(skipBtn);
            controls.add(nextBtn);

            bottomPanel.add(answerField, BorderLayout.CENTER);
            bottomPanel.add(controls, BorderLayout.SOUTH);

            frame.setLayout(new BorderLayout(10, 10));
            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(imageLabel, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);
            frame.setVisible(true);

            final javax.swing.Timer[] countdown = new javax.swing.Timer[1];
            final int[] secondsLeft = {timePerPlateSeconds};

            Runnable showPlate = new Runnable() {
                @Override
                public void run() {
                    if (index[0] >= totalPlates) {
                        if (countdown[0] != null && countdown[0].isRunning()) countdown[0].stop();
                        frame.dispose();
                        String result;
                        if (protanopiaCount[0] >= 3)
                            result = "⚠️ Possible Protanopia (Red Color Blindness)";
                        else if (deuteranopiaCount[0] >= 3)
                            result = "⚠️ Possible Deuteranopia (Green Color Blindness)";
                        else if (tritanopiaCount[0] >= 2)
                            result = "⚠️ Possible Tritanopia (Blue Color Blindness)";
                        else
                            result = "✅ Normal Color Vision";
                        saveTestResult(currentUsername, answersSB.toString(), result);

                        String explanation = buildDetailedExplanation(protanopiaCount[0], deuteranopiaCount[0], tritanopiaCount[0], result);
                        showResultWindowDetailed(result, explanation);
                        return;
                    }

                    progressLabel.setText("Plate " + (index[0] + 1) + " of " + totalPlates);
                    String[] test = TESTS[index[0]];
                    String imgPath = test[0];
                    try {
                        BufferedImage img = ImageIO.read(new File(imgPath));
                        if (img != null) {
                            Image scaled = img.getScaledInstance(560, 420, Image.SCALE_SMOOTH);
                            imageLabel.setIcon(new ImageIcon(scaled));
                            imageLabel.setText("");
                        } else {
                            imageLabel.setIcon(null);
                            imageLabel.setText("<html><center>Image not found:<br>" + imgPath + "</center></html>");
                        }
                    } catch (Exception ex) {
                        imageLabel.setIcon(null);
                        imageLabel.setText("<html><center>Cannot load image:<br>" + imgPath + "</center></html>");
                    }

                    answerField.setText("");
                    answerField.requestFocusInWindow();
                    secondsLeft[0] = timePerPlateSeconds;
                    timerLabel.setText("Time left: " + secondsLeft[0] + "s");
                    if (countdown[0] != null && countdown[0].isRunning()) countdown[0].stop();

                    countdown[0] = new javax.swing.Timer(1000, e -> {
                        secondsLeft[0]--;
                        timerLabel.setText("Time left: " + secondsLeft[0] + "s");
                        if (secondsLeft[0] <= 0) {
                            countdown[0].stop();
                            processAndAdvance("");
                        }
                    });
                    countdown[0].start();
                }

                private void processAndAdvance(String userAns) {
                    String[] test = TESTS[index[0]];
                    String expected = test[1];
                    String type = test[2];

                    if (answersSB.length() > 0) answersSB.append(",");
                    answersSB.append(userAns.isEmpty() ? "-" : userAns);

                    if (!userAns.equals(expected)) {
                        switch (type) {
                            case "protanopia" -> protanopiaCount[0]++;
                            case "deuteranopia" -> deuteranopiaCount[0]++;
                            case "tritanopia" -> tritanopiaCount[0]++;
                        }
                    }

                    index[0]++;
                    SwingUtilities.invokeLater(this);
                }
            };

            nextBtn.addActionListener(e -> {
                if (countdown[0] != null && countdown[0].isRunning()) countdown[0].stop();
                String ans = answerField.getText().trim();
                String[] test = TESTS[index[0]];
                String expected = test[1];
                String type = test[2];

                if (answersSB.length() > 0) answersSB.append(",");
                answersSB.append(ans.isEmpty() ? "-" : ans);

                if (!ans.equals(expected)) {
                    switch (type) {
                        case "protanopia" -> protanopiaCount[0]++;
                        case "deuteranopia" -> deuteranopiaCount[0]++;
                        case "tritanopia" -> tritanopiaCount[0]++;
                    }
                }
                index[0]++;
                showPlate.run();
            });

            skipBtn.addActionListener(e -> {
                if (countdown[0] != null && countdown[0].isRunning()) countdown[0].stop();
                String[] test = TESTS[index[0]];
                switch (test[2]) {
                    case "protanopia" -> protanopiaCount[0]++;
                    case "deuteranopia" -> deuteranopiaCount[0]++;
                    case "tritanopia" -> tritanopiaCount[0]++;
                }
                // record skip
                if (answersSB.length() > 0) answersSB.append(",");
                answersSB.append("-");
                index[0]++;
                showPlate.run();
            });

            showPlate.run();
        });
    }

private static void undoLastTestResult() {
    if (testHistory.isEmpty()) {
        System.out.println("No recent test result to undo!");
        return;
    }

    int lastId = testHistory.pop();
    String sql = "DELETE FROM test_results WHERE id = ? AND username = ?";
    try (Connection conn = connectDB();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, lastId);
        ps.setString(2, currentUsername);
        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("✅ Successfully undone last test result (ID " + lastId + ")");
        else
            System.out.println(" Could not find test result to undo.");
    } catch (SQLException e) {
        System.out.println("❌ Undo failed: " + e.getMessage());
    }
}


    
  private static void saveTestResult(String username, String answers, String result) {
    String sql = "INSERT INTO test_results(username, test_date, answers, result) VALUES(?, datetime('now'), ?, ?)";
    try (Connection conn = connectDB();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, username);
        ps.setString(2, answers);
        ps.setString(3, result);
        ps.executeUpdate();

        
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            int id = keys.getInt(1);
            testHistory.push(id);
            System.out.println("💾 Test result saved (ID " + id + ") — added to undo history!");
        }
    } catch (SQLException e) {
        System.out.println("❌ Failed to save result: " + e.getMessage());
    }
}


    private static void viewPastResults() {
        String sql = "SELECT * FROM test_results WHERE username = ? ORDER BY test_date DESC";
        try (Connection conn = connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n───────────────────────────────────────────────");
            System.out.println("   Past Test Results for: " + currentUsername);
            System.out.println("───────────────────────────────────────────────");

            boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                String date = rs.getString("test_date");
                String result = rs.getString("result");
                String answers = rs.getString("answers");

                System.out.println("📅 Date: " + date);
                System.out.println("🧾 Result: " + result);

                if (answers != null && !answers.isEmpty()) {
                    System.out.println("🔢 Answers: [" + answers + "]");
                }

                System.out.println("───────────────────────────────────────────────");
            }

            if (!hasResults) {
                System.out.println("⚠️ No previous test results found.");
                System.out.println("───────────────────────────────────────────────");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error retrieving results: " + e.getMessage());
        }
    }


    private static String buildDetailedExplanation(int pCount, int dCount, int tCount, String highLevelResult) {
        return "Test summary:\n"
                + "- Protanopia plates incorrect: " + pCount + "\n"
                + "- Deuteranopia plates incorrect: " + dCount + "\n"
                + "- Tritanopia plates incorrect: " + tCount + "\n\n"
                + "Result: " + highLevelResult + "\n\nNote: This test is for screening only.";
    }

    private static void showResultWindowDetailed(String result, String explanation) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Color Vision Test Result");
            frame.setSize(500, 350);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JLabel resultLabel = new JLabel(result, SwingConstants.CENTER);
            resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            JTextArea area = new JTextArea(explanation);
            area.setEditable(false);
            frame.add(resultLabel, BorderLayout.NORTH);
            frame.add(new JScrollPane(area), BorderLayout.CENTER);
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> frame.dispose());
            frame.add(ok, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }

    private static void processImage() {
        System.out.print("\nEnter image path: ");
        String path = sc.nextLine().replace("\"", "").trim();
        BufferedImage img;
        try {
            img = ImageIO.read(new File(path));
            if (img == null) {
                System.out.println("❌ Image not found or format unsupported.");
                return;
            }
        } catch (Exception e) {
            System.out.println("❌ Cannot read image.");
            return;
        }

        System.out.println("""
            Choose color blindness type:
            1. Protanopia (Red-blind)
            2. Deuteranopia (Green-blind)
            3. Tritanopia (Blue-blind)
            4. Grayscale
        """);
        System.out.print("Type: ");
        int type;
        try { type = Integer.parseInt(sc.nextLine()); }
        catch (Exception e) { System.out.println("Invalid input."); return; }
        if (type < 1 || type > 4) { System.out.println("❌ Invalid type."); return; }

        System.out.println("Processing image, please wait...");

        BufferedImage sim = simulateColorBlindness(img, type);
        BufferedImage cor = (type == 4) ? sim : daltonize(img, sim, type);
        BufferedImage side = combineSideBySide(img, sim);

        String base = path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : path;
        String[] names = {"", "protanopia", "deuteranopia", "tritanopia", "grayscale"};

        try {
            ImageIO.write(sim, "png", new File(base + "_" + names[type] + "_simulated.png"));
            ImageIO.write(cor, "png", new File(base + "_" + names[type] + "_corrected.png"));
            ImageIO.write(side, "png", new File(base + "_" + names[type] + "_comparison.png"));
            System.out.println("✅ Generated simulated, corrected, and comparison images:");
            System.out.println("   " + base + "_" + names[type] + "_simulated.png");
            System.out.println("   " + base + "_" + names[type] + "_corrected.png");
            System.out.println("   " + base + "_" + names[type] + "_comparison.png");
        } catch (Exception e) {
            System.out.println("❌ Failed to save results.");
        }
    }

    
    private static BufferedImage simulateColorBlindness(BufferedImage src, int type) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double nr = r, ng = g, nb = b;

                switch (type) {
                    case 1 -> { 
                        nr = 0.56667 * r + 0.43333 * g;
                        ng = 0.55833 * r + 0.44167 * g;
                        nb = 0.24167 * g + 0.75833 * b;
                    }
                    case 2 -> { 
                        nr = 0.625 * r + 0.375 * g;
                        ng = 0.7 * r + 0.3 * g;
                        nb = 0.3 * g + 0.7 * b;
                    }
                    case 3 -> { 
                        nr = 0.95 * r + 0.05 * g;
                        ng = 0.43333 * g + 0.56667 * b;
                        nb = 0.475 * g + 0.525 * b;
                    }
                    case 4 -> { 
                        int gray = (r + g + b) / 3;
                        nr = ng = nb = gray;
                    }
                }
                int newRGB = (clamp((int) nr) << 16) | (clamp((int) ng) << 8) | clamp((int) nb);
                out.setRGB(x, y, newRGB);
            }
        }
        return out;
    }

    private static BufferedImage daltonize(BufferedImage orig, BufferedImage sim, int type) {
        int w = orig.getWidth(), h = orig.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oRGB = orig.getRGB(x, y);
                int sRGB = sim.getRGB(x, y);

                int r = (oRGB >> 16) & 0xFF;
                int g = (oRGB >> 8) & 0xFF;
                int b = oRGB & 0xFF;

                int sr = (sRGB >> 16) & 0xFF;
                int sg = (sRGB >> 8) & 0xFF;
                int sb = sRGB & 0xFF;

                int dr = r - sr;
                int dg = g - sg;
                int db = b - sb;

                double nr = r, ng = g, nb = b;

                
                switch (type) {
                    case 1 -> { 
                        nr = r;
                        ng = g + 3.0 * dr;
                        nb = b + 1.0 * dr;
                    }
                    case 2 -> { 
                        nr = r + 3.0 * dg;
                        ng = g;
                        nb = b + 1.0 * dg;
                    }
                    case 3 -> { 
                        nr = r + 1.0 * db;
                        ng = g + 1.0 * db;
                        nb = b;
                    }
                }

                int newRGB = (clamp((int) nr) << 16) | (clamp((int) ng) << 8) | clamp((int) nb);
                out.setRGB(x, y, newRGB);
            }
        }
        return out;
    }

    private static BufferedImage combineSideBySide(BufferedImage normal, BufferedImage simulated) {
        int w = normal.getWidth(), h = normal.getHeight();
        BufferedImage out = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = out.createGraphics();
        g.drawImage(normal, 0, 0, null);
        g.drawImage(simulated, w, 0, null);
        g.dispose();
        return out;
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
    
}
