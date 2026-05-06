package vista;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import entidades.Cliente;

public class MenuClienteView {

	private final JFrame frame;
	private final Cliente cliente;

	public MenuClienteView(Cliente cliente) {
		this.cliente = cliente;
		this.frame = new JFrame("Mini Home Banking - " + cliente.getNombre());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(crearEncabezado(), BorderLayout.NORTH);
		frame.add(crearPanel(), BorderLayout.CENTER);
		frame.setSize(420, 320);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JLabel crearEncabezado() {
		JLabel label = new JLabel("Hola, " + cliente.getNombre() + " " + cliente.getApellido(), JLabel.CENTER);
		return label;
	}

	private JPanel crearPanel() {
		JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
		JButton cuentas = new JButton("Mis cuentas");
		cuentas.addActionListener(e -> new ResumenView(cliente));
		JButton transferir = new JButton("Realizar transferencia");
		transferir.addActionListener(e -> new FormularioTransferencia(cliente));
		JButton tarjetas = new JButton("Mis tarjetas");
		tarjetas.addActionListener(e -> new FormularioTarjeta(cliente));
		JButton resumen = new JButton("Ver movimientos");
		resumen.addActionListener(e -> new ResumenView(cliente));
		JButton salir = new JButton("Cerrar sesión");
		salir.addActionListener(e -> frame.dispose());
		panel.add(cuentas);
		panel.add(transferir);
		panel.add(tarjetas);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}

	public JFrame getFrame() {
		return frame;
	}
}
