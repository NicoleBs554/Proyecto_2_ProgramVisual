import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        // 1. Cargar sim.properties si existe
        try (var fis = new FileInputStream("sim.properties")) {
            properties.load(fis);
        } catch (IOException ignored) {
            // archivo opcional
        }
        // 2. Cargar .env si está presente (formato simple KEY=VAL, comentarios con '#')
        try (var fis = new FileInputStream(".env");
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                // remove surrounding quotes if present
                if ((val.startsWith("\"") && val.endsWith("\"")) ||
                    (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                if (!key.isEmpty()) {
                    properties.setProperty(key, val);
                }
            }
        } catch (IOException ignored) {
            // .env opcional
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

    /**
     * Devuelve la URL JDBC completa. Se puede proporcionar directamente en la
     * variable DB_URL/DATABASE_URL; si no existe, se construye a partir de
     * DB_HOST, DB_PORT, DB_NAME y opcionalmente DB_DIALECT.
     */
    public static String getJdbcUrl() {
        String url = get("DB_URL");
        if (url != null && !url.isBlank()) {
            return url;
        }
        // permitir también DATABASE_URL por compatibilidad con algunas plataformas
        url = get("DATABASE_URL");
        if (url != null && !url.isBlank()) {
            return url;
        }
        String host = get("DB_HOST");
        String port = get("DB_PORT");
        String name = get("DB_NAME");
        String dialect = get("DB_DIALECT");
        if (dialect == null || dialect.isBlank()) {
            dialect = "postgresql";
        } else if ("postgres".equals(dialect)) {
            // corregir variante común
            dialect = "postgresql";
        }
        if (host == null) host = "";
        if (port == null) port = "";
        if (name == null) name = "";
        return "jdbc:" + dialect + "://" + host + (port.isBlank() ? "" : ":" + port) + "/" + name;
    }
}
