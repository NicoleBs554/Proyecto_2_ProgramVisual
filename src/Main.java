import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // cargar driver explícitamente para detectar problemas tempranos
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC de PostgreSQL no encontrado en el classpath: " + e.getMessage());
            return;
        }
        int samples = SimulationConfig.getSamples();
        System.out.println("Simulación iniciada con " + samples + " muestras.");
        // Cola y manager para estadisticas
        var cola = new ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
        var manager = new EstadisticaManager(cola);
        var hiloMgr = new Thread(manager);
        hiloMgr.start();

        // Raw
        LoggerUtil.logInfo("Iniciando simulación raw");
        Cliente cliente = new Cliente(samples);
        cliente.setEstadisticaQueue(cola);
        cliente.ejecutarSinPoolConEstadisticas();
        manager.stop();
        hiloMgr.join();
        System.out.println("Raw resultados: " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, " + String.format("%.2f", manager.getPorcentajeExito()) + "% éxito, promedio intentos=" + String.format("%.2f", manager.getPromedioIntentos()));

        // Preparar nueva simulación con pool
        cola.clear();
        manager = new EstadisticaManager(cola);
        hiloMgr = new Thread(manager);
        hiloMgr.start();

        LoggerUtil.logInfo("Iniciando simulación con pool");
        Cliente clientePool = new Cliente(samples);
        clientePool.setEstadisticaQueue(cola);
        clientePool.ejecutarConPoolConEstadisticas();
        manager.stop();
        hiloMgr.join();
        System.out.println("Pool resultados: " + manager.getExitosas() + " exitosas, " + manager.getFallidas() + " fallidas, " + String.format("%.2f", manager.getPorcentajeExito()) + "% éxito, promedio intentos=" + String.format("%.2f", manager.getPromedioIntentos()));

        LoggerUtil.logInfo("Simulación completada");
    }
}
