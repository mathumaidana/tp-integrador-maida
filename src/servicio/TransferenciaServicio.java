package servicio;

import java.sql.SQLException;
import java.time.LocalDateTime;

import entidades.Cuenta;
import entidades.Movimiento;
import entidades.TipoMovimiento;
import persistencia.TransferenciaDao;

public class TransferenciaServicio {

	private final TransferenciaDao transferenciaDao;

	public TransferenciaServicio(TransferenciaDao transferenciaDao) {
		this.transferenciaDao = transferenciaDao;
	}

	public void transferir(Cuenta origen, Cuenta destino, Double monto, String descripcion)
			throws SaldoInsuficienteException, GrabandoException, TransferenciaInvalidaException {
		if (origen == null || destino == null) {
			throw new TransferenciaInvalidaException("Indicá las cuentas origen y destino.");
		}
		if (origen.esLaMismaQue(destino)) {
			throw new TransferenciaInvalidaException("No podés transferirte a la misma cuenta.");
		}
		if (!origen.mismaMonedaQue(destino)) {
			throw new TransferenciaInvalidaException(
				"La moneda de las cuentas no coincide (" + origen.getMoneda() + " vs " + destino.getMoneda() + ").");
		}

		Double saldoOrigenPrevio = origen.getSaldo();
		Double saldoDestinoPrevio = destino.getSaldo();

		origen.debitar(monto);
		destino.acreditar(monto);

		try {
			transferenciaDao.transferir(origen.getId(), origen.getSaldo(), destino.getId(), destino.getSaldo(),
				crearMovimiento(origen, destino, monto, descripcion, true),
				crearMovimiento(destino, origen, monto, descripcion, false));
		} catch (SQLException e) {
			origen.setSaldo(saldoOrigenPrevio);
			destino.setSaldo(saldoDestinoPrevio);
			throw new GrabandoException("Error al realizar la transferencia: " + e.getMessage());
		}
	}

	private Movimiento crearMovimiento(Cuenta propia, Cuenta otra, Double monto, String descripcion, boolean envio) {
		String prefijo = envio ? "A " : "De ";
		String detalle = prefijo + otra.referencia();
		if (descripcion != null && !descripcion.isEmpty()) {
			detalle += ": " + descripcion;
		}
		return new Movimiento(null, LocalDateTime.now(), monto,
			envio ? TipoMovimiento.TRANSFERENCIA_ENVIADA : TipoMovimiento.TRANSFERENCIA_RECIBIDA,
			detalle, propia);
	}
}
