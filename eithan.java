import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ejemplo didáctico de hilos (Thread/Runnable) en Java:
 *
 *   - Hilo principal (EDT): dibuja y mantiene la interfaz gráfica.
 *   - HiloAnimacion: va mostrando los fotogramas .gif para simular animación.
 *   - HiloSonido: reproduce Cancion.mp3 en bucle, en paralelo con la animación.
 *
 * Ambos hilos de trabajo se inician al mismo tiempo y corren de forma
 * independiente, lo que permite que la imagen cambie y el sonido se escuche
 * simultáneamente sin que uno bloquee al otro.
 */
public class AnimacionGif {

    // Nombre esperado: frame_00_delay-0.12s.gif, frame_01_delay-0.12s.gif, ...
    private static final Pattern PATRON_FOTOGRAMA =
            Pattern.compile("frame_(\\d+)_delay-([0-9.]+)s\\.gif", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnimacionGif::crearInterfaz);
    }

    private static void crearInterfaz() {
        JFrame ventana = new JFrame("Animación con Hilos");
        ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ventana.setSize(500, 340);
        ventana.setLocationRelativeTo(null);

        JLabel etiquetaImagen = new JLabel();
        etiquetaImagen.setHorizontalAlignment(JLabel.CENTER);
        ventana.add(etiquetaImagen);

        DatosAnimacion datos = cargarFotogramas();

        HiloAnimacion tareaAnimacion = new HiloAnimacion(etiquetaImagen, datos.fotogramas, datos.retrasosMs);
        Thread hiloAnimacion = new Thread(tareaAnimacion, "Hilo-Animacion");

        File archivoSonido = new File("recursos/gracias_aficion.mp3");
        HiloSonido tareaSonido = new HiloSonido(archivoSonido);
        Thread hiloSonido = new Thread(tareaSonido, "Hilo-Sonido");

        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tareaAnimacion.detener();
                tareaSonido.detener();
                hiloAnimacion.interrupt();
                ventana.dispose();
                System.exit(0);
            }
        });

        // Se inician ambos hilos: a partir de aquí la animación y el sonido
        // avanzan de forma concurrente, cada uno en su propio hilo.
        hiloAnimacion.start();
        hiloSonido.start();

        ventana.setVisible(true);
    }

    private static final class DatosAnimacion {
        final ImageIcon[] fotogramas;
        final int[] retrasosMs;

        DatosAnimacion(ImageIcon[] fotogramas, int[] retrasosMs) {
            this.fotogramas = fotogramas;
            this.retrasosMs = retrasosMs;
        }
    }

    private static DatosAnimacion cargarFotogramas() {
        File carpetaImagenes = new File("recursos/imagenes");
        File[] archivosGif = carpetaImagenes.exists()
                ? carpetaImagenes.listFiles((dir, nombre) -> PATRON_FOTOGRAMA.matcher(nombre).matches())
                : null;

        if (archivosGif != null && archivosGif.length > 0) {
            Arrays.sort(archivosGif, (a, b) -> numeroDeFotograma(a.getName()) - numeroDeFotograma(b.getName()));

            List<ImageIcon> fotogramas = new ArrayList<>();
            List<Integer> retrasos = new ArrayList<>();
            for (File archivo : archivosGif) {
                fotogramas.add(new ImageIcon(archivo.getAbsolutePath()));
                retrasos.add(retrasoDeFotograma(archivo.getName()));
            }
            return new DatosAnimacion(
                    fotogramas.toArray(new ImageIcon[0]),
                    retrasos.stream().mapToInt(Integer::intValue).toArray());
        }

        // Si no hay fotogramas .gif, se intenta con goku1..4.png
        String[] nombresArchivo = {"goku1.png", "goku2.png", "goku3.png", "goku4.png"};
        ImageIcon[] fotogramas = new ImageIcon[nombresArchivo.length];
        int[] retrasos = new int[nombresArchivo.length];
        for (int i = 0; i < nombresArchivo.length; i++) {
            File archivo = new File("recursos/imagenes/" + nombresArchivo[i]);
            fotogramas[i] = archivo.exists()
                    ? new ImageIcon(archivo.getAbsolutePath())
                    : crearFotogramaDeRespaldo(i);
            retrasos[i] = 150;
        }
        return new DatosAnimacion(fotogramas, retrasos);
    }

    private static int numeroDeFotograma(String nombreArchivo) {
        Matcher m = PATRON_FOTOGRAMA.matcher(nombreArchivo);
        return m.matches() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static int retrasoDeFotograma(String nombreArchivo) {
        Matcher m = PATRON_FOTOGRAMA.matcher(nombreArchivo);
        if (m.matches()) {
            double segundos = Double.parseDouble(m.group(2));
            return Math.max(10, (int) Math.round(segundos * 1000));
        }
        return 150;
    }

    private static ImageIcon crearFotogramaDeRespaldo(int indice) {
        int ancho = 300, alto = 300;
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagen.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color[] colores = {Color.ORANGE, Color.YELLOW, new Color(220, 50, 50), new Color(50, 90, 220)};
        g2.setColor(colores[indice % colores.length]);
        g2.fillOval(50, 50, 200, 200);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Goku " + (indice + 1), 105, 160);
        g2.dispose();

        return new ImageIcon(imagen);
    }
}
