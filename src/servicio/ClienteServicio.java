package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.Tarjeta;
import persistencia.ClienteDao;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;
import persistencia.TarjetaDao;
import persistencia.UsuarioDao;

public class ClienteServicio {

	private final ClienteDao clienteDao;
	private final CuentaDao cuentaDao;
	private final MovimientoDao movimientoDao;
	private final TarjetaDao tarjetaDao;
	private final UsuarioDao usuarioDao;

	public ClienteServicio(ClienteDao clienteDao) {
		this(clienteDao, new CuentaDao(), new MovimientoDao(), new TarjetaDao(), new UsuarioDao());
	}

	public ClienteServicio(ClienteDao clienteDao, CuentaDao cuentaDao,
			MovimientoDao movimientoDao, TarjetaDao tarjetaDao, UsuarioDao usuarioDao) {
		this.clienteDao = clienteDao;
		this.cuentaDao = cuentaDao;
		this.movimientoDao = movimientoDao;
		this.tarjetaDao = tarjetaDao;
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

	public void modificar(Cliente c) throws GrabandoException, ClienteInexistenteException {
		try {
			Cliente existente = clienteDao.leer(c.getId());
			if (existente == null) {
				throw new ClienteInexistenteException("No existe un cliente con id " + c.getId());
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
			for (Cuenta c : cuentaDao.leerPorTitular(id)) {
				movimientoDao.borrarPorCuenta(c.getId());
				cuentaDao.borrar(c.getId());
			}
			for (Tarjeta t : tarjetaDao.leerPorTitular(id)) {
				movimientoDao.borrarPorTarjeta(t.getId());
				tarjetaDao.borrar(t.getId());
			}
			clienteDao.borrar(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar el cliente: " + e.getMessage());
		}
	}
}
