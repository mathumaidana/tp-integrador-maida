package vista;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import entidades.Usuario;

public abstract class MenuView {

	protected static final int MARGIN = 12;

	protected final Usuario usuario;
	protected final JFrame frame;

	protected MenuView(Usuario usuario) {
		this.usuario = usuario;
		this.frame = new JFrame(tituloVentana());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearEncabezado(), BorderLayout.NORTH);
		root.add(crearPanelOpciones(), BorderLayout.CENTER);
		frame.add(root, BorderLayout.CENTER);
		configurarTamano();
		frame.setLocationRelativeTo(null);
	}

	public void mostrar() {
		frame.setVisible(true);
	}

	public void alCerrar(VentanaListener listener) {
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				listener.onCerrar();
			}
		});
	}

	protected abstract String tituloVentana();

	protected abstract JPanel crearPanelOpciones();

	protected JLabel crearEncabezado() {
		return new JLabel(usuario.getNombreCompleto(), JLabel.CENTER);
	}

	protected void configurarTamano() {
		frame.setSize(440, 320);
	}

	@FunctionalInterface
	public interface VentanaListener {
		void onCerrar();
	}
}
