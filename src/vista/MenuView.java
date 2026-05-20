package vista;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import entidades.Administrador;
import entidades.Cliente;
import entidades.Usuario;

public abstract class MenuView {

	protected static final int MARGIN = 12;

	protected final Usuario usuario;
	protected final JFrame frame;

	protected MenuView(Usuario usuario) {
		this.usuario = usuario;
		this.frame = new JFrame(resolverTitulo(usuario));
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

	public static MenuView crearPara(Usuario usuario) {
		if (usuario instanceof Administrador) {
			return new MenuAdminView((Administrador) usuario);
		}
		if (usuario instanceof Cliente) {
			return new MenuClienteView((Cliente) usuario);
		}
		throw new IllegalArgumentException("Tipo de usuario no soportado: " + usuario.getClass().getName());
	}

	public void mostrar() {
		frame.setVisible(true);
	}

	public JFrame getFrame() {
		return frame;
	}

	public void alCerrar(VentanaListener listener) {
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				listener.onCerrar();
			}
		});
	}

	protected abstract JPanel crearPanelOpciones();

	protected JLabel crearEncabezado() {
		return new JLabel(usuario.getNombre() + " " + usuario.getApellido(), JLabel.CENTER);
	}

	protected void configurarTamano() {
		frame.setSize(440, 320);
	}

	protected Cliente cliente() {
		return (Cliente) usuario;
	}

	protected Administrador administrador() {
		return (Administrador) usuario;
	}

	private static String resolverTitulo(Usuario usuario) {
		if (usuario instanceof Administrador) {
			return "Menú admin";
		}
		if (usuario instanceof Cliente) {
			return "Mini Home Banking - " + usuario.getNombre();
		}
		throw new IllegalArgumentException("Tipo de usuario no soportado: " + usuario.getClass().getName());
	}

	@FunctionalInterface
	public interface VentanaListener {
		void onCerrar();
	}
}
