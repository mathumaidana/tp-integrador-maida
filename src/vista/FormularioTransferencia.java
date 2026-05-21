package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import entidades.Cliente;
import entidades.Cuenta;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;
import servicio.CuentaServicio;
import servicio.GrabandoException;
import servicio.LeyendoException;
import servicio.SaldoInsuficienteException;
import servicio.TransferenciaInvalidaException;
import servicio.TransferenciaServicio;

public class FormularioTransferencia {

	private static final int MARGIN = 12;
	private static final String[] MODOS_BUSQUEDA = { "CBU", "Alias" };

	private final JFrame frame;
	private final Cliente cliente;

	private JComboBox<Cuenta> origenCombo;
	private JComboBox<String> modoBusquedaCombo;
	private JTextField destinoField;
	private JLabel destinoResueltoLbl;
	private JTextField montoField;
	private JTextField descripcionField;

	private Cuenta destinoActual;

	private final CuentaServicio cuentaServicio;
	private final TransferenciaServicio transferenciaServicio;

	public FormularioTransferencia(Cliente cliente) {
		this.cliente = cliente;
		CuentaDao cuentaDao = new CuentaDao();
		this.cuentaServicio = new CuentaServicio(cuentaDao);
		this.transferenciaServicio = new TransferenciaServicio(cuentaDao, new MovimientoDao());

		this.frame = new JFrame("Transferencias");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearFormulario(), BorderLayout.CENTER);
		root.add(crearBotonera(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setMinimumSize(new Dimension(640, 360));
		frame.setSize(720, 380);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JPanel crearFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 8));
		wrap.setBorder(BorderFactory.createTitledBorder("Datos de la transferencia"));

		JPanel grid = new JPanel(new GridLayout(6, 2, 8, 6));

		grid.add(new JLabel("Cuenta origen:"));
		origenCombo = new JComboBox<>();
		try {
			List<Cuenta> mias = cuentaServicio.listarPorCliente(cliente);
			for (Cuenta c : mias) origenCombo.addItem(c);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		grid.add(origenCombo);

		grid.add(new JLabel("Buscar destino por:"));
		modoBusquedaCombo = new JComboBox<>(MODOS_BUSQUEDA);
		grid.add(modoBusquedaCombo);

		grid.add(new JLabel("Valor:"));
		destinoField = new JTextField();
		grid.add(destinoField);

		grid.add(new JLabel("Cuenta destino:"));
		destinoResueltoLbl = new JLabel("(usá 'Buscar destino' para validar)");
		destinoResueltoLbl.setPreferredSize(new Dimension(260, 24));
		grid.add(destinoResueltoLbl);

		grid.add(new JLabel("Monto:"));
		montoField = new JTextField();
		grid.add(montoField);

		grid.add(new JLabel("Descripción:"));
		descripcionField = new JTextField();
		grid.add(descripcionField);

		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearBotonera() {
		JPanel bar = new JPanel(new BorderLayout(0, 8));

		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton buscar = new JButton("Buscar destino");
		buscar.addActionListener(e -> buscarDestino());
		JButton transferir = new JButton("Transferir");
		transferir.addActionListener(e -> ejecutar());
		acciones.add(buscar);
		acciones.add(transferir);

		JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton volver = new JButton("Volver");
		volver.addActionListener(e -> frame.dispose());
		nav.add(volver);

		bar.add(acciones, BorderLayout.CENTER);
		bar.add(nav, BorderLayout.SOUTH);
		return bar;
	}

	private void buscarDestino() {
		destinoActual = null;
		destinoResueltoLbl.setText("(sin resolver)");

		Cuenta origen = (Cuenta) origenCombo.getSelectedItem();
		String valor = destinoField.getText().trim();
		if (valor.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Ingresá un CBU o alias para buscar el destino",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String modo = (String) modoBusquedaCombo.getSelectedItem();
		try {
			Cuenta destino = "CBU".equals(modo)
				? cuentaServicio.buscarPorCbu(valor)
				: cuentaServicio.buscarPorAlias(valor);
			if (destino == null) {
				destinoResueltoLbl.setText("(no encontrada)");
				JOptionPane.showMessageDialog(frame, "No se encontró ninguna cuenta con ese " + modo,
					"Aviso", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (origen != null && origen.esLaMismaQue(destino)) {
				destinoResueltoLbl.setText("(es tu misma cuenta)");
				JOptionPane.showMessageDialog(frame, "No podés transferirte a la misma cuenta.",
					"Aviso", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (origen != null && !origen.mismaMonedaQue(destino)) {
				destinoResueltoLbl.setText("(moneda distinta: " + destino.getMoneda() + ")");
				JOptionPane.showMessageDialog(frame,
					"La cuenta destino opera en " + destino.getMoneda() + " y la origen en " + origen.getMoneda() + ".",
					"Aviso", JOptionPane.WARNING_MESSAGE);
				return;
			}
			destinoActual = destino;
			destinoResueltoLbl.setText(destino.getTipo() + " " + destino.referencia());
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void ejecutar() {
		Cuenta origen = (Cuenta) origenCombo.getSelectedItem();
		if (origen == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una cuenta origen",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (destinoActual == null) {
			JOptionPane.showMessageDialog(frame, "Buscá y validá la cuenta destino antes de transferir.",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (montoField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Ingresá el monto", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Double monto;
		try {
			monto = Double.valueOf(montoField.getText().trim());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Monto inválido", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			transferenciaServicio.transferir(origen, destinoActual, monto, descripcionField.getText().trim());
			JOptionPane.showMessageDialog(frame,
				"Transferencia hecha. Saldo de tu cuenta: $" + origen.getSaldo(),
				"Aviso", JOptionPane.INFORMATION_MESSAGE);
			frame.dispose();
		} catch (TransferenciaInvalidaException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (SaldoInsuficienteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saldo", JOptionPane.WARNING_MESSAGE);
		} catch (IllegalArgumentException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
