import javax.swing.SwingUtilities;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            logica modelo = new logica();
            VentanaPrincipal ventana = new VentanaPrincipal();
            Controlador controlador = new Controlador(modelo, ventana);
            ventana.setControlador(controlador);
            ventana.setVisible(true);
        });
    }
}