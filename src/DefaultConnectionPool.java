import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultConnectionPool implements ConnectionPool {
    private final LinkedBlockingQueue<Connection> pool;
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final int minSize;
    private final int maxSize;
    private final double downThreshold;

    public DefaultConnectionPool() {
        // configuración dinámica
        int initial = Math.max(1, Config.getInt("POOL_SIZE"));
        minSize = Math.max(1, Config.getInt("POOL_MIN_SIZE"));
        if (minSize > initial) {
            // ensure initial not below min
            initial = minSize;
        }
        int cfgMax = Config.getInt("POOL_MAX_SIZE");
        if (cfgMax <= 0) {
            maxSize = initial;
        } else {
            maxSize = Math.max(initial, cfgMax);
        }
        downThreshold = Config.getInt("POOL_DOWN_THRESHOLD") / 100.0; // porcentaje de libres para reducir
        pool = new LinkedBlockingQueue<>();

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver de PostgreSQL no encontrado: " + e.getMessage());
        }
        // crear conexiones iniciales
        for (int i = 0; i < initial; i++) {
            try {
                pool.offer(createConnection());
                totalConnections.incrementAndGet();
            } catch (SQLException e) {
                System.err.println("No se pudo crear conexión para el pool: " + e.getMessage());
            }
        }
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                Config.get("DB_USER"),
                Config.get("DB_PASSWORD")
        );
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        Connection conn = pool.poll();
        if (conn != null) {
            return conn;
        }
        // no hay libres
        if (totalConnections.get() < maxSize) {
            try {
                conn = createConnection();
                totalConnections.incrementAndGet();
                System.out.println("[Pool] escala hacia arriba: nuevo tamaño=" + totalConnections.get());
                return conn;
            } catch (SQLException e) {
                // no se pudo crear, seguir esperando
            }
        }
        // si no se puede expandir, esperar
        return pool.take();
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection == null) return;
        if (!pool.offer(connection)) {
            // cola llena, cerrar la conexión
            try { connection.close(); } catch (SQLException ignored) {}
            totalConnections.decrementAndGet();
            return;
        }
        maybeShrink();
    }

    private void maybeShrink() {
        if (downThreshold <= 0) return;
        int total = totalConnections.get();
        int free = pool.size();
        if (total > minSize && free / (double) total > downThreshold) {
            Connection c = pool.poll();
            if (c != null) {
                try { c.close(); } catch (SQLException ignored) {}
                totalConnections.decrementAndGet();
                System.out.println("[Pool] escala hacia abajo: nuevo tamaño=" + totalConnections.get());
            }
        }
    }

    @Override
    public void shutdown() {
        Connection conn;
        while ((conn = pool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
