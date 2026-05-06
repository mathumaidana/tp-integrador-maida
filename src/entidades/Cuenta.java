package entidades;

import java.util.ArrayList;
import java.util.List;

public class Cuenta {
	private Integer id;
	private String alias;
	private String cbu;
	private Double saldo;
	private TipoCuenta tipo;
	private Cliente titular;
	private List<Movimiento> movimientos;

	public Cuenta() {
		this.movimientos = new ArrayList<>();
	}

	public Cuenta(Integer id, String alias, String cbu, Double saldo, TipoCuenta tipo, Cliente titular) {
		this.id = id;
		this.alias = alias;
		this.cbu = cbu;
		this.saldo = saldo;
		this.tipo = tipo;
		this.titular = titular;
		this.movimientos = new ArrayList<>();
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

	public List<Movimiento> getMovimientos() {
		return movimientos;
	}

	public void setMovimientos(List<Movimiento> movimientos) {
		this.movimientos = movimientos;
	}

	public void agregarMovimiento(Movimiento m) {
		this.movimientos.add(m);
	}

	@Override
	public String toString() {
		return id + " - " + tipo + " - alias: " + alias + " - saldo: " + saldo;
	}
}
