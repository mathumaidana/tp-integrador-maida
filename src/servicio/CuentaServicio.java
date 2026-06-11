package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.Movimiento;
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

	public void agregar(Cuenta c)
			throws GrabandoException, CuentaDuplicadaException, SaldoInicialInvalidoException {
		try {
			validarReferencia(c);
			validarSaldoInicial(c);
			chequearUnicidad(c);
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

	public void modificar(Cuenta c) throws GrabandoException, CuentaInexistenteException,
			CuentaDuplicadaException, OperacionNoPermitidaException {
		try {
			Cuenta existente = cuentaDao.leer(c.getId());
			if (existente == null) {
				throw new CuentaInexistenteException("No existe una cuenta con id " + c.getId());
			}
			if (existente.getTipo() != c.getTipo()) {
				throw new OperacionNoPermitidaException(
					"No se puede cambiar el tipo de una cuenta existente. Cerrala y abrí una nueva.");
			}
			validarReferencia(c);
			chequearUnicidad(c);
			cuentaDao.modificar(c);
		} catch (SQLException e) {
			throw new GrabandoException("Error al modificar la cuenta: " + e.getMessage());
		}
	}

	public List<Movimiento> movimientos(Cuenta c) throws LeyendoException {
		try {
			return movimientoDao.leerPorCuenta(c);
		} catch (SQLException e) {
			throw new LeyendoException("Error al leer los movimientos: " + e.getMessage());
		}
	}

	public void borrar(Integer id) throws GrabandoException, CuentaInexistenteException {
		try {
			if (cuentaDao.leer(id) == null) {
				throw new CuentaInexistenteException("No existe una cuenta con id " + id);
			}
			cuentaDao.borrarEnCascada(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar la cuenta: " + e.getMessage());
		}
	}

	private void validarSaldoInicial(Cuenta c) throws SaldoInicialInvalidoException {
		Double saldo = c.getSaldo();
		if (saldo == null || saldo < c.saldoMinimo()) {
			throw new SaldoInicialInvalidoException(
				"El saldo debe ser mayor o igual a " + c.saldoMinimo() + " para una " + c.getTipo() + ".");
		}
	}

	private void validarReferencia(Cuenta c) throws CuentaDuplicadaException {
		boolean sinAlias = c.getAlias() == null || c.getAlias().trim().isEmpty();
		boolean sinCbu = c.getCbu() == null || c.getCbu().trim().isEmpty();
		if (sinAlias && sinCbu) {
			throw new CuentaDuplicadaException("Cargá al menos un alias o un CBU para identificar la cuenta.");
		}
	}

	private void chequearUnicidad(Cuenta c) throws SQLException, CuentaDuplicadaException {
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
}
