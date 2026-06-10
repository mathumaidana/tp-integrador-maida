package entidades;

public class CajaAhorro extends Cuenta {

	public CajaAhorro(TipoCuenta tipo) {
		super(tipo);
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
