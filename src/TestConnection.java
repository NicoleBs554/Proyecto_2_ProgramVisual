import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/Proyecto2_visual", "postgres", "Peroqueconio12")) {
                System.out.println("Conexión exitosa");
                conn.isValid(5);
                System.out.println("Conexión válida");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}