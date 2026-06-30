package entidades;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Cheque {

	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private Integer id;
	private String numero;
	private Double monto;
	private Cuenta cuenta;
	private String beneficiario;
	private LocalDateTime fechaEmision;
	private EstadoCheque estado;

	public Cheque() {
		this.estado = EstadoCheque.PENDIENTE;
	}

	public Cheque(Integer id, String numero, Double monto, Cuenta cuenta, String beneficiario,
			LocalDateTime fechaEmision, EstadoCheque estado) {
		this.id = id;
		this.numero = numero;
		this.monto = monto;
		this.cuenta = cuenta;
		this.beneficiario = beneficiario;
		this.fechaEmision = fechaEmision;
		this.estado = estado;
	}

	public boolean estaPendiente() {
		return estado == EstadoCheque.PENDIENTE;
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

	public Double getMonto() {
		return monto;
	}

	public void setMonto(Double monto) {
		this.monto = monto;
	}

	public Cuenta getCuenta() {
		return cuenta;
	}

	public void setCuenta(Cuenta cuenta) {
		this.cuenta = cuenta;
	}

	public String getBeneficiario() {
		return beneficiario;
	}

	public void setBeneficiario(String beneficiario) {
		this.beneficiario = beneficiario;
	}

	public LocalDateTime getFechaEmision() {
		return fechaEmision;
	}

	public void setFechaEmision(LocalDateTime fechaEmision) {
		this.fechaEmision = fechaEmision;
	}

	public EstadoCheque getEstado() {
		return estado;
	}

	public void setEstado(EstadoCheque estado) {
		this.estado = estado;
	}

	@Override
	public String toString() {
		String f = fechaEmision != null ? fechaEmision.format(FORMATO_FECHA) : "(sin fecha)";
		return "N° " + numero + "  $" + String.format("%.2f", monto)
			+ "  → " + beneficiario + "  [" + estado + "]  " + f;
	}
}
