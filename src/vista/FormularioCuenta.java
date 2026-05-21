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

import entidades.Cliente;
import entidades.Cuenta;
import entidades.TipoCuenta;
import persistencia.ClienteDao;
import persistencia.CuentaDao;
import servicio.ClienteServicio;
import servicio.CuentaDuplicadaException;
import servicio.CuentaInexistenteException;
import servicio.CuentaServicio;
import servicio.GrabandoException;
import servicio.LeyendoException;

public class FormularioCuenta {

	private static final int MARGIN = 12;

	private final JFrame frame;

	private JTextField idField;
	private JComboBox<Cliente> titularCombo;
	private JComboBox<TipoCuenta> tipoCombo;
	private JTextField aliasField;
	private JTextField cbuField;
	private JTextField saldoField;

	private JList<Cuenta> lista;
	private DefaultListModel<Cuenta> modelo;

	private final CuentaServicio cuentaServicio;
	private final ClienteServicio clienteServicio;

	public FormularioCuenta() {
		this.cuentaServicio = new CuentaServicio(new CuentaDao());
		this.clienteServicio = new ClienteServicio(new ClienteDao());

		this.frame = new JFrame("Cuentas");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearPanelFormulario(), BorderLayout.NORTH);
		root.add(crearPanelLista(), BorderLayout.CENTER);
		root.add(crearPanelBotones(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setMinimumSize(new Dimension(620, 480));
		frame.setSize(720, 540);
		frame.setLocationRelativeTo(null);

		cargarTitulares();
		refrescarLista();
		frame.setVisible(true);
	}

	private JPanel crearPanelFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Cuenta"));

		JPanel grid = new JPanel(new GridLayout(6, 2, 8, 6));
		grid.add(new JLabel("Id"));
		idField = new JTextField();
		idField.setEditable(false);
		grid.add(idField);

		grid.add(new JLabel("Titular"));
		titularCombo = new JComboBox<>();
		grid.add(titularCombo);

		grid.add(new JLabel("Tipo"));
		tipoCombo = new JComboBox<>(TipoCuenta.values());
		grid.add(tipoCombo);

		grid.add(new JLabel("Alias"));
		aliasField = new JTextField();
		grid.add(aliasField);

		grid.add(new JLabel("CBU"));
		cbuField = new JTextField();
		grid.add(cbuField);

		grid.add(new JLabel("Saldo"));
		saldoField = new JTextField("0");
		grid.add(saldoField);

		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelLista() {
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBorder(BorderFactory.createTitledBorder("Lista"));

		modelo = new DefaultListModel<>();
		lista = new JList<>(modelo);
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setVisibleRowCount(8);
		lista.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
				cargarEnFormulario(lista.getSelectedValue());
			}
		});

		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(420, 240));
		wrap.add(scroll, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelBotones() {
		JPanel bar = new JPanel(new BorderLayout(0, 8));

		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton limpiar = new JButton("Limpiar");
		limpiar.addActionListener(e -> limpiarFormulario());
		JButton grabar = new JButton("Guardar");
		grabar.addActionListener(e -> grabarOModificar());
		JButton borrar = new JButton("Eliminar");
		borrar.addActionListener(e -> borrarSeleccionada());
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> {
			cargarTitulares();
			refrescarLista();
		});
		acciones.add(limpiar);
		acciones.add(grabar);
		acciones.add(borrar);
		acciones.add(refrescar);

		JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton volver = new JButton("Volver");
		volver.addActionListener(e -> frame.dispose());
		nav.add(volver);

		bar.add(acciones, BorderLayout.CENTER);
		bar.add(nav, BorderLayout.SOUTH);
		return bar;
	}

	private void cargarTitulares() {
		Object seleccionado = titularCombo.getSelectedItem();
		titularCombo.removeAllItems();
		try {
			List<Cliente> clientes = clienteServicio.listar();
			for (Cliente c : clientes) titularCombo.addItem(c);
			if (seleccionado instanceof Cliente) {
				seleccionarCliente((Cliente) seleccionado);
			}
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void seleccionarCliente(Cliente cliente) {
		for (int i = 0; i < titularCombo.getItemCount(); i++) {
			Cliente item = titularCombo.getItemAt(i);
			if (item.getId().equals(cliente.getId())) {
				titularCombo.setSelectedIndex(i);
				return;
			}
		}
	}

	private void cargarEnFormulario(Cuenta c) {
		idField.setText(String.valueOf(c.getId()));
		if (c.getTitular() != null) seleccionarCliente(c.getTitular());
		tipoCombo.setSelectedItem(c.getTipo());
		aliasField.setText(c.getAlias() != null ? c.getAlias() : "");
		cbuField.setText(c.getCbu() != null ? c.getCbu() : "");
		saldoField.setText(String.valueOf(c.getSaldo()));
	}

	private void limpiarFormulario() {
		lista.clearSelection();
		idField.setText("");
		if (titularCombo.getItemCount() > 0) titularCombo.setSelectedIndex(0);
		tipoCombo.setSelectedIndex(0);
		aliasField.setText("");
		cbuField.setText("");
		saldoField.setText("0");
	}

	private void grabarOModificar() {
		Cliente titular = (Cliente) titularCombo.getSelectedItem();
		if (titular == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná un titular", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		TipoCuenta tipo = (TipoCuenta) tipoCombo.getSelectedItem();
		String alias = aliasField.getText().trim();
		String cbu = cbuField.getText().trim();
		Double saldo;
		try {
			saldo = Double.valueOf(saldoField.getText().trim());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Saldo inválido", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}

		Cuenta cuenta = tipo.nuevaCuenta();
		cuenta.setTitular(titular);
		cuenta.setAlias(alias.isEmpty() ? null : alias);
		cuenta.setCbu(cbu.isEmpty() ? null : cbu);
		cuenta.setSaldo(saldo);

		try {
			if (idField.getText().trim().isEmpty()) {
				cuentaServicio.agregar(cuenta);
				JOptionPane.showMessageDialog(frame, "Cuenta guardada.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
			} else {
				cuenta.setId(Integer.valueOf(idField.getText()));
				cuentaServicio.modificar(cuenta);
				JOptionPane.showMessageDialog(frame, "Cuenta actualizada.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
			}
			limpiarFormulario();
			refrescarLista();
		} catch (CuentaDuplicadaException | CuentaInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void borrarSeleccionada() {
		Cuenta sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una cuenta de la lista",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(frame,
			"¿Borrar la cuenta '" + sel + "'? Se eliminarán también sus movimientos.",
			"Confirmar", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		try {
			cuentaServicio.borrar(sel.getId());
			limpiarFormulario();
			refrescarLista();
		} catch (CuentaInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void refrescarLista() {
		try {
			List<Cuenta> cuentas = cuentaServicio.listar();
			modelo.clear();
			for (Cuenta c : cuentas) modelo.addElement(c);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
