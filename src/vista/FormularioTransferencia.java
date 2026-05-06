package vista;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

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

	private final JFrame frame;
	private final Cliente cliente;
	private JComboBox<Cuenta> origenCombo;
	private JTextField destinoField;
	private JTextField montoField;
	private JTextField descripcionField;

	private final CuentaServicio cuentaServicio;
	private final TransferenciaServicio transferenciaServicio;
	private final CuentaDao cuentaDao;

	public FormularioTransferencia(Cliente cliente) {
		this.cliente = cliente;
		this.cuentaDao = new CuentaDao();
		this.cuentaServicio = new CuentaServicio(cuentaDao);
		this.transferenciaServicio = new TransferenciaServicio(cuentaDao, new MovimientoDao());
		this.frame = new JFrame("Transferencia");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(crearFormulario(), BorderLayout.CENTER);
		frame.add(crearBotonera(), BorderLayout.SOUTH);
		frame.setSize(420, 240);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private JPanel crearFormulario() {
		JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
		panel.add(new JLabel("Cuenta origen:"));
		origenCombo = new JComboBox<>();
		try {
			List<Cuenta> mias = cuentaServicio.listarPorCliente(cliente);
			for (Cuenta c : mias) origenCombo.addItem(c);
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		panel.add(origenCombo);
		panel.add(new JLabel("Id cuenta destino:"));
		destinoField = new JTextField();
		panel.add(destinoField);
		panel.add(new JLabel("Monto:"));
		montoField = new JTextField();
		panel.add(montoField);
		panel.add(new JLabel("Descripción:"));
		descripcionField = new JTextField();
		panel.add(descripcionField);
		return panel;
	}

	private JPanel crearBotonera() {
		JPanel panel = new JPanel();
		JButton transferir = new JButton("Transferir");
		transferir.addActionListener(e -> ejecutar());
		JButton cancelar = new JButton("Volver");
		cancelar.addActionListener(e -> frame.dispose());
		panel.add(transferir);
		panel.add(cancelar);
		return panel;
	}

	private void ejecutar() {
		try {
			if (destinoField.getText().trim().isEmpty()) throw new CampoVacioException("Id cuenta destino");
			if (montoField.getText().trim().isEmpty()) throw new CampoVacioException("Monto");
			Cuenta origen = (Cuenta) origenCombo.getSelectedItem();
			if (origen == null) {
				JOptionPane.showMessageDialog(frame, "Seleccioná una cuenta origen", "Validación", JOptionPane.WARNING_MESSAGE);
				return;
			}
			Cuenta destino = cuentaServicio.leer(Integer.valueOf(destinoField.getText().trim()));
			if (destino == null) {
				JOptionPane.showMessageDialog(frame, "No existe la cuenta destino", "Validación", JOptionPane.WARNING_MESSAGE);
				return;
			}
			Double monto = Double.valueOf(montoField.getText().trim());
			transferenciaServicio.transferir(origen, destino, monto, descripcionField.getText());
			JOptionPane.showMessageDialog(frame, "Transferencia realizada", "OK", JOptionPane.INFORMATION_MESSAGE);
			frame.dispose();
		} catch (CampoVacioException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Monto o id inválido", "Validación", JOptionPane.WARNING_MESSAGE);
		} catch (SaldoInsuficienteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saldo", JOptionPane.WARNING_MESSAGE);
		} catch (LeyendoException | GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
