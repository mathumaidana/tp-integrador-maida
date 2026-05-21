package entidades;

public class CajaAhorro extends Cuenta {

	public CajaAhorro() {
		super();
	}

	public CajaAhorro(TipoCuenta tipo) {
		super(tipo);
	}

	public CajaAhorro(Integer id, String alias, String cbu, Double saldo, TipoCuenta tipo, Cliente titular) {
		super(id, alias, cbu, saldo, tipo, titular);
	}

	@Override
	public double saldoMinimo() {
		return 0.0;
	}

	@Override
	public boolean permiteCheques() {
		return false;
	}
}
