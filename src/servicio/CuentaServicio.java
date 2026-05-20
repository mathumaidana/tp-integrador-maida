package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;

public class CuentaServicio {

	private final CuentaDao cuentaDao;
	private final MovimientoDao movimientoDao;

	public CuentaServicio(CuentaDao cuentaDao) {
		this(cuentaDao, new MovimientoDao());
	}

	public CuentaServicio(CuentaDao cuentaDao, MovimientoDao movimientoDao) {
		this.cuentaDao = cuentaDao;
		this.movimientoDao = movimientoDao;
	}

	public void agregar(Cuenta c) throws GrabandoException, CuentaDuplicadaException {
		try {
			if (c.getCbu() != null && !c.getCbu().isEmpty()
					&& cuentaDao.buscarPorCbu(c.getCbu()) != null) {
				throw new CuentaDuplicadaException("Ya existe una cuenta con CBU '" + c.getCbu() + "'");
			}
			if (c.getAlias() != null && !c.getAlias().isEmpty()
					&& cuentaDao.buscarPorAlias(c.getAlias()) != null) {
				throw new CuentaDuplicadaException("Ya existe una cuenta con alias '" + c.getAlias() + "'");
			}
			cuentaDao.grabar(c);
		} catch (SQLException e) {
			throw new GrabandoException("Error al grabar la cuenta: " + e.getMessage());
		}
	}

	public Cuenta leer(Integer id) throws LeyendoException {
		try {
			return cuentaDao.leer(id);
		} catch (SQLException e) {
			throw new LeyendoException("Error al leer la cuenta: " + e.getMessage());
		}
	}

	public Cuenta buscarPorCbu(String cbu) throws LeyendoException {
		try {
			return cuentaDao.buscarPorCbu(cbu);
		} catch (SQLException e) {
			throw new LeyendoException("Error al buscar por CBU: " + e.getMessage());
		}
	}

	public Cuenta buscarPorAlias(String alias) throws LeyendoException {
		try {
			return cuentaDao.buscarPorAlias(alias);
		} catch (SQLException e) {
			throw new LeyendoException("Error al buscar por alias: " + e.getMessage());
		}
	}

	public List<Cuenta> listarPorCliente(Cliente cliente) throws LeyendoException {
		try {
			return cuentaDao.leerPorTitular(cliente.getId());
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar cuentas: " + e.getMessage());
		}
	}

	public List<Cuenta> listar() throws LeyendoException {
		try {
			return cuentaDao.leer();
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar cuentas: " + e.getMessage());
		}
	}

	public void modificar(Cuenta c) throws GrabandoException, CuentaInexistenteException, CuentaDuplicadaException {
		try {
			Cuenta existente = cuentaDao.leer(c.getId());
			if (existente == null) {
				throw new CuentaInexistenteException("No existe una cuenta con id " + c.getId());
			}
			validarCbuAliasUnicosEnEdicion(c);
			cuentaDao.modificar(c);
		} catch (SQLException e) {
			throw new GrabandoException("Error al modificar la cuenta: " + e.getMessage());
		}
	}

	private void validarCbuAliasUnicosEnEdicion(Cuenta c) throws SQLException, CuentaDuplicadaException {
		if (c.getCbu() != null && !c.getCbu().isEmpty()) {
			Cuenta porCbu = cuentaDao.buscarPorCbu(c.getCbu());
			if (porCbu != null && !porCbu.getId().equals(c.getId())) {
				throw new CuentaDuplicadaException("Otra cuenta ya usa el CBU '" + c.getCbu() + "'");
			}
		}
		if (c.getAlias() != null && !c.getAlias().isEmpty()) {
			Cuenta porAlias = cuentaDao.buscarPorAlias(c.getAlias());
			if (porAlias != null && !porAlias.getId().equals(c.getId())) {
				throw new CuentaDuplicadaException("Otra cuenta ya usa el alias '" + c.getAlias() + "'");
			}
		}
	}

	public void borrar(Integer id) throws GrabandoException, CuentaInexistenteException {
		try {
			Cuenta existente = cuentaDao.leer(id);
			if (existente == null) {
				throw new CuentaInexistenteException("No existe una cuenta con id " + id);
			}
			movimientoDao.borrarPorCuenta(id);
			cuentaDao.borrar(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar la cuenta: " + e.getMessage());
		}
	}
}
