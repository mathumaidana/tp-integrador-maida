package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import persistencia.ClienteDao;
import persistencia.UsuarioDao;

public class ClienteServicio {

	private final ClienteDao clienteDao;
	private final UsuarioDao usuarioDao;

	public ClienteServicio(ClienteDao clienteDao) {
		this(clienteDao, new UsuarioDao());
	}

	public ClienteServicio(ClienteDao clienteDao, UsuarioDao usuarioDao) {
		this.clienteDao = clienteDao;
		this.usuarioDao = usuarioDao;
	}

	public void agregar(Cliente c) throws GrabandoException, ClienteExistenteException {
		try {
			if (usuarioDao.buscarPorUsername(c.getUsername()) != null) {
				throw new ClienteExistenteException("Ya existe un usuario con username '" + c.getUsername() + "'");
			}
			clienteDao.grabar(c);
		} catch (SQLException e) {
			throw new GrabandoException("Error al grabar el cliente: " + e.getMessage());
		}
	}

	public Cliente leer(Integer id) throws LeyendoException, ClienteInexistenteException {
		try {
			Cliente c = clienteDao.leer(id);
			if (c == null) {
				throw new ClienteInexistenteException("No existe un cliente con id " + id);
			}
			return c;
		} catch (SQLException e) {
			throw new LeyendoException("Error al leer el cliente: " + e.getMessage());
		}
	}

	public List<Cliente> listar() throws LeyendoException {
		try {
			return clienteDao.leer();
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar clientes: " + e.getMessage());
		}
	}

	public void modificar(Cliente c) throws GrabandoException, ClienteInexistenteException, ClienteExistenteException {
		try {
			Cliente existente = clienteDao.leer(c.getId());
			if (existente == null) {
				throw new ClienteInexistenteException("No existe un cliente con id " + c.getId());
			}
			entidades.Usuario otro = usuarioDao.buscarPorUsername(c.getUsername());
			if (otro != null && !otro.getId().equals(c.getId())) {
				throw new ClienteExistenteException(
					"Ya existe otro usuario con username '" + c.getUsername() + "'");
			}
			clienteDao.modificar(c);
		} catch (SQLException e) {
			throw new GrabandoException("Error al modificar el cliente: " + e.getMessage());
		}
	}

	public void borrar(Integer id) throws GrabandoException, ClienteInexistenteException {
		try {
			Cliente existente = clienteDao.leer(id);
			if (existente == null) {
				throw new ClienteInexistenteException("No existe un cliente con id " + id);
			}
			clienteDao.borrarEnCascada(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar el cliente: " + e.getMessage());
		}
	}
}
