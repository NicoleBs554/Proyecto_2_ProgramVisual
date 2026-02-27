import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SimulatorUI extends JFrame {
    private final JTextArea logArea = new JTextArea(20, 60);
    private final JButton startButton = new JButton("Iniciar");
    private final JButton stopButton = new JButton("Detener");
    private Thread simThread;

    public SimulatorUI() {
        super("Simulador JDBC PostgreSQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        add(panel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());

        pack();
        setLocationRelativeTo(null);
        setupLogListener();
    }

    private void setupLogListener() {
        LoggerUtil.addListener(line -> {
            SwingUtilities.invokeLater(() -> {
                logArea.append(line + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        });
    }

    private void startSimulation() {
        if (simThread != null && simThread.isAlive()) {
            JOptionPane.showMessageDialog(this, "La simulación ya está en curso.");
            return;
        }
        logArea.setText("");
        simThread = new Thread(() -> {
            try {
                Main.runSimulation();
            } catch (InterruptedException ex) {
                appendLog("Simulación interrumpida");
            } catch (Exception ex) {
                appendLog("Error en simulación: " + ex.getMessage());
            }
        }, "SimThread");
        simThread.start();
    }

    private void stopSimulation() {
        Cliente.activarFreno(true);
        appendLog("Freno activado (se detendrá el próximo intento).");
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimulatorUI().setVisible(true));
    }
}