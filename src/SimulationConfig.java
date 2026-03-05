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

    public static void setQuery(String q) {
        if (q != null) props.setProperty("query", q);
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

    public static void setSamples(String s) {
        if (s != null) props.setProperty("samples", s);
    }

    public static int getRetries() {
        return Integer.parseInt(props.getProperty("retries", "2"));
    }

    public static void setRetries(int r) {
        props.setProperty("retries", String.valueOf(r));
    }

    /**
     * Indica el modo de ejecución de la simulación: RAW abre conexión en cada
     * hilo de forma independiente, POOL usa el pool de conexiones, BOTH ejecuta
     * ambas variantes y compara resultados. Puede invocarse desde la línea de
     * comandos o la interfaz gráfica; si no se especifica, se lee de
     * sim.properties (valor por defecto "both").
     */
    public enum Mode { RAW, POOL, BOTH }

    public static Mode getMode() {
        String m = props.getProperty("mode", "both").trim().toUpperCase();
        try {
            return Mode.valueOf(m);
        } catch (IllegalArgumentException e) {
            // valor inválido, usar BOTH y avisar para debug
            System.err.println("Modo de simulación desconocido en sim.properties: " + m + ". Usando BOTH.");
            return Mode.BOTH;
        }
    }

    public static void setMode(Mode m) {
        if (m != null) props.setProperty("mode", m.name());
    }

    /**
     * SQL a ejecutar antes de iniciar las muestras, puede contener varios
     * statements separados por ";". útil para crear tablas o cargar datos.
     */
    public static String getInitSql() {
        return props.getProperty("init_sql");
    }
}
