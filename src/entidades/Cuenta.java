package entidades;

import servicio.SaldoInsuficienteException;

public abstract class Cuenta {
	protected Integer id;
	protected String alias;
	protected String cbu;
	protected Double saldo;
	protected TipoCuenta tipo;
	protected Cliente titular;

	protected Cuenta() {
		this.saldo = 0.0;
	}

	protected Cuenta(TipoCuenta tipo) {
		this();
		this.tipo = tipo;
	}

	protected Cuenta(Integer id, String alias, String cbu, Double saldo, TipoCuenta tipo, Cliente titular) {
		this.id = id;
		this.alias = alias;
		this.cbu = cbu;
		this.saldo = saldo != null ? saldo : 0.0;
		this.tipo = tipo;
		this.titular = titular;
	}

	public abstract double saldoMinimo();

	public abstract boolean permiteCheques();

	public Moneda getMoneda() {
		return tipo != null ? tipo.getMoneda() : null;
	}

	public boolean mismaMonedaQue(Cuenta otra) {
		return otra != null && getMoneda() == otra.getMoneda();
	}

	public boolean esLaMismaQue(Cuenta otra) {
		return otra != null && id != null && id.equals(otra.getId());
	}

	public void debitar(Double monto) throws SaldoInsuficienteException {
		if (monto == null || monto <= 0) {
			throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
		}
		if (saldo - monto < saldoMinimo()) {
			throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + referencia());
		}
		this.saldo -= monto;
	}

	public void acreditar(Double monto) {
		if (monto == null || monto <= 0) {
			throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
		}
		this.saldo += monto;
	}

	public String referencia() {
		if (alias != null && !alias.isEmpty()) return alias;
		if (cbu != null && !cbu.isEmpty()) return cbu;
		return String.valueOf(id);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getCbu() {
		return cbu;
	}

	public void setCbu(String cbu) {
		this.cbu = cbu;
	}

	public Double getSaldo() {
		return saldo;
	}

	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public void setTipo(TipoCuenta tipo) {
		this.tipo = tipo;
	}

	public Cliente getTitular() {
		return titular;
	}

	public void setTitular(Cliente titular) {
		this.titular = titular;
	}

	@Override
	public String toString() {
		return tipo + " " + referencia() + " (saldo $" + saldo + ")";
	}
}
