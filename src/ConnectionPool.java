import java.sql.Connection;

public interface ConnectionPool {
    Connection getConnection() throws InterruptedException;
    void releaseConnection(Connection connection);
    /**
     * Cierra todas las conexiones y deja el pool inoperante.
     */
    void shutdown();
}
