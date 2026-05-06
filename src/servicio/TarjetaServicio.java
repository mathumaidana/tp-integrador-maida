package servicio;

import java.sql.SQLException;
import java.util.List;

import entidades.Cliente;
import entidades.Tarjeta;
import persistencia.TarjetaDao;

public class TarjetaServicio {

	private final TarjetaDao tarjetaDao;

	public TarjetaServicio(TarjetaDao tarjetaDao) {
		this.tarjetaDao = tarjetaDao;
	}

	public void agregar(Tarjeta t) throws GrabandoException {
		try {
			tarjetaDao.grabar(t);
		} catch (SQLException e) {
			throw new GrabandoException("Error al grabar la tarjeta: " + e.getMessage());
		}
	}

	public List<Tarjeta> listarPorCliente(Cliente cliente) throws LeyendoException {
		try {
			return tarjetaDao.leerPorTitular(cliente.getId());
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar tarjetas: " + e.getMessage());
		}
	}

	public List<Tarjeta> listar() throws LeyendoException {
		try {
			return tarjetaDao.leer();
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar tarjetas: " + e.getMessage());
		}
	}
}
