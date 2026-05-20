package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Administrador;

public class MenuAdminView extends MenuView {

	public MenuAdminView(Administrador administrador) {
		super(administrador);
	}

	@Override
	protected void configurarTamano() {
		frame.setSize(440, 380);
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(6, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton clientes = new JButton("Clientes");
		clientes.addActionListener(e -> new FormularioCliente());

		JButton administradores = new JButton("Administradores");
		administradores.addActionListener(e -> new FormularioAdministrador(administrador()));

		JButton cuentas = new JButton("Cuentas");
		cuentas.addActionListener(e -> new FormularioCuenta());

		JButton tarjetas = new JButton("Tarjetas");
		tarjetas.addActionListener(e -> new FormularioTarjeta());

		JButton resumen = new JButton("Movimientos");
		resumen.addActionListener(e -> new ResumenView(null));

		JButton salir = new JButton("Salir");
		salir.addActionListener(e -> frame.dispose());

		panel.add(clientes);
		panel.add(administradores);
		panel.add(cuentas);
		panel.add(tarjetas);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}
}
