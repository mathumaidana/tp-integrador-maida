package vista;

import java.util.List;

import entidades.Cliente;
import persistencia.ClienteDao;
import servicio.ClienteServicio;
import servicio.LeyendoException;

public class FormularioCliente extends FormularioUsuario<Cliente> {

	private final ClienteServicio clienteServicio;

	public FormularioCliente() {
		this.clienteServicio = new ClienteServicio(new ClienteDao());
		montarVentana();
	}

	@Override
	protected String tituloVentana() {
		return "Clientes";
	}

	@Override
	protected String tituloDatos() {
		return "Cliente";
	}

	@Override
	protected String tituloLista() {
		return "Lista";
	}

	@Override
	protected String nombreEntidad() {
		return "cliente";
	}

	@Override
	protected Cliente crearEntidadVacia() {
		return new Cliente();
	}

	@Override
	protected void copiarDesdeFormulario(Cliente entidad) {
		copiarCamposComunes(entidad);
	}

	@Override
	protected void cargarEnFormulario(Cliente entidad) {
		cargarCamposComunes(entidad);
	}

	@Override
	protected List<Cliente> listarTodos() throws LeyendoException {
		return clienteServicio.listar();
	}

	@Override
	protected void guardarNuevo(Cliente entidad) throws Exception {
		clienteServicio.agregar(entidad);
	}

	@Override
	protected void guardarModificacion(Cliente entidad) throws Exception {
		clienteServicio.modificar(entidad);
	}

	@Override
	protected void eliminar(Integer id) throws Exception {
		clienteServicio.borrar(id);
	}

	@Override
	protected String mensajeAltaOk() {
		return "Cliente guardado.";
	}

	@Override
	protected String mensajeModificacionOk() {
		return "Cliente actualizado.";
	}
}
