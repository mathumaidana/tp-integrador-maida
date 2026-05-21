package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Administrador;

public class MenuAdminView extends MenuView {

	private final Administrador administrador;

	public MenuAdminView(Administrador administrador) {
		super(administrador);
		this.administrador = administrador;
	}

	@Override
	protected String tituloVentana() {
		return "Panel de administración";
	}

	@Override
	protected void configurarTamano() {
		frame.setSize(440, 300);
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(3, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton empleados = new JButton("Empleados");
		empleados.addActionListener(e -> new FormularioEmpleado(administrador));

		JButton resumen = new JButton("Movimientos");
		resumen.addActionListener(e -> new ResumenView(null));

		JButton salir = new JButton("Salir");
		salir.addActionListener(e -> frame.dispose());

		panel.add(empleados);
		panel.add(resumen);
		panel.add(salir);
		return panel;
	}
}
