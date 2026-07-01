package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import entidades.Cheque;
import entidades.Cliente;
import entidades.Cuenta;
import entidades.SaldoInsuficienteException;
import persistencia.ChequeDao;
import persistencia.CuentaDao;
import servicio.ChequeDuplicadoException;
import servicio.ChequeServicio;
import servicio.CuentaServicio;
import servicio.DatosInvalidosException;
import servicio.GrabandoException;
import servicio.LeyendoException;
import servicio.OperacionNoPermitidaException;

public class FormularioCheque {

	private static final int MARGIN = 12;

	private final JFrame frame;
	private final boolean modoEmpleado;
	private final Cliente clienteFijo;

	private JComboBox<Cuenta> cuentaCombo;
	private JTextField numeroField;
	private JTextField montoField;
	private JTextField beneficiarioField;

	private DefaultListModel<Cheque> modelo;
	private JList<Cheque> lista;
	private boolean cargandoCuentas;

	private final ChequeServicio chequeServicio;
	private final CuentaServicio cuentaServicio;

	public JFrame getFrame() {
		return frame;
	}

	public FormularioCheque() {
		this(null);
	}

	public FormularioCheque(Cliente cliente) {
		this.clienteFijo = cliente;
		this.modoEmpleado = (cliente == null);
		this.chequeServicio = new ChequeServicio(new ChequeDao());
		this.cuentaServicio = new CuentaServicio(new CuentaDao());

		this.frame = new JFrame("Cheques");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearPanelFormulario(), BorderLayout.NORTH);
		root.add(crearPanelLista(), BorderLayout.CENTER);
		root.add(crearPanelBotones(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setMinimumSize(new Dimension(620, 500));
		frame.setSize(720, 540);
		frame.setLocationRelativeTo(null);

		cargarCuentas();
		refrescar();
		frame.setVisible(true);
	}

	private JPanel crearPanelFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Emitir cheque"));

		JPanel grid = new JPanel(new GridLayout(4, 2, 8, 6));
		grid.add(new JLabel("Cuenta emisora"));
		cuentaCombo = new JComboBox<>();
		cuentaCombo.addActionListener(e -> { if (!cargandoCuentas) refrescar(); });
		grid.add(cuentaCombo);

		grid.add(new JLabel("Número"));
		numeroField = new JTextField();
		grid.add(numeroField);

		grid.add(new JLabel("Monto"));
		montoField = new JTextField();
		grid.add(montoField);

		grid.add(new JLabel("Beneficiario"));
		beneficiarioField = new JTextField();
		grid.add(beneficiarioField);

		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelLista() {
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBorder(BorderFactory.createTitledBorder("Cheques de la cuenta seleccionada"));
		modelo = new DefaultListModel<>();
		lista = new JList<>(modelo);
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setVisibleRowCount(8);
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(420, 240));
		wrap.add(scroll, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelBotones() {
		JPanel bar = new JPanel(new BorderLayout(0, 8));
		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));

		JButton emitir = new JButton("Emitir");
		emitir.addActionListener(e -> emitir());
		JButton cobrar = new JButton("Cobrar");
		cobrar.addActionListener(e -> cobrar());
		JButton anular = new JButton("Anular");
		anular.addActionListener(e -> anular());
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> { cargarCuentas(); refrescar(); });
		acciones.add(emitir);
		acciones.add(cobrar);
		acciones.add(anular);
		acciones.add(refrescar);

		JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton volver = new JButton("Volver");
		volver.addActionListener(e -> frame.dispose());
		nav.add(volver);

		bar.add(acciones, BorderLayout.CENTER);
		bar.add(nav, BorderLayout.SOUTH);
		return bar;
	}

	private void cargarCuentas() {
		Object sel = cuentaCombo.getSelectedItem();
		cargandoCuentas = true;
		try {
			cuentaCombo.removeAllItems();
			List<Cuenta> cuentas = modoEmpleado
				? cuentaServicio.listar()
				: cuentaServicio.listarPorCliente(clienteFijo);
			for (Cuenta c : cuentas) if (c.permiteCheques()) cuentaCombo.addItem(c);
			if (sel instanceof Cuenta) {
				for (int i = 0; i < cuentaCombo.getItemCount(); i++) {
					if (cuentaCombo.getItemAt(i).getId().equals(((Cuenta) sel).getId())) {
						cuentaCombo.setSelectedIndex(i);
						break;
					}
				}
			}
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} finally {
			cargandoCuentas = false;
		}
	}

	private void emitir() {
		Cuenta cuenta = (Cuenta) cuentaCombo.getSelectedItem();
		if (cuenta == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una cuenta emisora",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (numeroField.getText().trim().isEmpty() || beneficiarioField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Completá número y beneficiario",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Double monto;
		try {
			monto = Double.valueOf(montoField.getText().trim());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Monto inválido", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Cheque ch = new Cheque();
		ch.setCuenta(cuenta);
		ch.setNumero(numeroField.getText().trim());
		ch.setMonto(monto);
		ch.setBeneficiario(beneficiarioField.getText().trim());
		try {
			chequeServicio.emitir(ch);
			JOptionPane.showMessageDialog(frame, "Cheque emitido (pendiente).",
				"Aviso", JOptionPane.INFORMATION_MESSAGE);
			limpiarFormulario();
			refrescar();
		} catch (OperacionNoPermitidaException | ChequeDuplicadoException | DatosInvalidosException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void cobrar() {
		Cheque sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná un cheque de la lista",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(frame,
			"¿Cobrar el cheque " + sel.getNumero() + " por $" + String.format("%.2f", sel.getMonto())
			+ "? Se debitará de la cuenta.",
			"Confirmar", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		try {
			chequeServicio.cobrar(sel);
			JOptionPane.showMessageDialog(frame, "Cheque cobrado.",
				"Aviso", JOptionPane.INFORMATION_MESSAGE);
			cargarCuentas();
			refrescar();
		} catch (SaldoInsuficienteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saldo", JOptionPane.WARNING_MESSAGE);
		} catch (OperacionNoPermitidaException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void anular() {
		Cheque sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná un cheque de la lista",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			chequeServicio.anular(sel);
			JOptionPane.showMessageDialog(frame, "Cheque anulado.",
				"Aviso", JOptionPane.INFORMATION_MESSAGE);
			refrescar();
		} catch (OperacionNoPermitidaException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void limpiarFormulario() {
		numeroField.setText("");
		montoField.setText("");
		beneficiarioField.setText("");
	}

	private void refrescar() {
		if (modelo == null) return;
		modelo.clear();
		Cuenta cuenta = (Cuenta) cuentaCombo.getSelectedItem();
		if (cuenta == null) return;
		try {
			for (Cheque ch : chequeServicio.listarPorCuenta(cuenta)) modelo.addElement(ch);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
