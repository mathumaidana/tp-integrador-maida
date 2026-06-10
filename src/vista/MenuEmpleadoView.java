package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Empleado;

public class MenuEmpleadoView extends MenuView {

	private final Empleado empleado;

	public MenuEmpleadoView(Empleado empleado) {
		super(empleado);
		this.empleado = empleado;
	}

	@Override
	protected String tituloVentana() {
		return "Panel del banco";
	}

	@Override
	protected void configurarTamano() {
		frame.setSize(440, 420);
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(6, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton empleados = new JButton("Empleados");
		empleados.addActionListener(e -> registrarVentana(new FormularioEmpleado(empleado).getFrame()));

		JButton clientes = new JButton("Clientes");
		clientes.addActionListener(e -> registrarVentana(new FormularioCliente().getFrame()));

		JButton cuentas = new JButton("Cuentas");
		cuentas.addActionListener(e -> registrarVentana(new FormularioCuenta().getFrame()));

		JButton tarjetas = new JButton("Tarjetas");
		tarjetas.addActionListener(e -> registrarVentana(new FormularioTarjeta().getFrame()));

		JButton resumen = new JButton("Movimientos");
		resumen.addActionListener(e -> registrarVentana(new ResumenView(null).getFrame()));

		JButton salir = new JButton("Salir");
		salir.addActionListener(e -> frame.dispose());

		panel.add(empleados);
		panel.add(clientes);
		panel.add(cuentas);
		panel.add(tarjetas);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}
}
