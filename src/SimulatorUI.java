import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.io.OutputStream;

public class SimulatorUI extends JFrame {
    private final JTextArea logArea = new JTextArea(20, 60);
    private final JButton startButton = new JButton("Iniciar");
    private final JButton stopButton = new JButton("Detener");
    private final JComboBox<SimulationConfig.Mode> modeSelector = new JComboBox<>(SimulationConfig.Mode.values());

    // extra config fields
    private final JTextField queryField = new JTextField(30);
    private final JTextField samplesField = new JTextField(10);
    private final JTextField retriesField = new JTextField(3);

    private Thread simThread;

    public SimulatorUI() {
        super("Simulador JDBC PostgreSQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // configuration panel at top
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.add(new JLabel("Query:"));
        queryField.setText(SimulationConfig.getQuery());
        configPanel.add(queryField);
        configPanel.add(new JLabel("Samples:"));
        samplesField.setText(String.valueOf(SimulationConfig.getSamplesList()[0]));
        configPanel.add(samplesField);
        configPanel.add(new JLabel("Retries:"));
        retriesField.setText(String.valueOf(SimulationConfig.getRetries()));
        configPanel.add(retriesField);
        add(configPanel, BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.add(new JLabel("Modo:"));
        modeSelector.setSelectedItem(SimulationConfig.getMode());
        panel.add(modeSelector);
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
            // saltar las entradas de muestra para que la interfaz no se bloquee
            if (line.contains(" | id=")) return;
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
        // update config from fields
        SimulationConfig.setQuery(queryField.getText().trim());
        SimulationConfig.setSamples(samplesField.getText().trim());
        try {
            SimulationConfig.setRetries(Integer.parseInt(retriesField.getText().trim()));
        } catch (NumberFormatException ignored) {}
        SimulationConfig.setMode((SimulationConfig.Mode) modeSelector.getSelectedItem());

        logArea.setText("");
        SimulationConfig.Mode mode = (SimulationConfig.Mode) modeSelector.getSelectedItem();

        // disable controls while running
        startButton.setEnabled(false);
        modeSelector.setEnabled(false);
        queryField.setEnabled(false);
        samplesField.setEnabled(false);
        retriesField.setEnabled(false);

        simThread = new Thread(() -> {
            // redirigir salida estándar para que no aparezca en la terminal
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));
            try {
                Main.runSimulation(mode);
            } catch (InterruptedException ex) {
                appendLog("Simulación interrumpida");
            } catch (Exception ex) {
                appendLog("Error en simulación: " + ex.getMessage());
            } finally {
                // restaurar
                System.setOut(oldOut);
                System.setErr(oldErr);
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(true);
                    modeSelector.setEnabled(true);
                    queryField.setEnabled(true);
                    samplesField.setEnabled(true);
                    retriesField.setEnabled(true);
                    simThread = null; // permitir arranque de nueva simulación
                });
            }
        }, "SimThread");
        simThread.start();
    }

    private void stopSimulation() {
        Cliente.activarFreno(true);
        if (simThread != null) simThread.interrupt();
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