package vista;

import java.util.List;

import entidades.Administrador;
import persistencia.AdministradorDao;
import servicio.AdministradorServicio;
import servicio.LeyendoException;

public class FormularioAdministrador extends FormularioUsuario<Administrador> {

	private final AdministradorServicio administradorServicio;
	private final Administrador sesion;

	public FormularioAdministrador(Administrador sesion) {
		this.sesion = sesion;
		this.administradorServicio = new AdministradorServicio(new AdministradorDao());
		montarVentana();
	}

	@Override
	protected String tituloVentana() {
		return "Administradores";
	}

	@Override
	protected String tituloDatos() {
		return "Administrador";
	}

	@Override
	protected String tituloLista() {
		return "Lista";
	}

	@Override
	protected String nombreEntidad() {
		return "administrador";
	}

	@Override
	protected Administrador crearEntidadVacia() {
		return new Administrador();
	}

	@Override
	protected void copiarDesdeFormulario(Administrador entidad) {
		copiarCamposComunes(entidad);
	}

	@Override
	protected void cargarEnFormulario(Administrador entidad) {
		cargarCamposComunes(entidad);
	}

	@Override
	protected List<Administrador> listarTodos() throws LeyendoException {
		return administradorServicio.listar();
	}

	@Override
	protected void guardarNuevo(Administrador entidad) throws Exception {
		administradorServicio.agregar(entidad);
	}

	@Override
	protected void guardarModificacion(Administrador entidad) throws Exception {
		administradorServicio.modificar(entidad);
	}

	@Override
	protected void eliminar(Integer id) throws Exception {
		administradorServicio.borrar(id, sesion);
	}

	@Override
	protected boolean puedeEliminar(Administrador seleccionado) {
		return sesion == null || !sesion.getId().equals(seleccionado.getId());
	}

	@Override
	protected String mensajeEliminacionNoPermitida(Administrador seleccionado) {
		return "No podés borrarte mientras estás logueado.";
	}

	@Override
	protected String mensajeAltaOk() {
		return "Administrador guardado.";
	}

	@Override
	protected String mensajeModificacionOk() {
		return "Administrador actualizado.";
	}
}
