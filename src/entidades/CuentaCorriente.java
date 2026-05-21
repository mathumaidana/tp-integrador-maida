package entidades;

public class CuentaCorriente extends Cuenta {

	private static final double ACUERDO_POR_DEFECTO = 50000.0;

	private double acuerdoDescubierto;

	public CuentaCorriente() {
		super();
		this.acuerdoDescubierto = ACUERDO_POR_DEFECTO;
	}

	public CuentaCorriente(TipoCuenta tipo) {
		super(tipo);
		this.acuerdoDescubierto = ACUERDO_POR_DEFECTO;
	}

	public CuentaCorriente(Integer id, String alias, String cbu, Double saldo, TipoCuenta tipo, Cliente titular) {
		super(id, alias, cbu, saldo, tipo, titular);
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

	public double getAcuerdoDescubierto() {
		return acuerdoDescubierto;
	}

	public void setAcuerdoDescubierto(double acuerdoDescubierto) {
		this.acuerdoDescubierto = acuerdoDescubierto;
	}
}
