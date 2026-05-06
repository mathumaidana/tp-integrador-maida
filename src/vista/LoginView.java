package vista;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import entidades.Administrador;
import entidades.Cliente;
import entidades.Usuario;
import persistencia.UsuarioDao;
import servicio.AutenticacionException;
import servicio.AutenticacionServicio;

public class LoginView {

	private final JFrame frame;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private final AutenticacionServicio autenticacionServicio;

	public LoginView() {
		this.autenticacionServicio = new AutenticacionServicio(new UsuarioDao());
		this.frame = new JFrame("Mini Home Banking - Login");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(crearFormulario(), BorderLayout.CENTER);
		frame.add(crearBotones(), BorderLayout.SOUTH);
		frame.setSize(360, 200);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JPanel crearFormulario() {
		JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
		panel.add(new JLabel("Usuario:"));
		usernameField = new JTextField();
		panel.add(usernameField);
		panel.add(new JLabel("Contraseña:"));
		passwordField = new JPasswordField();
		panel.add(passwordField);
		return panel;
	}

	private JPanel crearBotones() {
		JPanel panel = new JPanel();
		JButton ingresar = new JButton("Ingresar");
		ingresar.addActionListener(e -> intentarLogin());
		JButton cancelar = new JButton("Cancelar");
		cancelar.addActionListener(e -> System.exit(0));
		panel.add(ingresar);
		panel.add(cancelar);
		return panel;
	}

	private void intentarLogin() {
		String username = usernameField.getText();
		String password = new String(passwordField.getPassword());
		try {
			Usuario u = autenticacionServicio.autenticar(username, password);
			frame.dispose();
			abrirMenu(u);
		} catch (AutenticacionException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error de autenticación", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void abrirMenu(Usuario u) {
		JFrame siguiente;
		if (u instanceof Administrador) {
			siguiente = new MenuAdminView().getFrame();
		} else if (u instanceof Cliente) {
			siguiente = new MenuClienteView((Cliente) u).getFrame();
		} else {
			JOptionPane.showMessageDialog(null, "Rol desconocido", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}
		siguiente.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				new LoginView();
			}
		});
	}
}
