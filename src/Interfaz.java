import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Interfaz extends Application {
    @Override
    public void start(Stage stage) {
        var btnSimular = new Button("Iniciar simulación");
        var btnFreno = new Button("Freno de emergencia");
        var lblPeticiones = new Label("Número de peticiones:");
        var txtPeticiones = new TextField(String.valueOf(SimulationConfig.getSamples()));
        var statsBox = new VBox();
        var statsLabel = new Label("Estadísticas");
        var statsSinPool = new Label();
        var statsConPool = new Label();
        var progressBarSinPool = new ProgressBar(0);
        progressBarSinPool.setPrefWidth(400);
        var progressBarConPool = new ProgressBar(0);
        progressBarConPool.setPrefWidth(400);
        var progresoSinPool = new Label();
        var progresoConPool = new Label();
        statsBox.getChildren().addAll(statsLabel, progressBarSinPool, progresoSinPool, statsSinPool, progressBarConPool, progresoConPool, statsConPool);

        var titulo = new Label("Pool de Conexiones - Simulación");
        var controls = new HBox(btnSimular, btnFreno);
        controls.setSpacing(15);
        var peticionesBox = new HBox(lblPeticiones, txtPeticiones);
        peticionesBox.setSpacing(10);
        var layout = new VBox(titulo, peticionesBox, controls, statsBox);
        layout.setSpacing(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30 0 0 0;");

        stage.setScene(new Scene(layout, 800, 600));
        stage.setTitle("Pool de Conexiones - Simulación");
        stage.show();

        btnSimular.setOnAction(_e -> {
            Cliente.activarFreno(false);
            int num;
            try { num = Integer.parseInt(txtPeticiones.getText()); }
            catch (NumberFormatException ex) { statsSinPool.setText("Número inválido"); return; }

            var colaSin = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerSin = new EstadisticaManager(colaSin);
            var hiloSin = new Thread(managerSin);
            var cliente = new Cliente(num);
            cliente.setEstadisticaQueue(colaSin);
            hiloSin.start();

            Thread actualizador = new Thread(() -> {
                while (cliente.getCompletadas() < num && !Cliente.estaFrenado()) {
                    double progreso = cliente.getCompletadas() / (double) num;
                    int completadas = cliente.getCompletadas();
                    int faltantes = num - completadas;
                    Platform.runLater(() -> {
                        progressBarSinPool.setProgress(progreso);
                        progresoSinPool.setText("Completadas: " + completadas + " | Faltantes: " + faltantes);
                    });
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
                Platform.runLater(() -> progressBarSinPool.setProgress(1.0));
            });
            actualizador.setDaemon(true);
            actualizador.start();
            cliente.ejecutarSinPoolConEstadisticas();
            managerSin.stop();
            try { hiloSin.join(); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                statsSinPool.setText("✅ Sin pool: " + managerSin.getExitosas() + " exitosas, " + managerSin.getFallidas() + " fallidas");
            });

            // Con pool
            Cliente.activarFreno(false);
            var colaCon = new java.util.concurrent.ConcurrentLinkedQueue<EstadisticaManager.Peticion>();
            var managerCon = new EstadisticaManager(colaCon);
            var hiloCon = new Thread(managerCon);
            var clientePool = new Cliente(num);
            clientePool.setEstadisticaQueue(colaCon);
            hiloCon.start();

            Thread actualizador2 = new Thread(() -> {
                while (clientePool.getCompletadas() < num && !Cliente.estaFrenado()) {
                    double progreso = clientePool.getCompletadas() / (double) num;
                    int completadas = clientePool.getCompletadas();
                    int faltantes = num - completadas;
                    Platform.runLater(() -> {
                        progressBarConPool.setProgress(progreso);
                        progresoConPool.setText("Completadas: " + completadas + " | Faltantes: " + faltantes);
                    });
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
                Platform.runLater(() -> progressBarConPool.setProgress(1.0));
            });
            actualizador2.setDaemon(true);
            actualizador2.start();
            clientePool.ejecutarConPoolConEstadisticas();
            managerCon.stop();
            try { hiloCon.join(); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                statsConPool.setText("✨ Con pool: " + managerCon.getExitosas() + " exitosas, " + managerCon.getFallidas() + " fallidas");
            });
        });

        btnFreno.setOnAction(_e -> {
            Cliente.activarFreno(true);
            statsSinPool.setText("Freno de emergencia activado");
        });
    }
}
