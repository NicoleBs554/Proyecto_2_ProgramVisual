import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EstadisticaManager implements Runnable {
    public static class Peticion {
        public final int id;
        public final boolean exito;
        public final String mensaje;

        public Peticion(int id, boolean exito, String mensaje) {
            this.id = id;
            this.exito = exito;
            this.mensaje = mensaje;
        }
    }

    private final ConcurrentLinkedQueue<Peticion> cola;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger exitosas = new AtomicInteger(0);
    private final AtomicInteger fallidas = new AtomicInteger(0);

    public EstadisticaManager(ConcurrentLinkedQueue<Peticion> cola) {
        this.cola = cola;
    }

    @Override
    public void run() {
        while (running.get() || !cola.isEmpty()) {
            var p = cola.poll();
            if (p != null) {
                if (p.exito) exitosas.incrementAndGet();
                else fallidas.incrementAndGet();
                LoggerUtil.logSample(p.id, p.exito, p.mensaje);
            } else {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public void stop() { running.set(false); }

    public int getExitosas() { return exitosas.get(); }
    public int getFallidas() { return fallidas.get(); }
    public double getPorcentajeExito() {
        int tot = exitosas.get() + fallidas.get();
        return tot == 0 ? 0.0 : (exitosas.get() * 100.0 / tot);
    }
    public double getPorcentajeFallo() {
        int tot = exitosas.get() + fallidas.get();
        return tot == 0 ? 0.0 : (fallidas.get() * 100.0 / tot);
    }
}
