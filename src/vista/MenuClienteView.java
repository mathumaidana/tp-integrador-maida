package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Cliente;

public class MenuClienteView extends MenuView {

	public MenuClienteView(Cliente cliente) {
		super(cliente);
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton cuentas = new JButton("Mis cuentas");
		cuentas.addActionListener(e -> new MisCuentasView(cliente()));

		JButton movimientos = new JButton("Movimientos");
		movimientos.addActionListener(e -> new ResumenView(cliente()));

		JButton transferir = new JButton("Transferencias");
		transferir.addActionListener(e -> new FormularioTransferencia(cliente()));

		JButton tarjetas = new JButton("Mis tarjetas");
		tarjetas.addActionListener(e -> new FormularioTarjeta(cliente()));

		JButton salir = new JButton("Salir");
		salir.addActionListener(e -> frame.dispose());

		panel.add(cuentas);
		panel.add(movimientos);
		panel.add(transferir);
		panel.add(tarjetas);
		panel.add(salir);
		return panel;
	}
}
