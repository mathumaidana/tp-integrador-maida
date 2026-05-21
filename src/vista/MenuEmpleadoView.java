package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Empleado;

public class MenuEmpleadoView extends MenuView {

	public MenuEmpleadoView(Empleado empleado) {
		super(empleado);
	}

	@Override
	protected String tituloVentana() {
		return "Mesa de atención";
	}

	@Override
	protected void configurarTamano() {
		frame.setSize(440, 360);
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton clientes = new JButton("Clientes");
		clientes.addActionListener(e -> new FormularioCliente());

		JButton cuentas = new JButton("Cuentas");
		cuentas.addActionListener(e -> new FormularioCuenta());

		JButton tarjetas = new JButton("Tarjetas");
		tarjetas.addActionListener(e -> new FormularioTarjeta());

		JButton resumen = new JButton("Movimientos");
		resumen.addActionListener(e -> new ResumenView(null));

		JButton salir = new JButton("Salir");
		salir.addActionListener(e -> frame.dispose());

		panel.add(clientes);
		panel.add(cuentas);
		panel.add(tarjetas);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}
}
