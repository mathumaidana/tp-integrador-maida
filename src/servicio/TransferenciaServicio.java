package servicio;

import java.sql.SQLException;
import java.time.LocalDateTime;

import entidades.Cuenta;
import entidades.Movimiento;
import entidades.TipoMovimiento;
import persistencia.CuentaDao;
import persistencia.MovimientoDao;

public class TransferenciaServicio {

	private final CuentaDao cuentaDao;
	private final MovimientoDao movimientoDao;

	public TransferenciaServicio(CuentaDao cuentaDao, MovimientoDao movimientoDao) {
		this.cuentaDao = cuentaDao;
		this.movimientoDao = movimientoDao;
	}

	public void transferir(Cuenta origen, Cuenta destino, Double monto, String descripcion)
			throws SaldoInsuficienteException, GrabandoException {
		if (origen == null || destino == null) {
			throw new GrabandoException("Origen y destino son obligatorios");
		}
		if (origen.getId().equals(destino.getId())) {
			throw new GrabandoException("La cuenta origen y destino no pueden ser la misma");
		}
		if (monto == null || monto <= 0) {
			throw new GrabandoException("El monto tiene que ser mayor a cero");
		}
		if (origen.getSaldo() < monto) {
			throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta origen");
		}

		Double saldoOrigenPrevio = origen.getSaldo();
		Double saldoDestinoPrevio = destino.getSaldo();

		try {
			origen.setSaldo(saldoOrigenPrevio - monto);
			destino.setSaldo(saldoDestinoPrevio + monto);
			cuentaDao.actualizarSaldo(origen.getId(), origen.getSaldo());
			cuentaDao.actualizarSaldo(destino.getId(), destino.getSaldo());

			Movimiento envio = new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.TRANSFERENCIA_ENVIADA,
				"A " + referenciaCuenta(destino) + (descripcion != null && !descripcion.isEmpty() ? ": " + descripcion : ""),
				origen);
			Movimiento recibo = new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.TRANSFERENCIA_RECIBIDA,
				"De " + referenciaCuenta(origen) + (descripcion != null && !descripcion.isEmpty() ? ": " + descripcion : ""),
				destino);
			movimientoDao.grabar(envio);
			movimientoDao.grabar(recibo);
		} catch (SQLException e) {
			compensar(origen, saldoOrigenPrevio, destino, saldoDestinoPrevio);
			throw new GrabandoException("Error al realizar la transferencia: " + e.getMessage());
		}
	}

	private void compensar(Cuenta origen, Double saldoOrigen, Cuenta destino, Double saldoDestino) {
		try {
			cuentaDao.actualizarSaldo(origen.getId(), saldoOrigen);
			cuentaDao.actualizarSaldo(destino.getId(), saldoDestino);
			origen.setSaldo(saldoOrigen);
			destino.setSaldo(saldoDestino);
		} catch (SQLException ignored) {
		}
	}

	private String referenciaCuenta(Cuenta c) {
		if (c.getAlias() != null && !c.getAlias().isEmpty()) return c.getAlias();
		if (c.getCbu() != null && !c.getCbu().isEmpty()) return c.getCbu();
		return String.valueOf(c.getId());
	}
}
