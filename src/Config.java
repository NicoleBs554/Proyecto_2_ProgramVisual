import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try {
            Path p = Path.of(".env");
            if (!Files.exists(p)) p = Path.of(".ENV");
            if (Files.exists(p)) {
                try (var fis = new FileInputStream(p.toFile())) {
                    properties.load(fis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el archivo de entorno", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        var v = properties.getProperty(key);
        return v == null || v.isBlank() ? 0 : Integer.parseInt(v);
    }

    public static long getLong(String key) {
        var v = properties.getProperty(key);
        return v == null || v.isBlank() ? 0L : Long.parseLong(v);
    }
}
