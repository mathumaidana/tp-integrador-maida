package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import entidades.Cliente;
import entidades.Cuenta;
import persistencia.CuentaDao;
import servicio.CuentaServicio;
import servicio.LeyendoException;

public class MisCuentasView {

	private static final int MARGIN = 12;

	private final JFrame frame;
	private final Cliente cliente;
	private final CuentaServicio cuentaServicio;
	private DefaultListModel<Cuenta> modelo;

	public MisCuentasView(Cliente cliente) {
		this.cliente = cliente;
		this.cuentaServicio = new CuentaServicio(new CuentaDao());
		this.frame = new JFrame("Mis cuentas - " + cliente.getNombre());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearListado(), BorderLayout.CENTER);
		root.add(crearBotonera(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setSize(560, 360);
		frame.setLocationRelativeTo(null);
		refrescar();
		frame.setVisible(true);
	}

	private JScrollPane crearListado() {
		modelo = new DefaultListModel<>();
		JList<Cuenta> lista = new JList<>(modelo);
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setBorder(BorderFactory.createTitledBorder("Cuentas y saldos"));
		scroll.setPreferredSize(new Dimension(520, 260));
		return scroll;
	}

	private JPanel crearBotonera() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> refrescar());
		JButton volver = new JButton("Volver");
		volver.addActionListener(e -> frame.dispose());
		bar.add(refrescar);
		bar.add(volver);
		return bar;
	}

	private void refrescar() {
		try {
			List<Cuenta> cuentas = cuentaServicio.listarPorCliente(cliente);
			modelo.clear();
			for (Cuenta c : cuentas) modelo.addElement(c);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
