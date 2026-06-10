package vista;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import entidades.Cliente;

public class MenuClienteView extends MenuView {

	private final Cliente cliente;

	public MenuClienteView(Cliente cliente) {
		super(cliente);
		this.cliente = cliente;
	}

	@Override
	protected String tituloVentana() {
		return "Home Banking";
	}

	@Override
	protected JPanel crearPanelOpciones() {
		JPanel panel = new JPanel(new GridLayout(5, 1, 8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JButton cuentas = new JButton("Mis cuentas");
		cuentas.addActionListener(e -> registrarVentana(new MisCuentasView(cliente).getFrame()));

		JButton movimientos = new JButton("Movimientos");
		movimientos.addActionListener(e -> registrarVentana(new ResumenView(cliente).getFrame()));

		JButton transferir = new JButton("Transferencias");
		transferir.addActionListener(e -> registrarVentana(new FormularioTransferencia(cliente).getFrame()));

		JButton tarjetas = new JButton("Mis tarjetas");
		tarjetas.addActionListener(e -> registrarVentana(new FormularioTarjeta(cliente).getFrame()));

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
