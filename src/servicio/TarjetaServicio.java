package servicio;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import entidades.Cliente;
import entidades.Movimiento;
import entidades.Tarjeta;
import entidades.TipoMovimiento;
import persistencia.MovimientoDao;
import persistencia.TarjetaDao;

public class TarjetaServicio {

	private final TarjetaDao tarjetaDao;
	private final MovimientoDao movimientoDao;

	public TarjetaServicio(TarjetaDao tarjetaDao) {
		this(tarjetaDao, new MovimientoDao());
	}

	public TarjetaServicio(TarjetaDao tarjetaDao, MovimientoDao movimientoDao) {
		this.tarjetaDao = tarjetaDao;
		this.movimientoDao = movimientoDao;
	}

	public void agregar(Tarjeta t) throws GrabandoException, TarjetaDuplicadaException {
		try {
			if (existeNumero(t.getNumero())) {
				throw new TarjetaDuplicadaException("Ya existe una tarjeta con número '" + t.getNumero() + "'");
			}
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

	public void modificar(Tarjeta t) throws GrabandoException, TarjetaInexistenteException {
		try {
			Tarjeta existente = tarjetaDao.leer(t.getId());
			if (existente == null) {
				throw new TarjetaInexistenteException("No existe una tarjeta con id " + t.getId());
			}
			tarjetaDao.modificar(t);
		} catch (SQLException e) {
			throw new GrabandoException("Error al modificar la tarjeta: " + e.getMessage());
		}
	}

	public void borrar(Integer id) throws GrabandoException, TarjetaInexistenteException {
		try {
			Tarjeta existente = tarjetaDao.leer(id);
			if (existente == null) {
				throw new TarjetaInexistenteException("No existe una tarjeta con id " + id);
			}
			movimientoDao.borrarPorTarjeta(id);
			tarjetaDao.borrar(id);
		} catch (SQLException e) {
			throw new GrabandoException("Error al borrar la tarjeta: " + e.getMessage());
		}
	}

	public void debitar(Tarjeta t, Double monto, String descripcion)
			throws GrabandoException, SaldoInsuficienteException {
		if (monto == null || monto <= 0) {
			throw new GrabandoException("El monto tiene que ser mayor a cero");
		}
		if (t.getDisponible() < monto) {
			throw new SaldoInsuficienteException("Disponible insuficiente en la tarjeta");
		}
		try {
			Double nuevoDisp = t.getDisponible() - monto;
			Double nuevoSaldo = t.getSaldoAPagar() + monto;
			t.setDisponible(nuevoDisp);
			t.setSaldoAPagar(nuevoSaldo);
			tarjetaDao.modificar(t);
			movimientoDao.grabar(new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.DEBITO_TARJETA, descripcion, null, t));
		} catch (SQLException e) {
			throw new GrabandoException("Error al registrar el débito de tarjeta: " + e.getMessage());
		}
	}

	public void pagar(Tarjeta t, Double monto, String descripcion) throws GrabandoException {
		if (monto == null || monto <= 0) {
			throw new GrabandoException("El monto tiene que ser mayor a cero");
		}
		try {
			Double nuevoDisp = t.getDisponible() + monto;
			Double nuevoSaldo = Math.max(0, t.getSaldoAPagar() - monto);
			t.setDisponible(nuevoDisp);
			t.setSaldoAPagar(nuevoSaldo);
			tarjetaDao.modificar(t);
			movimientoDao.grabar(new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.PAGO_TARJETA, descripcion, null, t));
		} catch (SQLException e) {
			throw new GrabandoException("Error al registrar el pago de tarjeta: " + e.getMessage());
		}
	}

	public List<Movimiento> resumenMensual(Tarjeta t, YearMonth mes) throws LeyendoException {
		try {
			return movimientoDao.leerPorTarjetaYMes(t, mes);
		} catch (SQLException e) {
			throw new LeyendoException("Error al obtener el resumen de tarjeta: " + e.getMessage());
		}
	}

	private boolean existeNumero(String numero) throws SQLException {
		if (numero == null) return false;
		for (Tarjeta existente : tarjetaDao.leer()) {
			if (numero.equals(existente.getNumero())) return true;
		}
		return false;
	}
}
