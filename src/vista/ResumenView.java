package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
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
import javax.swing.border.EmptyBorder;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.Movimiento;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;
import servicio.CuentaServicio;
import servicio.LeyendoException;

public class ResumenView {

	private static final int MARGIN = 12;

	private final JFrame frame;
	private final Cliente cliente;
	private final CuentaServicio cuentaServicio;
	private final MovimientoDao movimientoDao;
	private JComboBox<Cuenta> cuentaCombo;
	private JTextField mesField;
	private DefaultListModel<Movimiento> modelo;
	private JLabel totalLbl;

	public ResumenView(Cliente cliente) {
		this.cliente = cliente;
		this.cuentaServicio = new CuentaServicio(new CuentaDao());
		this.movimientoDao = new MovimientoDao();
		this.frame = new JFrame("Movimientos");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearFiltros(), BorderLayout.NORTH);
		root.add(crearListado(), BorderLayout.CENTER);
		root.add(crearBotonera(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setSize(680, 460);
		frame.setLocationRelativeTo(null);
		refrescar();
		frame.setVisible(true);
	}

	private JPanel crearFiltros() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Filtros"));

		JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		fila.add(new JLabel("Cuenta:"));
		cuentaCombo = new JComboBox<>();
		try {
			List<Cuenta> cuentas = (cliente == null)
				? cuentaServicio.listar()
				: cuentaServicio.listarPorCliente(cliente);
			for (Cuenta c : cuentas) cuentaCombo.addItem(c);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		cuentaCombo.addActionListener(e -> refrescar());
		fila.add(cuentaCombo);

		fila.add(new JLabel("Mes (AAAA-MM):"));
		mesField = new JTextField(8);
		fila.add(mesField);

		JButton aplicar = new JButton("Buscar");
		aplicar.addActionListener(e -> refrescar());
		fila.add(aplicar);

		JButton limpiar = new JButton("Ver todo");
		limpiar.addActionListener(e -> {
			mesField.setText("");
			refrescar();
		});
		fila.add(limpiar);

		wrap.add(fila, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearListado() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Movimientos"));

		modelo = new DefaultListModel<>();
		JList<Movimiento> lista = new JList<>(modelo);
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(620, 280));
		wrap.add(scroll, BorderLayout.CENTER);

		totalLbl = new JLabel("Total: $0");
		wrap.add(totalLbl, BorderLayout.SOUTH);
		return wrap;
	}

	private JPanel crearBotonera() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton volver = new JButton("Volver");
		volver.addActionListener(e -> frame.dispose());
		bar.add(volver);
		return bar;
	}

	private void refrescar() {
		Cuenta cuenta = (Cuenta) cuentaCombo.getSelectedItem();
		modelo.clear();
		totalLbl.setText("Total: $0");
		if (cuenta == null) return;
		try {
			List<Movimiento> movs = filtrar(movimientoDao.leerPorCuenta(cuenta));
			double total = 0;
			for (Movimiento m : movs) {
				modelo.addElement(m);
				total += signo(m) * m.getMonto();
			}
			totalLbl.setText("Mostrando " + movs.size() + " movimientos. Balance del período: $" + total);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private List<Movimiento> filtrar(List<Movimiento> entrada) {
		String txt = mesField.getText().trim();
		if (txt.isEmpty()) return entrada;
		YearMonth ym;
		try {
			ym = YearMonth.parse(txt);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Formato inválido. Usá AAAA-MM",
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return entrada;
		}
		List<Movimiento> out = new ArrayList<>();
		for (Movimiento m : entrada) {
			if (m.getFecha() != null && YearMonth.from(m.getFecha()).equals(ym)) out.add(m);
		}
		return out;
	}

	private int signo(Movimiento m) {
		switch (m.getTipo()) {
			case CREDITO:
			case TRANSFERENCIA_RECIBIDA:
			case PAGO_TARJETA:
				return 1;
			default:
				return -1;
		}
	}
}
