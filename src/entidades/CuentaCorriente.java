package entidades;

public class CuentaCorriente extends Cuenta {

	private static final double ACUERDO_POR_DEFECTO = 50000.0;

	private final double acuerdoDescubierto;

	public CuentaCorriente(TipoCuenta tipo) {
		super(tipo);
		this.acuerdoDescubierto = ACUERDO_POR_DEFECTO;
	}

	@Override
	public double saldoMinimo() {
		return -acuerdoDescubierto;
	}

	@Override
	public boolean permiteCheques() {
		return true;
	}
}
