import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DefaultConnectionPool implements ConnectionPool {
    private static final int POOL_SIZE = Math.max(1, Config.getInt("POOL_SIZE"));
    private final BlockingQueue<Connection> pool;

    public DefaultConnectionPool() {
        pool = new ArrayBlockingQueue<>(POOL_SIZE);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver de PostgreSQL no encontrado: " + e.getMessage());
        }
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                var conn = DriverManager.getConnection(
                    "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                    Config.get("DB_USER"),
                    Config.get("DB_PASSWORD")
                );
                pool.offer(conn);
            } catch (SQLException e) {
                System.err.println("No se pudo crear conexión para el pool: " + e.getMessage());
            }
        }
    }

    @Override
    public Connection getConnection() throws InterruptedException {
        return pool.take();
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null) pool.offer(connection);
    }
}
