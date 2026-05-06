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
		if (monto == null || monto <= 0) {
			throw new GrabandoException("El monto debe ser mayor a cero");
		}
		if (origen.getSaldo() < monto) {
			throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta origen");
		}
		try {
			origen.setSaldo(origen.getSaldo() - monto);
			destino.setSaldo(destino.getSaldo() + monto);
			cuentaDao.modificar(origen);
			cuentaDao.modificar(destino);

			Movimiento envio = new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.TRANSFERENCIA_ENVIADA,
				"A " + (destino.getAlias() != null ? destino.getAlias() : destino.getId()) + ": " + descripcion,
				origen);
			Movimiento recibo = new Movimiento(null, LocalDateTime.now(), monto,
				TipoMovimiento.TRANSFERENCIA_RECIBIDA,
				"De " + (origen.getAlias() != null ? origen.getAlias() : origen.getId()) + ": " + descripcion,
				destino);
			movimientoDao.grabar(envio);
			movimientoDao.grabar(recibo);
		} catch (SQLException e) {
			throw new GrabandoException("Error al realizar la transferencia: " + e.getMessage());
		}
	}
}
