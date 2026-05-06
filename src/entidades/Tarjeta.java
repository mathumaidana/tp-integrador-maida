package entidades;

public class Tarjeta {
	private Integer id;
	private String numero;
	private Cliente titular;
	private Double disponible;
	private Double saldoAPagar;

	public Tarjeta() {
	}

	public Tarjeta(Integer id, String numero, Cliente titular, Double disponible, Double saldoAPagar) {
		this.id = id;
		this.numero = numero;
		this.titular = titular;
		this.disponible = disponible;
		this.saldoAPagar = saldoAPagar;
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
		return id + " - **** " + (numero != null && numero.length() >= 4 ? numero.substring(numero.length() - 4) : numero)
			+ " - disp: " + disponible + " - a pagar: " + saldoAPagar;
	}
}
