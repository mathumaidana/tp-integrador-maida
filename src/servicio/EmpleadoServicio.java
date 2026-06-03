package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Empleado;
import persistencia.EmpleadoDao;
import persistencia.UsuarioDao;

public class EmpleadoServicio {

	private final EmpleadoDao empleadoDao;
	private final UsuarioDao usuarioDao;

	public EmpleadoServicio(EmpleadoDao empleadoDao) {
		this(empleadoDao, new UsuarioDao());
	}

	public EmpleadoServicio(EmpleadoDao empleadoDao, UsuarioDao usuarioDao) {
		this.empleadoDao = empleadoDao;
		this.usuarioDao = usuarioDao;
	}

	public void agregar(Empleado e) throws GrabandoException, EmpleadoExistenteException {
		try {
			if (usuarioDao.buscarPorUsername(e.getUsername()) != null) {
				throw new EmpleadoExistenteException(
					"Ya existe un usuario con username '" + e.getUsername() + "'");
			}
			empleadoDao.grabar(e);
		} catch (SQLException ex) {
			throw new GrabandoException("Error al grabar el empleado: " + ex.getMessage());
		}
	}

	public List<Empleado> listar() throws LeyendoException {
		try {
			return empleadoDao.leer();
		} catch (SQLException ex) {
			throw new LeyendoException("Error al listar empleados: " + ex.getMessage());
		}
	}

	public void modificar(Empleado e) throws GrabandoException, EmpleadoInexistenteException, EmpleadoExistenteException {
		try {
			if (empleadoDao.leer(e.getId()) == null) {
				throw new EmpleadoInexistenteException("No existe un empleado con id " + e.getId());
			}
			entidades.Usuario otro = usuarioDao.buscarPorUsername(e.getUsername());
			if (otro != null && !otro.getId().equals(e.getId())) {
				throw new EmpleadoExistenteException(
					"Ya existe otro usuario con username '" + e.getUsername() + "'");
			}
			empleadoDao.modificar(e);
		} catch (SQLException ex) {
			throw new GrabandoException("Error al modificar el empleado: " + ex.getMessage());
		}
	}

	public void borrar(Integer id, Empleado sesion)
			throws GrabandoException, EmpleadoInexistenteException, OperacionNoPermitidaException {
		try {
			if (empleadoDao.leer(id) == null) {
				throw new EmpleadoInexistenteException("No existe un empleado con id " + id);
			}
			if (sesion != null && sesion.getId().equals(id)) {
				throw new OperacionNoPermitidaException("No podés borrarte mientras estás logueado.");
			}
			empleadoDao.borrar(id);
		} catch (SQLException ex) {
			throw new GrabandoException("Error al borrar el empleado: " + ex.getMessage());
		}
	}
}
