package entidades;

import java.time.LocalDateTime;

public class Movimiento {
	private Integer id;
	private LocalDateTime fecha;
	private Double monto;
	private TipoMovimiento tipo;
	private String descripcion;
	private Cuenta cuenta;

	public Movimiento() {
	}

	public Movimiento(Integer id, LocalDateTime fecha, Double monto, TipoMovimiento tipo, String descripcion, Cuenta cuenta) {
		this.id = id;
		this.fecha = fecha;
		this.monto = monto;
		this.tipo = tipo;
		this.descripcion = descripcion;
		this.cuenta = cuenta;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public Double getMonto() {
		return monto;
	}

	public void setMonto(Double monto) {
		this.monto = monto;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public void setTipo(TipoMovimiento tipo) {
		this.tipo = tipo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Cuenta getCuenta() {
		return cuenta;
	}

	public void setCuenta(Cuenta cuenta) {
		this.cuenta = cuenta;
	}

	@Override
	public String toString() {
		return fecha + " - " + tipo + " - $" + monto + " - " + descripcion;
	}
}
