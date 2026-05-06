package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import persistencia.CuentaDao;

public class CuentaServicio {

	private final CuentaDao cuentaDao;

	public CuentaServicio(CuentaDao cuentaDao) {
		this.cuentaDao = cuentaDao;
	}

	public void agregar(Cuenta c) throws GrabandoException {
		try {
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
}
