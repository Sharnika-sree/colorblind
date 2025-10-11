import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;

public class ColorAidApp {
    private static final HashMap<String, String> users = new HashMap<>();
    private static final Scanner sc = new Scanner(System.in);
    private static final String USERS_FILE = "users.txt";

    public static void main(String[] args) {
        loadUsers();
        System.out.println("=== ColorAid: Visual Aid for Colorblind People ===");

        while (true) {
            System.out.println("\n1. Sign Up\n2. Log In\n3. Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> signUp();
                case "2" -> { if (login()) processImage(); }
                case "3" -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void signUp() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        if (u.isEmpty() || users.containsKey(u)) {
            System.out.println("Username invalid or exists.");
            return;
        }
        System.out.print("Password: ");
        String p = sc.nextLine().trim();
        if (p.isEmpty()) { System.out.println("Password cannot be empty."); return; }
        users.put(u, p); saveUsers();
        System.out.println("Account created.");
    }

    private static boolean login() {
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();
        if (users.containsKey(u) && users.get(u).equals(p)) {
            System.out.println("Welcome, " + u + "!");
            return true;
        }
        System.out.println("Wrong credentials.");
        return false;
    }

    private static void loadUsers() {
        try {
            if (!Files.exists(Paths.get(USERS_FILE))) return;
            for (String line : Files.readAllLines(Paths.get(USERS_FILE))) {
                String[] s = line.split(":");
                if (s.length == 2) users.put(s[0], s[1]);
            }
        } catch (Exception ignored) {}
    }

    private static void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (var e : users.entrySet()) bw.write(e.getKey() + ":" + e.getValue() + "\n");
        } catch (Exception ignored) {}
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
        BufferedImage cor = type == 4 ? sim : daltonize(img, sim, type);
        BufferedImage side = combineSideBySide(img, sim);

        String base = path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : path;
        String[] names = {"", "protanopia", "deuteranopia", "tritanopia", "grayscale"};

        try {
            ImageIO.write(sim, "png", new File(base + "_" + names[type] + "_simulated.png"));
            ImageIO.write(cor, "png", new File(base + "_" + names[type] + "_corrected.png"));
            ImageIO.write(side, "png", new File(base + "_" + names[type] + "_comparison.png"));
            System.out.println("✅ Generated simulated, corrected, and comparison images.");
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
                        nr = 0.56667*r + 0.43333*g;
                        ng = 0.55833*r + 0.44167*g;
                        nb = 0.24167*g + 0.75833*b;
                    }
                    case 2 -> { 
                        nr = 0.625*r + 0.375*g;
                        ng = 0.7*r + 0.3*g;
                        nb = 0.3*g + 0.7*b;
                    }
                    case 3 -> { 
                        nr = 0.95*r + 0.05*g;
                        ng = 0.43333*g + 0.56667*b;
                        nb = 0.475*g + 0.525*b;
                    }
                    case 4 -> { 
                        int gray = (r + g + b)/3;
                        nr = ng = nb = gray;
                    }
                }

                int newRGB = (clamp((int)nr)<<16)|(clamp((int)ng)<<8)|clamp((int)nb);
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

    private static BufferedImage combineSideBySide(BufferedImage normal, BufferedImage simulated){
        int w = normal.getWidth(), h = normal.getHeight();
        BufferedImage out = new BufferedImage(w*2,h,BufferedImage.TYPE_INT_RGB);

        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                out.setRGB(x, y, normal.getRGB(x,y));
                out.setRGB(x+w, y, simulated.getRGB(x,y));
            }
        }
        return out;
    }

    private static int clamp(int val){
        return Math.max(0,Math.min(255,val));
    }
}