package vista;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.Movimiento;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;
import servicio.CuentaServicio;
import servicio.LeyendoException;

public class ResumenView {

	private final JFrame frame;
	private final Cliente cliente;
	private final CuentaServicio cuentaServicio;
	private final MovimientoDao movimientoDao;
	private JComboBox<Cuenta> cuentaCombo;
	private DefaultListModel<Movimiento> modelo;

	public ResumenView(Cliente cliente) {
		this.cliente = cliente;
		this.cuentaServicio = new CuentaServicio(new CuentaDao());
		this.movimientoDao = new MovimientoDao();
		this.frame = new JFrame("Resumen de movimientos");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(crearSelector(), BorderLayout.NORTH);
		frame.add(crearListado(), BorderLayout.CENTER);
		frame.add(crearBotonera(), BorderLayout.SOUTH);
		frame.setSize(640, 420);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JPanel crearSelector() {
		JPanel panel = new JPanel();
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
		panel.add(cuentaCombo);
		return panel;
	}

	private JScrollPane crearListado() {
		modelo = new DefaultListModel<>();
		JList<Movimiento> lista = new JList<>(modelo);
		return new JScrollPane(lista);
	}

	private JPanel crearBotonera() {
		JPanel panel = new JPanel();
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> refrescar());
		JButton cerrar = new JButton("Volver");
		cerrar.addActionListener(e -> frame.dispose());
		panel.add(refrescar);
		panel.add(cerrar);
		return panel;
	}

	private void refrescar() {
		Cuenta cuenta = (Cuenta) cuentaCombo.getSelectedItem();
		modelo.clear();
		if (cuenta == null) return;
		try {
			List<Movimiento> movs = movimientoDao.leerPorCuenta(cuenta);
			for (Movimiento m : movs) modelo.addElement(m);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
