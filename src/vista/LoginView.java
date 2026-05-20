package vista;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import entidades.Usuario;
import persistencia.UsuarioDao;
import servicio.AutenticacionException;
import servicio.AutenticacionServicio;

public class LoginView {

	private final JFrame frame;
	private final AutenticacionServicio autenticacionServicio;

	public LoginView() {
		this.autenticacionServicio = new AutenticacionServicio(new UsuarioDao());
		this.frame = new LoginFrame(this::intentarLogin, () -> System.exit(0));
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void intentarLogin(String username, String password) {
		try {
			Usuario usuario = autenticacionServicio.autenticar(username, password);
			frame.dispose();
			abrirMenu(usuario);
		} catch (AutenticacionException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error de autenticación", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void abrirMenu(Usuario usuario) {
		MenuView menu = MenuView.crearPara(usuario);
		menu.alCerrar(this::mostrarLogin);
		menu.mostrar();
	}

	private void mostrarLogin() {
		new LoginView();
	}
}
