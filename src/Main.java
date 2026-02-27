import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    private static void validateConfig() {
        String[] keys = {"DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD"};
        for (String k : keys) {
            String v = Config.get(k);
            System.out.println("Config " + k + "=" + v);
            if (v == null || v.isBlank()) {
                System.err.println("Falta variable de entorno obligatoria: " + k);
                throw new IllegalStateException("Variable faltante: " + k);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        runSimulation();
    }

    /**
     * Ejecuta la simulación completa usando la configuración actual.
     * Reutilizable desde otras clases (por ejemplo una interfaz gráfica).
     */
    public static void runSimulation() throws InterruptedException {
        // limpiar log al inicio
        LoggerUtil.init();

        validateConfig();

        // cargar driver explícitamente para detectar problemas tempranos
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC de PostgreSQL no encontrado en el classpath: " + e.getMessage());
            System.err.println("Asegúrate de ejecutar con -cp que incluya el jar del driver.");
            return;
        }

        // mostrar classpath actual para depuración
        System.out.println("Classpath en ejecución: " + System.getProperty("java.class.path"));

        // probar la conexión básica inmediatamente
        try (var conn = java.sql.DriverManager.getConnection(
                "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME"),
                Config.get("DB_USER"),
                Config.get("DB_PASSWORD")
        )) {
            System.out.println("Conexión de prueba exitosa a la base de datos.");
        } catch (Exception e) {
            System.err.println("Error de prueba de conexión: " + e.getMessage());
            // si falla, salimos porque de poco sirve continuar
            return;
        }

        // preparar la base si la query lo requiere
        prepareDatabase();

        int[] sampleSizes = SimulationConfig.getSamplesList();
        long totalRawTime = 0;
        long totalPoolTime = 0;
        for (int samples : sampleSizes) {
            System.out.println("Simulación iniciada con " + samples + " muestras.");
            // Cola y manager para estadisticas
            var cola = new ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var manager = new EstadisticaManager(cola);
            var hiloMgr = new Thread(manager);
            hiloMgr.start();

            // Raw
            LoggerUtil.logInfo("Iniciando simulación raw con " + samples + " hilos");
            Cliente cliente = new Cliente(samples);
            cliente.setEstadisticaQueue(cola);
            long startRaw = System.currentTimeMillis();
            cliente.ejecutarSinPoolConEstadisticas();
            long rawTime = System.currentTimeMillis() - startRaw;
            totalRawTime += rawTime;

            manager.stop();
            hiloMgr.join();
            double porcRaw = manager.getPorcentajeExito();
            double promRaw = manager.getPromedioIntentos();
            System.out.println("Raw resultados: " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, "
                    + String.format("%.2f", porcRaw) + "% éxito, promedio intentos=" + String.format("%.2f", promRaw) + ", tiempo=" + rawTime + " ms");
            LoggerUtil.logInfo("Resumen raw (" + samples + " hilos): " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, "
                    + String.format("%.2f", porcRaw) + "% éxito, promedio intentos=" + String.format("%.2f", promRaw) + ", tiempo=" + rawTime + " ms");

            // Preparar nueva simulación con pool
            cola.clear();
            manager = new EstadisticaManager(cola);
            hiloMgr = new Thread(manager);
            hiloMgr.start();

            LoggerUtil.logInfo("Iniciando simulación con pool con " + samples + " hilos");
            Cliente clientePool = new Cliente(samples);
            clientePool.setEstadisticaQueue(cola);
            long startPool = System.currentTimeMillis();
            clientePool.ejecutarConPoolConEstadisticas();
            long poolTime = System.currentTimeMillis() - startPool;
            totalPoolTime += poolTime;

            manager.stop();
            hiloMgr.join();
            double porcPool = manager.getPorcentajeExito();
            double promPool = manager.getPromedioIntentos();
            System.out.println("Pool resultados: " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, "
                    + String.format("%.2f", porcPool) + "% éxito, promedio intentos=" + String.format("%.2f", promPool) + ", tiempo=" + poolTime + " ms");
            LoggerUtil.logInfo("Resumen pool (" + samples + " hilos): " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, "
                    + String.format("%.2f", porcPool) + "% éxito, promedio intentos=" + String.format("%.2f", promPool) + ", tiempo=" + poolTime + " ms");
        }

        // comparación general
        System.out.println("Tiempo total raw: " + totalRawTime + " ms, pool: " + totalPoolTime + " ms");
        if (totalRawTime < totalPoolTime) {
            System.out.println("En general raw fue más rápido.");
            LoggerUtil.logInfo("Comparativa: raw mejor que pool (" + totalRawTime + " vs " + totalPoolTime + ")");
        } else if (totalPoolTime < totalRawTime) {
            System.out.println("En general pool fue más rápido.");
            LoggerUtil.logInfo("Comparativa: pool mejor que raw (" + totalPoolTime + " vs " + totalRawTime + ")");
        } else {
            System.out.println("Tiempos totales iguales para raw y pool.");
            LoggerUtil.logInfo("Comparativa: raw igual a pool (" + totalRawTime + ")");
        }

        // liberar recursos del pool antes de terminar
        PoolManager.getInstance().shutdown();

        LoggerUtil.logInfo("Simulación completada");
    }

    /**
     * Analiza la consulta de configuración y, si contiene un FROM <tabla>,
     * crea dicha tabla en la base de datos (con columna id serial) y añade
     * una fila si no existe ninguna. así la consulta SELECT devolverá un resultado
     * y las simulaciones producirán éxitos.
     */
    private static void prepareDatabase() {
        String query = SimulationConfig.getQuery();
        if (query == null) return;
        String lower = query.toLowerCase();
        if (lower.contains(" from ")) {
            String after = lower.split(" from ")[1].trim();
            String table = after.split("\\s+")[0].replaceAll("[^a-z0-9_]", "");
            if (!table.isEmpty()) {
                String url = "jdbc:postgresql://" + Config.get("DB_HOST") + ":" + Config.get("DB_PORT") + "/" + Config.get("DB_NAME");
                try (var conn = java.sql.DriverManager.getConnection(url, Config.get("DB_USER"), Config.get("DB_PASSWORD"));
                     var stmt = conn.createStatement()) {
                    // ejecutar SQL de inicialización si está configurado
                    String init = SimulationConfig.getInitSql();
                    if (init != null && !init.isBlank()) {
                        stmt.execute(init);
                    }
                    // crear tabla simple y al menos una fila
                    stmt.execute("CREATE TABLE IF NOT EXISTS " + table + " (id serial primary key);");
                    // insertar siempre una fila para que la query tenga resultados
                    stmt.execute("INSERT INTO " + table + " DEFAULT VALUES;");
                    System.out.println("Se aseguró existencia de tabla '" + table + "' y se añadió una fila.");
                } catch (Exception e) {
                    System.err.println("No se pudo preparar la tabla '" + table + "': " + e.getMessage());
                }
            }
        }
    }
}
