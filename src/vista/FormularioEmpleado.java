package vista;

import java.util.List;

import entidades.Administrador;
import entidades.Empleado;
import persistencia.EmpleadoDao;
import servicio.EmpleadoServicio;
import servicio.LeyendoException;

public class FormularioEmpleado extends FormularioUsuario<Empleado> {

	private final EmpleadoServicio empleadoServicio;
	private final Administrador sesion;

	public FormularioEmpleado(Administrador sesion) {
		this.sesion = sesion;
		this.empleadoServicio = new EmpleadoServicio(new EmpleadoDao());
		montarVentana();
	}

	@Override
	protected String tituloVentana() {
		return "Empleados";
	}

	@Override
	protected String tituloDatos() {
		return "Empleado";
	}

	@Override
	protected String tituloLista() {
		return "Lista";
	}

	@Override
	protected String nombreEntidad() {
		return "empleado";
	}

	@Override
	protected Empleado crearEntidadVacia() {
		return new Empleado();
	}

	@Override
	protected void copiarDesdeFormulario(Empleado entidad) {
		copiarCamposComunes(entidad);
	}

	@Override
	protected void cargarEnFormulario(Empleado entidad) {
		cargarCamposComunes(entidad);
	}

	@Override
	protected List<Empleado> listarTodos() throws LeyendoException {
		return empleadoServicio.listar();
	}

	@Override
	protected void guardarNuevo(Empleado entidad) throws Exception {
		empleadoServicio.agregar(entidad);
	}

	@Override
	protected void guardarModificacion(Empleado entidad) throws Exception {
		empleadoServicio.modificar(entidad);
	}

	@Override
	protected void eliminar(Integer id) throws Exception {
		empleadoServicio.borrar(id, sesion);
	}

	@Override
	protected String mensajeAltaOk() {
		return "Empleado guardado.";
	}

	@Override
	protected String mensajeModificacionOk() {
		return "Empleado actualizado.";
	}
}
