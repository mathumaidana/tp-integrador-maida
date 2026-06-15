package entidades;


public class Tarjeta {
	private Integer id;
	private String numero;
	private Cliente titular;
	private Double disponible;
	private Double saldoAPagar;

	public Tarjeta() {
		this.disponible = 0.0;
		this.saldoAPagar = 0.0;
	}

	public void debitar(Double monto) throws SaldoInsuficienteException {
		if (monto == null || monto <= 0) {
			throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
		}
		if (disponible < monto) {
			throw new SaldoInsuficienteException("Disponible insuficiente en la tarjeta " + ultimosCuatro());
		}
		this.disponible -= monto;
		this.saldoAPagar += monto;
	}

	public void pagar(Double monto) {
		if (monto == null || monto <= 0) {
			throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
		}
		this.disponible += monto;
		this.saldoAPagar = Math.max(0.0, this.saldoAPagar - monto);
	}

	public String ultimosCuatro() {
		if (numero == null) return "????";
		return numero.length() >= 4 ? numero.substring(numero.length() - 4) : numero;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Cliente getTitular() {
		return titular;
	}

	public void setTitular(Cliente titular) {
		this.titular = titular;
	}

	public Double getDisponible() {
		return disponible;
	}

	public void setDisponible(Double disponible) {
		this.disponible = disponible;
	}

	public Double getSaldoAPagar() {
		return saldoAPagar;
	}

	public void setSaldoAPagar(Double saldoAPagar) {
		this.saldoAPagar = saldoAPagar;
	}

	@Override
	public String toString() {
		return "**** " + ultimosCuatro() + " (disp $" + String.format("%.2f", disponible)
			+ " · a pagar $" + String.format("%.2f", saldoAPagar) + ")";
	}
}
