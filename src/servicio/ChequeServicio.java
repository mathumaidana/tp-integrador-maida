package servicio;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import entidades.Cheque;
import entidades.Cuenta;
import entidades.EstadoCheque;
import entidades.Movimiento;
import entidades.SaldoInsuficienteException;
import entidades.TipoMovimiento;
import persistencia.ChequeDao;

public class ChequeServicio {

	private final ChequeDao chequeDao;

	public ChequeServicio(ChequeDao chequeDao) {
		this.chequeDao = chequeDao;
	}

	public void emitir(Cheque ch)
			throws OperacionNoPermitidaException, ChequeDuplicadoException, GrabandoException, DatosInvalidosException {
		if (ch.getCuenta() == null) {
			throw new OperacionNoPermitidaException("Seleccioná la cuenta emisora del cheque.");
		}
		if (!ch.getCuenta().permiteCheques()) {
			throw new OperacionNoPermitidaException(
				"La cuenta " + ch.getCuenta().referencia()
				+ " no permite cheques. Solo las cuentas corrientes pueden emitir cheques.");
		}
		if (ch.getMonto() == null || ch.getMonto() <= 0) {
			throw new DatosInvalidosException("El monto del cheque debe ser mayor a cero.");
		}
		try {
			if (chequeDao.buscarPorNumero(ch.getNumero()) != null) {
				throw new ChequeDuplicadoException("Ya existe un cheque con número '" + ch.getNumero() + "'");
			}
			ch.setFechaEmision(LocalDateTime.now());
			ch.setEstado(EstadoCheque.PENDIENTE);
			chequeDao.grabar(ch);
		} catch (SQLException e) {
			throw new GrabandoException("Error al emitir el cheque: " + e.getMessage());
		}
	}

	public void cobrar(Cheque ch)
			throws OperacionNoPermitidaException, SaldoInsuficienteException, GrabandoException {
		if (!ch.estaPendiente()) {
			throw new OperacionNoPermitidaException(
				"El cheque ya está " + ch.getEstado() + "; solo se puede cobrar un cheque pendiente.");
		}
		Cuenta cuenta = ch.getCuenta();
		Double saldoPrevio = cuenta.getSaldo();
		cuenta.debitar(ch.getMonto());
		try {
			Movimiento m = new Movimiento(null, LocalDateTime.now(), ch.getMonto(),
				TipoMovimiento.CHEQUE_COBRADO,
				"Cheque " + ch.getNumero() + " a " + ch.getBeneficiario(), cuenta);
			chequeDao.cobrar(ch, m);
			ch.setEstado(EstadoCheque.COBRADO);
		} catch (SQLException e) {
			cuenta.setSaldo(saldoPrevio);
			throw new GrabandoException("Error al cobrar el cheque: " + e.getMessage());
		}
	}

	public void anular(Cheque ch) throws OperacionNoPermitidaException, GrabandoException {
		if (!ch.estaPendiente()) {
			throw new OperacionNoPermitidaException("Solo se puede anular un cheque pendiente.");
		}
		EstadoCheque previo = ch.getEstado();
		ch.setEstado(EstadoCheque.ANULADO);
		try {
			chequeDao.modificar(ch);
		} catch (SQLException e) {
			ch.setEstado(previo);
			throw new GrabandoException("Error al anular el cheque: " + e.getMessage());
		}
	}

	public List<Cheque> listarPorCuenta(Cuenta cuenta) throws LeyendoException {
		try {
			return chequeDao.leerPorCuenta(cuenta.getId());
		} catch (SQLException e) {
			throw new LeyendoException("Error al listar cheques: " + e.getMessage());
		}
	}
}
