package vista;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import entidades.Cliente;
import entidades.Tarjeta;
import persistencia.TarjetaDao;
import servicio.LeyendoException;
import servicio.TarjetaServicio;

public class FormularioTarjeta {

	private final JFrame frame;
	private final Cliente cliente;
	private final TarjetaServicio tarjetaServicio;
	private DefaultListModel<Tarjeta> modelo;

	public FormularioTarjeta() {
		this(null);
	}

	public FormularioTarjeta(Cliente cliente) {
		this.cliente = cliente;
		this.tarjetaServicio = new TarjetaServicio(new TarjetaDao());
		this.frame = new JFrame("Tarjetas" + (cliente != null ? " - " + cliente.getUsername() : ""));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(crearListado(), BorderLayout.CENTER);
		frame.add(crearBotonera(), BorderLayout.SOUTH);
		frame.setSize(520, 320);
		frame.setLocationRelativeTo(null);
		refrescar();
		frame.setVisible(true);
	}

	private JScrollPane crearListado() {
		modelo = new DefaultListModel<>();
		JList<Tarjeta> lista = new JList<>(modelo);
		return new JScrollPane(lista);
	}

	private JPanel crearBotonera() {
		JPanel panel = new JPanel();
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> refrescar());
		JButton info = new JButton("Operaciones avanzadas");
		info.addActionListener(e -> JOptionPane.showMessageDialog(frame,
			"Alta y débitos de tarjeta se completan en la entrega final",
			"Pendiente", JOptionPane.INFORMATION_MESSAGE));
		JButton cerrar = new JButton("Volver");
		cerrar.addActionListener(e -> frame.dispose());
		panel.add(refrescar);
		panel.add(info);
		panel.add(cerrar);
		return panel;
	}

	private void refrescar() {
		try {
			List<Tarjeta> tarjetas = (cliente == null)
				? tarjetaServicio.listar()
				: tarjetaServicio.listarPorCliente(cliente);
			modelo.clear();
			for (Tarjeta t : tarjetas) modelo.addElement(t);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
