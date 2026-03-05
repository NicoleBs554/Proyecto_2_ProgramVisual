import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

public class Cliente {
    private final int numeroPeticiones;
    private static final AtomicBoolean freno = new AtomicBoolean(false);
    // mantener referencia a los hilos creados para poder interrumpirlos
    private static final java.util.Set<Thread> activeThreads = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final List<ExecutorService> activeExecutors = new ArrayList<>();
    private ConcurrentLinkedQueue<EstadisticaManager.Peticion> colaEstadisticas;
    private final AtomicInteger completadas = new AtomicInteger(0);
    private final AtomicInteger exitosas = new AtomicInteger(0);
    private final AtomicInteger fallidas = new AtomicInteger(0);
    private PoolManager poolManager;
    // para nombrar hilos generados por el executor
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    public Cliente(int numeroPeticiones) {
        this.numeroPeticiones = numeroPeticiones;
    }

    public void setEstadisticaQueue(ConcurrentLinkedQueue<EstadisticaManager.Peticion> queue) { this.colaEstadisticas = queue; }
    public int getCompletadas() { return completadas.get(); }

    public static void activarFreno(boolean estado) {
        freno.set(estado);
        if (estado) {
            // interrumpir cualquier hilo que esté dormido
            for (Thread t : activeThreads) {
                t.interrupt();
            }
            // detener inmediatamente los executors
            synchronized (activeExecutors) {
                for (ExecutorService e : activeExecutors) {
                    e.shutdownNow();
                }
                activeExecutors.clear();
            }
        }
    }
    public static void activarFreno() { activarFreno(true); }
    public static boolean estaFrenado() { return freno.get(); }

    private Thread escucharFreno() {
        var t = new Thread(() -> {
            try {
                while (!estaFrenado()) {
                    if (System.in.available() > 0) {
                        int input = System.in.read();
                        if (input == '\n' || input == '\r') {
                            activarFreno();
                            break;
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (Exception ignored) {}
        }, "FrenoThread");
        t.setDaemon(true);
        t.start();
        return t;
    }

    // helper para crear hilos con menor stack y así poder escalar a 10000+ hilos
    private Thread createWorker(Runnable r, String name) {
        // envolver para eliminar de activeThreads al terminar
        Runnable wrapped = () -> {
            try {
                r.run();
            } finally {
                activeThreads.remove(Thread.currentThread());
            }
        };
        Thread t = new Thread(null, wrapped, name, 128 * 1024);
        activeThreads.add(t);
        return t;
    }

    public void ejecutarSinPoolConEstadisticas() {
        activeThreads.clear();
        activarFreno(false);
        var frenoThread = escucharFreno();
        completadas.set(0);
        exitosas.set(0);
        fallidas.set(0);
        var inicio = System.currentTimeMillis();
        var startLatch = new CountDownLatch(1);
        int maxRetries = SimulationConfig.getRetries();
        String query = SimulationConfig.getQuery();

        // usar factory que produce hilos ligeros para poder escalar a 10k
        ThreadFactory factory = r -> createWorker(r, "Raw-" + threadCounter.incrementAndGet());
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        synchronized (activeExecutors) { activeExecutors.add(executor); }
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                boolean exito = false;
                int attempts = 0;
                while (attempts <= maxRetries && !exito && !estaFrenado() && !Thread.currentThread().isInterrupted()) {
                    attempts++;
                    try (Connection connection = DriverManager.getConnection(
                            Config.getJdbcUrl(),
                            Config.get("DB_USER"),
                            Config.get("DB_PASSWORD")
                    )) {
                        if (estaFrenado()) break;
                        try (Statement stmt = connection.createStatement()) {
                            ResultSet rs = stmt.executeQuery(query);
                            while (rs.next() && !estaFrenado()) {
                                exito = true;
                            }
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito ? "OK" : "Sin resultados (attempt="+attempts+")", attempts));
                            if (exito) exitosas.incrementAndGet(); else if (attempts>maxRetries) fallidas.incrementAndGet();
                        }
                        Thread.sleep(new Random().nextInt(200));
                    } catch (Exception e) {
                        if (attempts>maxRetries) {
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, e.getMessage(), attempts));
                            fallidas.incrementAndGet();
                        }
                        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                }
                completadas.incrementAndGet();
            });
        }
        // lanzar todas a la vez
        startLatch.countDown();

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        synchronized (activeExecutors) { activeExecutors.remove(executor); }
        activarFreno(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        LoggerUtil.logInfo("Tiempo total sin pool: " + (fin - inicio) + " ms");
    }

    public void ejecutarConPoolConEstadisticas() {
        activeThreads.clear();
        activarFreno(false);
        var frenoThread = escucharFreno();
        completadas.set(0);
        exitosas.set(0);
        fallidas.set(0);
        var inicio = System.currentTimeMillis();
        var startLatch = new CountDownLatch(1);
        int maxRetries = SimulationConfig.getRetries();
        String query = SimulationConfig.getQuery();

        // factory para hilos del pool
        ThreadFactory factory = r -> createWorker(r, "Pool-" + threadCounter.incrementAndGet());
        ExecutorService executor = Executors.newCachedThreadPool(factory);
        synchronized (activeExecutors) { activeExecutors.add(executor); }
        for (var i = 0; i < numeroPeticiones; i++) {
            final var idx = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                boolean exito = false;
                int attempts = 0;
                while (attempts <= maxRetries && !exito && !estaFrenado() && !Thread.currentThread().isInterrupted()) {
                    attempts++;
                    try {
                        if (poolManager == null) poolManager = PoolManager.getInstance();
                        var connection = poolManager.getConnection();
                        if (connection == null) {
                            if (attempts>maxRetries) {
                                if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, "No se obtuvo conexión (attempt="+attempts+")", attempts));
                                fallidas.incrementAndGet();
                            }
                            try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                            continue;
                        }
                        try (Statement stmt = connection.createStatement()) {
                            ResultSet rs = stmt.executeQuery(query);
                            while (rs.next() && !estaFrenado()) {
                                exito = true;
                            }
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, exito, exito?"OK":"Sin resultados (attempt="+attempts+")", attempts));
                            if (exito) exitosas.incrementAndGet(); else if (attempts>maxRetries) fallidas.incrementAndGet();
                        } finally {
                            poolManager.releaseConnection(connection);
                        }
                        Thread.sleep(new Random().nextInt(200));
                    } catch (Exception e) {
                        if (attempts>maxRetries) {
                            if (colaEstadisticas != null) colaEstadisticas.add(new EstadisticaManager.Peticion(idx, false, e.getMessage(), attempts));
                            fallidas.incrementAndGet();
                        }
                        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    }
                }
                completadas.incrementAndGet();
            });
        }
        // lanzar todas
        startLatch.countDown();

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        synchronized (activeExecutors) { activeExecutors.remove(executor); }
        activarFreno(true);
        try { if (frenoThread != null) frenoThread.join(100); } catch (InterruptedException ignored) {}
        var fin = System.currentTimeMillis();
        LoggerUtil.logInfo("Tiempo total con pool: " + (fin - inicio) + " ms");
    }
}
