import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try {
            // mostrar directorio de trabajo para que sepamos dónde busca
            System.out.println("Config cargando desde directorio: " + Path.of(".").toAbsolutePath());
            Path p = Path.of(".env");
            if (!Files.exists(p)) p = Path.of(".ENV");
            if (Files.exists(p)) {
                System.out.println("Leyendo configuracion de " + p.toAbsolutePath());
                try (var fis = new FileInputStream(p.toFile())) {
                    properties.load(fis);
                }
            } else {
                System.out.println("No se encontro archivo .env/.ENV");
            }
            System.out.println("Propiedades cargadas: " + properties);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el archivo de entorno", e);
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
