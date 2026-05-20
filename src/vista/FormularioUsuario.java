package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

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

import entidades.Usuario;
import servicio.GrabandoException;
import servicio.LeyendoException;

public abstract class FormularioUsuario<T extends Usuario> {

	protected static final int MARGIN = 12;

	protected JFrame frame;
	protected JTextField idField;
	protected JTextField usernameField;
	protected JPasswordField passwordField;
	protected JTextField nombreField;
	protected JTextField apellidoField;
	protected JTextField dniField;
	protected JList<T> lista;
	protected DefaultListModel<T> modelo;

	protected FormularioUsuario() {
	}

	protected final void montarVentana() {
		this.frame = new JFrame(tituloVentana());
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

	protected abstract String tituloVentana();

	protected abstract String tituloDatos();

	protected abstract String tituloLista();

	protected abstract String nombreEntidad();

	protected abstract T crearEntidadVacia();

	protected abstract void copiarDesdeFormulario(T entidad);

	protected abstract void cargarEnFormulario(T entidad);

	protected abstract List<T> listarTodos() throws LeyendoException;

	protected abstract void guardarNuevo(T entidad) throws Exception;

	protected abstract void guardarModificacion(T entidad) throws Exception;

	protected abstract void eliminar(Integer id) throws Exception;

	protected boolean puedeEliminar(T seleccionado) {
		return true;
	}

	protected String mensajeEliminacionNoPermitida(T seleccionado) {
		return "No se puede borrar.";
	}

	protected abstract String mensajeAltaOk();

	protected abstract String mensajeModificacionOk();

	private JPanel crearPanelFormulario() {
		JPanel wrap = new JPanel(new BorderLayout(0, 6));
		wrap.setBorder(BorderFactory.createTitledBorder(tituloDatos()));

		JPanel grid = new JPanel(new GridLayout(6, 2, 8, 6));
		grid.add(new JLabel("Id"));
		idField = new JTextField();
		idField.setEditable(false);
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

		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	private JPanel crearPanelLista() {
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBorder(BorderFactory.createTitledBorder(tituloLista()));

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
		scroll.setPreferredSize(new Dimension(400, 220));
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
		borrar.addActionListener(e -> borrarSeleccionado());
		JButton refrescar = new JButton("Refrescar");
		refrescar.addActionListener(e -> refrescarLista());
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

	protected void limpiarFormulario() {
		lista.clearSelection();
		idField.setText("");
		usernameField.setText("");
		passwordField.setText("");
		nombreField.setText("");
		apellidoField.setText("");
		dniField.setText("");
	}

	protected void validarCampos() throws CampoVacioException {
		if (usernameField.getText().trim().isEmpty()) throw new CampoVacioException("Usuario");
		if (new String(passwordField.getPassword()).isEmpty()) throw new CampoVacioException("Contraseña");
		if (nombreField.getText().trim().isEmpty()) throw new CampoVacioException("Nombre");
		if (apellidoField.getText().trim().isEmpty()) throw new CampoVacioException("Apellido");
		if (dniField.getText().trim().isEmpty()) throw new CampoVacioException("DNI");
	}

	private void grabarOModificar() {
		try {
			validarCampos();
			T entidad = crearEntidadVacia();
			copiarDesdeFormulario(entidad);

			if (idField.getText().trim().isEmpty()) {
				guardarNuevo(entidad);
				JOptionPane.showMessageDialog(frame, mensajeAltaOk(), "Aviso", JOptionPane.INFORMATION_MESSAGE);
			} else {
				entidad.setId(Integer.valueOf(idField.getText()));
				guardarModificacion(entidad);
				JOptionPane.showMessageDialog(frame, mensajeModificacionOk(), "Aviso", JOptionPane.INFORMATION_MESSAGE);
			}
			limpiarFormulario();
			refrescarLista();
		} catch (CampoVacioException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void borrarSeleccionado() {
		T seleccionado = lista.getSelectedValue();
		if (seleccionado == null) {
			JOptionPane.showMessageDialog(frame,
				"Seleccioná un " + nombreEntidad() + " de la lista", "Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!puedeEliminar(seleccionado)) {
			JOptionPane.showMessageDialog(frame, mensajeEliminacionNoPermitida(seleccionado),
				"Aviso", JOptionPane.WARNING_MESSAGE);
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(frame,
			"¿Borrar " + nombreEntidad() + " '" + seleccionado.getUsername() + "'?",
			"Confirmar", JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) return;
		try {
			eliminar(seleccionado.getId());
			limpiarFormulario();
			refrescarLista();
		} catch (GrabandoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void refrescarLista() {
		try {
			List<T> registros = listarTodos();
			modelo.clear();
			for (T registro : registros) {
				modelo.addElement(registro);
			}
		} catch (LeyendoException ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void copiarCamposComunes(Usuario entidad) {
		entidad.setUsername(usernameField.getText().trim());
		entidad.setPassword(new String(passwordField.getPassword()));
		entidad.setNombre(nombreField.getText().trim());
		entidad.setApellido(apellidoField.getText().trim());
		entidad.setDni(dniField.getText().trim());
	}

	protected void cargarCamposComunes(Usuario entidad) {
		idField.setText(String.valueOf(entidad.getId()));
		usernameField.setText(entidad.getUsername());
		passwordField.setText(entidad.getPassword());
		nombreField.setText(entidad.getNombre());
		apellidoField.setText(entidad.getApellido());
		dniField.setText(entidad.getDni());
	}
}
