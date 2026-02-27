import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SimulationConfig {
    private static final Properties props = new Properties();

    static {
        try (var fis = new FileInputStream("sim.properties")) {
            props.load(fis);
        } catch (IOException ignored) {
            // archivo opcional; valores por defecto si no existe
        }
    }

    public static String getQuery() {
        // si hay varias consultas definidas, devolver una aleatoria
        String multi = props.getProperty("queries");
        if (multi != null && !multi.isBlank()) {
            String[] arr = multi.split(";");
            int idx = new java.util.Random().nextInt(arr.length);
            return arr[idx].trim();
        }
        return props.getProperty("query", "SELECT 1");
    }

    public static int[] getSamplesList() {
        String raw = props.getProperty("samples", "10");
        if (raw.contains(",")) {
            return java.util.Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
        return new int[]{Integer.parseInt(raw)};
    }

    public static int getRetries() {
        return Integer.parseInt(props.getProperty("retries", "2"));
    }

    /**
     * SQL a ejecutar antes de iniciar las muestras, puede contener varios
     * statements separados por ";". útil para crear tablas o cargar datos.
     */
    public static String getInitSql() {
        return props.getProperty("init_sql");
    }
}
