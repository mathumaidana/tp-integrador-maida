package vista;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.BiConsumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

class LoginFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	LoginFrame(BiConsumer<String, String> onLogin, Runnable onCancel) {
		super("Mini Home Banking - Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
		JTextField usernameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();
		panel.add(new JLabel("Usuario:"));
		panel.add(usernameField);
		panel.add(new JLabel("Contraseña:"));
		panel.add(passwordField);
		add(panel, BorderLayout.CENTER);

		JPanel botones = new JPanel();
		JButton ingresar = new JButton("Ingresar");
		ingresar.addActionListener(e -> onLogin.accept(
			usernameField.getText(), new String(passwordField.getPassword())));
		JButton cancelar = new JButton("Cancelar");
		cancelar.addActionListener(e -> onCancel.run());
		botones.add(ingresar);
		botones.add(cancelar);
		add(botones, BorderLayout.SOUTH);

		setSize(360, 200);
	}
}
