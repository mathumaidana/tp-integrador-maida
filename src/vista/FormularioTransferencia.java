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
import servicio.TransferenciaServicio;

public class FormularioTransferencia {

	private static final int MARGIN = 12;
	private static final String[] MODOS_BUSQUEDA = { "Id", "CBU", "Alias" };

	private final JFrame frame;
	private final Cliente cliente;

	private JComboBox<Cuenta> origenCombo;
	private JComboBox<String> modoBusquedaCombo;
	private JTextField destinoField;
	private JLabel destinoResueltoLbl;
	private JTextField montoField;
	private JTextField descripcionField;

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
		frame.setSize(540, 320);
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
		destinoResueltoLbl.setPreferredSize(new Dimension(220, 24));
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

	private Cuenta buscarDestino() {
		String valor = destinoField.getText().trim();
		if (valor.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Ingresá un valor para buscar el destino",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		String modo = (String) modoBusquedaCombo.getSelectedItem();
		try {
			Cuenta destino = null;
			if ("Id".equals(modo)) {
				destino = cuentaServicio.leer(Integer.valueOf(valor));
			} else if ("CBU".equals(modo)) {
				destino = cuentaServicio.buscarPorCbu(valor);
			} else if ("Alias".equals(modo)) {
				destino = cuentaServicio.buscarPorAlias(valor);
			}
			if (destino == null) {
				destinoResueltoLbl.setText("(no encontrada)");
				JOptionPane.showMessageDialog(frame, "No se encontró ninguna cuenta con ese " + modo,
					"Aviso", JOptionPane.WARNING_MESSAGE);
				return null;
			}
			destinoResueltoLbl.setText(destino.toString());
			return destino;
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Id inválido", "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	private void ejecutar() {
		Cuenta origen = (Cuenta) origenCombo.getSelectedItem();
		if (origen == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una cuenta origen",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Cuenta destino = buscarDestino();
		if (destino == null) return;
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
			transferenciaServicio.transferir(origen, destino, monto, descripcionField.getText().trim());
			JOptionPane.showMessageDialog(frame,
				"Transferencia hecha. Saldo origen: " + origen.getSaldo(),
				"Aviso", JOptionPane.INFORMATION_MESSAGE);
			frame.dispose();
		} catch (SaldoInsuficienteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saldo", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
