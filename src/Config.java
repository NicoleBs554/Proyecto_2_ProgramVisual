import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        // Cargar de sim.properties
        try (var fis = new FileInputStream("sim.properties")) {
            properties.load(fis);
        } catch (IOException ignored) {
            // archivo opcional
        }
    }

    public static String get(String key) {
        String v = properties.getProperty(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key); // intentar variable de entorno del sistema
        }
        return v;
    }

    public static int getInt(String key) {
        String v = properties.getProperty(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key);
        }
        return v == null || v.isBlank() ? 0 : Integer.parseInt(v);
    }

    public static long getLong(String key) {
        String v = properties.getProperty(key);
        if (v == null || v.isBlank()) {
            v = System.getenv(key);
        }
        return v == null || v.isBlank() ? 0L : Long.parseLong(v);
    }
}
