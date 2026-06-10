package app;

import java.sql.SQLException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import persistencia.InicializadorBD;
import vista.LoginView;

public class Main {

	public static void main(String[] args) {
		try {
			new InicializadorBD().crear();
		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null,
				"Error inicializando la base de datos: " + e.getMessage(),
				"Error fatal", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		SwingUtilities.invokeLater(LoginView::new);
	}
}
