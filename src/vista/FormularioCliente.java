package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import java.util.List;

import entidades.Cliente;
import persistencia.ClienteDao;
import servicio.ClienteExistenteException;
import servicio.ClienteInexistenteException;
import servicio.ClienteServicio;
import servicio.GrabandoException;
import servicio.LeyendoException;

public class FormularioCliente {

	private static final int MARGIN = 12;

	private final JFrame frame;
	private JTextField idField;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField nombreField;
	private JTextField apellidoField;
	private JTextField dniField;
	private JList<Cliente> lista;
	private DefaultListModel<Cliente> modelo;

	private final ClienteServicio clienteServicio;

	public FormularioCliente() {
		this.clienteServicio = new ClienteServicio(new ClienteDao());
		this.frame = new JFrame("Administración de clientes");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
		root.setBorder(new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN));
		root.add(crearPanelFormulario(), BorderLayout.NORTH);
		root.add(crearPanelLista(), BorderLayout.CENTER);
		root.add(crearPanelBotones(), BorderLayout.SOUTH);
		frame.add(root, BorderLayout.CENTER);
		frame.setMinimumSize(new Dimension(560, 440));
		frame.setSize(680, 520);
		frame.setLocationRelativeTo(null);
		refrescarLista();
		frame.setVisible(true);
	}

	private JPanel crearPanelFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder("Datos del cliente"));

		JPanel grid = new JPanel(new GridLayout(6, 2, 8, 6));
		JLabel idLbl = new JLabel("Id");
		idLbl.setToolTipText("Se completa al elegir un cliente; vacío indica alta nueva.");
		grid.add(idLbl);
		idField = new JTextField();
		idField.setEditable(false);
		idField.setToolTipText("Solo lectura. Vacío = nuevo cliente.");
		grid.add(idField);
		grid.add(new JLabel("Usuario"));
		usernameField = new JTextField();
		grid.add(usernameField);
		grid.add(new JLabel("Contraseña"));
		passwordField = new JPasswordField();
		grid.add(passwordField);
		grid.add(new JLabel("Nombre"));
		nombreField = new JTextField();
		grid.add(nombreField);
		grid.add(new JLabel("Apellido"));
		apellidoField = new JTextField();
		grid.add(apellidoField);
		grid.add(new JLabel("DNI"));
		dniField = new JTextField();
		grid.add(dniField);

		JLabel ayuda = new JLabel(
			"<html><body style='width:420px'>Elegí un cliente en la lista para modificarlo o borrarlo. "
				+ "<b>Limpiar formulario</b> quitá la selección y deja los campos listos para dar de alta otro usuario "
				+ "(por ejemplo, si venías editando uno). Después de guardar, el formulario también se limpia solo.</body></html>");

		wrap.add(grid, BorderLayout.NORTH);
		wrap.add(ayuda, BorderLayout.SOUTH);
		return wrap;
	}

	private JPanel crearPanelLista() {
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBorder(BorderFactory.createTitledBorder("Clientes registrados"));

		modelo = new DefaultListModel<>();
		lista = new JList<>(modelo);
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setVisibleRowCount(8);
		lista.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
				cargarFormulario(lista.getSelectedValue());
			}
		});

		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(400, 220));
		wrap.add(scroll, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelBotones() {
		JPanel bar = new JPanel(new BorderLayout(0, 8));

		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton limpiar = new JButton("Limpiar formulario");
		limpiar.setToolTipText("Deselecciona la lista y vacía los campos para cargar un cliente nuevo.");
		limpiar.addActionListener(e -> limpiarFormulario());
		JButton grabar = new JButton("Guardar");
		grabar.setToolTipText("Si no hay Id, crea un cliente; si hay Id, actualiza.");
		grabar.addActionListener(e -> grabarOModificar());
		JButton borrar = new JButton("Eliminar");
		borrar.addActionListener(e -> borrarSeleccionado());
		JButton refrescar = new JButton("Actualizar lista");
		refrescar.setToolTipText("Vuelve a leer los clientes desde la base de datos.");
		refrescar.addActionListener(e -> refrescarLista());
		acciones.add(limpiar);
		acciones.add(grabar);
		acciones.add(borrar);
		acciones.add(refrescar);

		JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		JButton volver = new JButton("Volver al menú administrador");
		volver.addActionListener(e -> frame.dispose());
		nav.add(volver);

		bar.add(acciones, BorderLayout.CENTER);
		bar.add(nav, BorderLayout.SOUTH);
		return bar;
	}

	private void cargarFormulario(Cliente c) {
		idField.setText(String.valueOf(c.getId()));
		usernameField.setText(c.getUsername());
		passwordField.setText(c.getPassword());
		nombreField.setText(c.getNombre());
		apellidoField.setText(c.getApellido());
		dniField.setText(c.getDni());
	}

	private void limpiarFormulario() {
		lista.clearSelection();
		idField.setText("");
		usernameField.setText("");
		passwordField.setText("");
		nombreField.setText("");
		apellidoField.setText("");
		dniField.setText("");
	}

	private void validarCampos() throws CampoVacioException {
		if (usernameField.getText().trim().isEmpty()) throw new CampoVacioException("Usuario");
		if (new String(passwordField.getPassword()).isEmpty()) throw new CampoVacioException("Contraseña");
		if (nombreField.getText().trim().isEmpty()) throw new CampoVacioException("Nombre");
		if (apellidoField.getText().trim().isEmpty()) throw new CampoVacioException("Apellido");
		if (dniField.getText().trim().isEmpty()) throw new CampoVacioException("DNI");
	}

	private void grabarOModificar() {
		try {
			validarCampos();
			Cliente c = new Cliente();
			c.setUsername(usernameField.getText().trim());
			c.setPassword(new String(passwordField.getPassword()));
			c.setNombre(nombreField.getText().trim());
			c.setApellido(apellidoField.getText().trim());
			c.setDni(dniField.getText().trim());

			if (idField.getText().trim().isEmpty()) {
				clienteServicio.agregar(c);
				JOptionPane.showMessageDialog(frame, "Cliente creado correctamente", "OK", JOptionPane.INFORMATION_MESSAGE);
			} else {
				c.setId(Integer.valueOf(idField.getText()));
				clienteServicio.modificar(c);
				JOptionPane.showMessageDialog(frame, "Cliente modificado correctamente", "OK", JOptionPane.INFORMATION_MESSAGE);
			}
			limpiarFormulario();
			refrescarLista();
		} catch (CampoVacioException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
		} catch (ClienteExistenteException | ClienteInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void borrarSeleccionado() {
		Cliente seleccionado = lista.getSelectedValue();
		if (seleccionado == null) {
			JOptionPane.showMessageDialog(frame, "Seleccioná un cliente de la lista", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(frame,
			"¿Borrar el cliente '" + seleccionado.getUsername() + "'?",
			"Confirmar", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		try {
			clienteServicio.borrar(seleccionado.getId());
			limpiarFormulario();
			refrescarLista();
		} catch (ClienteInexistenteException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void refrescarLista() {
		try {
			List<Cliente> clientes = clienteServicio.listar();
			modelo.clear();
			for (Cliente c : clientes) {
				modelo.addElement(c);
			}
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public JFrame getFrame() {
		return frame;
	}
}
