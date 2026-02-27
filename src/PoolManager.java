import java.sql.Connection;

public class PoolManager {
    private static PoolManager instance;
    private final ConnectionPool pool;

    private PoolManager() {
        pool = new DefaultConnectionPool();
    }

    public static synchronized PoolManager getInstance() {
        if (instance == null) instance = new PoolManager();
        return instance;
    }

    public Connection getConnection() {
        try {
            return pool.getConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void releaseConnection(Connection connection) {
        pool.releaseConnection(connection);
    }
}
