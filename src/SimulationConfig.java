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
        return props.getProperty("query", "SELECT 1");
    }

    public static int getSamples() {
        return Integer.parseInt(props.getProperty("samples", "10"));
    }

    public static int getRetries() {
        return Integer.parseInt(props.getProperty("retries", "2"));
    }
}
