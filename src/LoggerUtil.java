import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final Object lock = new Object();
    private static final String LOG_FILE = "simulation.log";
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static java.util.List<java.util.function.Consumer<String>> listeners = new java.util.ArrayList<>();

    /**
     * Inicializa el log borrando contenido previo. Llamar al inicio de la ejecución.
     */
    public static void init() {
        synchronized (lock) {
            try (var w = new BufferedWriter(new FileWriter(LOG_FILE))) {
                // truncar
            } catch (IOException e) {
                System.err.println("No se pudo inicializar el log: " + e.getMessage());
            }
        }
    }

    public static void addListener(java.util.function.Consumer<String> listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    public static void logSample(int id, boolean success, String message) {
        var ts = LocalDateTime.now().format(fmt);
        var line = String.format("%s | id=%d | %s | %s", ts, id, success ? "SUCCESS" : "FAIL", message == null ? "" : message);
        writeLine(line);
    }

    public static void logInfo(String message) {
        var ts = LocalDateTime.now().format(fmt);
        writeLine(ts + " | INFO | " + message);
    }

    private static void writeLine(String line) {
        synchronized (lock) {
            // escribir a fichero
            try (BufferedWriter w = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                w.write(line);
                w.newLine();
            } catch (IOException e) {
                System.err.println("No se pudo escribir en el log: " + e.getMessage());
            }
            // notificar a listeners
            for (var l : listeners) {
                try {
                    l.accept(line);
                } catch (Exception ignored) {}
            }
        }
    }
}
