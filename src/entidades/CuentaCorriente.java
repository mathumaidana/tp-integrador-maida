package entidades;

public class CuentaCorriente extends Cuenta {

	private static final double ACUERDO_POR_DEFECTO = 50000.0;

	private final double acuerdoDescubierto;

	public CuentaCorriente(TipoCuenta tipo) {
		this(tipo, ACUERDO_POR_DEFECTO);
	}

	public CuentaCorriente(TipoCuenta tipo, double acuerdoDescubierto) {
		super(tipo);
		this.acuerdoDescubierto = acuerdoDescubierto;
	}

	public double getAcuerdoDescubierto() {
		return acuerdoDescubierto;
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
