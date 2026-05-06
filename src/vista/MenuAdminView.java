package vista;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class MenuAdminView {

	private static final int MARGIN = 12;
	private final JFrame frame;

	public MenuAdminView() {
		frame = new JFrame("Menú Administrador");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout());
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearPanel(), BorderLayout.CENTER);
		frame.add(root, BorderLayout.CENTER);
		frame.setSize(440, 300);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JPanel crearPanel() {
		JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JButton clientes = new JButton("Administración de clientes");
		clientes.addActionListener(e -> new FormularioCliente());
		JButton cuentas = new JButton("Asignar cuentas a clientes");
		cuentas.addActionListener(e -> JOptionPane.showMessageDialog(frame,
			"Funcionalidad disponible en la entrega final", "Pendiente", JOptionPane.INFORMATION_MESSAGE));
		JButton tarjetas = new JButton("Emitir / cargar tarjetas");
		tarjetas.addActionListener(e -> new FormularioTarjeta());
		JButton resumen = new JButton("Reportes / resumen");
		resumen.addActionListener(e -> new ResumenView(null));
		JButton salir = new JButton("Cerrar sesión");
		salir.addActionListener(e -> frame.dispose());
		panel.add(clientes);
		panel.add(cuentas);
		panel.add(tarjetas);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}

	public JFrame getFrame() {
		return frame;
	}
}
