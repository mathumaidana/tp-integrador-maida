package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Administrador;
import persistencia.AdministradorDao;
import persistencia.UsuarioDao;

public class AdministradorServicio {

	private final AdministradorDao administradorDao;
	private final UsuarioDao usuarioDao;

	public AdministradorServicio(AdministradorDao administradorDao) {
		this(administradorDao, new UsuarioDao());
	}

	public AdministradorServicio(AdministradorDao administradorDao, UsuarioDao usuarioDao) {
		this.administradorDao = administradorDao;
		this.usuarioDao = usuarioDao;
	}

	public void agregar(Administrador a) throws GrabandoException, AdministradorExistenteException {
		try {
			if (usuarioDao.buscarPorUsername(a.getUsername()) != null) {
				throw new AdministradorExistenteException(
					"Ya existe un usuario con username '" + a.getUsername() + "'");
			}
			administradorDao.grabar(a);
		} catch (SQLException e) {
			throw new GrabandoException("Error al grabar el administrador: " + e.getMessage());
		}
	}

	public Administrador leer(Integer id) throws LeyendoException, AdministradorInexistenteException {
		try {
			Administrador a = administradorDao.leer(id);
			if (a == null) {
				throw new AdministradorInexistenteException("No existe un administrador con id " + id);
			}
			return a;
		} catch (SQLException e) {
			throw new LeyendoException("Error al leer el administrador: " + e.getMessage());
		}
	}

	public List<Administrador> listar() throws LeyendoException {
		try {
			return administradorDao.leer();
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar administradores: " + e.getMessage());
		}
	}

	public void modificar(Administrador a) throws GrabandoException, AdministradorInexistenteException {
		try {
			Administrador existente = administradorDao.leer(a.getId());
			if (existente == null) {
				throw new AdministradorInexistenteException("No existe un administrador con id " + a.getId());
			}
			administradorDao.modificar(a);
		} catch (SQLException e) {
			throw new GrabandoException("Error al modificar el administrador: " + e.getMessage());
		}
	}

	public void borrar(Integer id, Administrador sesion)
			throws GrabandoException, AdministradorInexistenteException, OperacionNoPermitidaException {
		try {
			Administrador existente = administradorDao.leer(id);
			if (existente == null) {
				throw new AdministradorInexistenteException("No existe un administrador con id " + id);
			}
			if (sesion != null && sesion.getId().equals(id)) {
				throw new OperacionNoPermitidaException(
					"No podés borrarte mientras estás logueado.");
			}
			if (administradorDao.leer().size() <= 1) {
				throw new OperacionNoPermitidaException(
					"Tiene que quedar al menos un administrador.");
			}
			administradorDao.borrar(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar el administrador: " + e.getMessage());
		}
	}
}
