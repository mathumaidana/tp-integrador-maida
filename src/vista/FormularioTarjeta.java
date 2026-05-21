package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.YearMonth;
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
import entidades.Movimiento;
import entidades.Tarjeta;
import persistencia.ClienteDao;
import persistencia.MovimientoDao;
import persistencia.TarjetaDao;
import servicio.ClienteServicio;
import servicio.GrabandoException;
import servicio.LeyendoException;
import servicio.SaldoInsuficienteException;
import servicio.TarjetaDuplicadaException;
import servicio.TarjetaInexistenteException;
import servicio.TarjetaServicio;

public class FormularioTarjeta {

	private static final int MARGIN = 12;

	private final JFrame frame;
	private final boolean modoAdmin;
	private final Cliente clienteFijo;

	private JTextField idField;
	private JComboBox<Cliente> titularCombo;
	private JTextField numeroField;
	private JTextField disponibleField;
	private JTextField saldoAPagarField;

	private DefaultListModel<Tarjeta> modelo;
	private JList<Tarjeta> lista;

	private final TarjetaServicio tarjetaServicio;
	private final ClienteServicio clienteServicio;

	public FormularioTarjeta() {
		this(null);
	}

	public FormularioTarjeta(Cliente cliente) {
		this.clienteFijo = cliente;
		this.modoAdmin = (cliente == null);
		this.tarjetaServicio = new TarjetaServicio(new TarjetaDao(), new MovimientoDao());
		this.clienteServicio = new ClienteServicio(new ClienteDao());

		String titulo = modoAdmin ? "Tarjetas" : "Mis tarjetas";
		this.frame = new JFrame(titulo);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		if (modoAdmin) {
			root.add(crearPanelFormulario(), BorderLayout.NORTH);
		}
		root.add(crearPanelLista(), BorderLayout.CENTER);
		root.add(crearPanelBotones(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setMinimumSize(new Dimension(620, modoAdmin ? 520 : 360));
		frame.setSize(720, modoAdmin ? 580 : 400);
		frame.setLocationRelativeTo(null);

		if (modoAdmin) cargarTitulares();
		refrescar();
		frame.setVisible(true);
	}

	private JPanel crearPanelFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Tarjeta"));

		JPanel grid = new JPanel(new GridLayout(5, 2, 8, 6));
		grid.add(new JLabel("Id"));
		idField = new JTextField();
		idField.setEditable(false);
		grid.add(idField);

		grid.add(new JLabel("Titular"));
		titularCombo = new JComboBox<>();
		grid.add(titularCombo);

		grid.add(new JLabel("Número"));
		numeroField = new JTextField();
		grid.add(numeroField);

		grid.add(new JLabel("Disponible"));
		disponibleField = new JTextField("0");
		grid.add(disponibleField);

		grid.add(new JLabel("Saldo a pagar"));
		saldoAPagarField = new JTextField("0");
		grid.add(saldoAPagarField);

		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelLista() {
		JPanel wrap = new JPanel(new BorderLayout());
		String titulo = modoAdmin ? "Lista" : "Mis tarjetas";
		wrap.setBorder(BorderFactory.createTitledBorder(titulo));

		modelo = new DefaultListModel<>();
		lista = new JList<>(modelo);
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setVisibleRowCount(8);
		if (modoAdmin) {
			lista.addListSelectionListener(e -> {
				if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
					cargarEnFormulario(lista.getSelectedValue());
				}
			});
		}
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(420, 240));
		wrap.add(scroll, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelBotones() {
		JPanel bar = new JPanel(new BorderLayout(0, 8));

		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		if (modoAdmin) {
			JButton limpiar = new JButton("Limpiar");
			limpiar.addActionListener(e -> limpiarFormulario());
			JButton grabar = new JButton("Guardar");
			grabar.addActionListener(e -> grabarOModificar());
			JButton borrar = new JButton("Eliminar");
			borrar.addActionListener(e -> borrarSeleccionada());
			JButton debitar = new JButton("Debitar");
			debitar.addActionListener(e -> registrarMovimiento(true));
			JButton pagar = new JButton("Pagar");
			pagar.addActionListener(e -> registrarMovimiento(false));
			acciones.add(limpiar);
			acciones.add(grabar);
			acciones.add(borrar);
			acciones.add(debitar);
			acciones.add(pagar);
		}
		JButton resumen = new JButton("Resumen mensual");
		resumen.addActionListener(e -> mostrarResumenMensual());
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> {
			if (modoAdmin) cargarTitulares();
			refrescar();
		});
		acciones.add(resumen);
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
			for (Cliente c : clienteServicio.listar()) titularCombo.addItem(c);
			if (seleccionado instanceof Cliente) {
				for (int i = 0; i < titularCombo.getItemCount(); i++) {
					if (titularCombo.getItemAt(i).getId().equals(((Cliente) seleccionado).getId())) {
						titularCombo.setSelectedIndex(i);
						break;
					}
				}
			}
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void cargarEnFormulario(Tarjeta t) {
		idField.setText(String.valueOf(t.getId()));
		if (t.getTitular() != null) {
			for (int i = 0; i < titularCombo.getItemCount(); i++) {
				if (titularCombo.getItemAt(i).getId().equals(t.getTitular().getId())) {
					titularCombo.setSelectedIndex(i);
					break;
				}
			}
		}
		numeroField.setText(t.getNumero());
		disponibleField.setText(String.valueOf(t.getDisponible()));
		saldoAPagarField.setText(String.valueOf(t.getSaldoAPagar()));
	}

	private void limpiarFormulario() {
		lista.clearSelection();
		idField.setText("");
		if (titularCombo.getItemCount() > 0) titularCombo.setSelectedIndex(0);
		numeroField.setText("");
		disponibleField.setText("0");
		saldoAPagarField.setText("0");
	}

	private Tarjeta tomarTarjetaDelFormulario() {
		Cliente titular = (Cliente) titularCombo.getSelectedItem();
		if (titular == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná un titular", "Aviso", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		if (numeroField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(frame, "Ingresá el número", "Aviso", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		try {
			Tarjeta t = new Tarjeta();
			t.setTitular(titular);
			t.setNumero(numeroField.getText().trim());
			t.setDisponible(Double.valueOf(disponibleField.getText().trim()));
			t.setSaldoAPagar(Double.valueOf(saldoAPagarField.getText().trim()));
			return t;
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Disponible o saldo inválido",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return null;
		}
	}

	private void grabarOModificar() {
		Tarjeta t = tomarTarjetaDelFormulario();
		if (t == null) return;
		try {
			if (idField.getText().trim().isEmpty()) {
				tarjetaServicio.agregar(t);
				JOptionPane.showMessageDialog(frame, "Tarjeta guardada.",
					"Aviso", JOptionPane.INFORMATION_MESSAGE);
			} else {
				t.setId(Integer.valueOf(idField.getText()));
				tarjetaServicio.modificar(t);
				JOptionPane.showMessageDialog(frame, "Tarjeta actualizada.",
					"Aviso", JOptionPane.INFORMATION_MESSAGE);
			}
			limpiarFormulario();
			refrescar();
		} catch (TarjetaDuplicadaException | TarjetaInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void borrarSeleccionada() {
		Tarjeta sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una tarjeta", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(frame,
			"¿Borrar la tarjeta '" + sel + "'? Se eliminarán también sus movimientos.",
			"Confirmar", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		try {
			tarjetaServicio.borrar(sel.getId());
			limpiarFormulario();
			refrescar();
		} catch (TarjetaInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void registrarMovimiento(boolean esDebito) {
		Tarjeta sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una tarjeta de la lista",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String prompt = esDebito ? "Monto del débito" : "Monto del pago";
		String input = JOptionPane.showInputDialog(frame, prompt + " para " + sel + ":");
		if (input == null || input.trim().isEmpty()) return;
		Double monto;
		try {
			monto = Double.valueOf(input.trim());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Monto inválido", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String descripcion = JOptionPane.showInputDialog(frame, "Descripción (opcional):");
		try {
			if (esDebito) {
				tarjetaServicio.debitar(sel, monto, descripcion);
				JOptionPane.showMessageDialog(frame, "Débito registrado.",
					"Aviso", JOptionPane.INFORMATION_MESSAGE);
			} else {
				tarjetaServicio.pagar(sel, monto, descripcion);
				JOptionPane.showMessageDialog(frame, "Pago registrado.",
					"Aviso", JOptionPane.INFORMATION_MESSAGE);
			}
			refrescar();
		} catch (SaldoInsuficienteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saldo", JOptionPane.WARNING_MESSAGE);
		} catch (IllegalArgumentException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void mostrarResumenMensual() {
		Tarjeta sel = lista.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná una tarjeta para ver su resumen",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		String input = JOptionPane.showInputDialog(frame,
			"Ingresá el mes en formato AAAA-MM (ej: 2026-05):",
			YearMonth.now().toString());
		if (input == null || input.trim().isEmpty()) return;
		YearMonth ym;
		try {
			ym = YearMonth.parse(input.trim());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Formato inválido. Usá AAAA-MM",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			List<Movimiento> movs = tarjetaServicio.resumenMensual(sel, ym);
			StringBuilder sb = new StringBuilder();
			sb.append("Resumen de ").append(sel).append("\nMes: ").append(ym).append("\n\n");
			double total = 0;
			if (movs.isEmpty()) {
				sb.append("(sin movimientos en este mes)");
			} else {
				for (Movimiento m : movs) {
					sb.append(m).append('\n');
					total += m.getMonto();
				}
				sb.append("\nTotal del período: $").append(total);
			}
			JOptionPane.showMessageDialog(frame, sb.toString(), "Resumen mensual",
				JOptionPane.INFORMATION_MESSAGE);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void refrescar() {
		try {
			List<Tarjeta> tarjetas = modoAdmin
				? tarjetaServicio.listar()
				: tarjetaServicio.listarPorCliente(clienteFijo);
			modelo.clear();
			for (Tarjeta t : tarjetas) modelo.addElement(t);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
