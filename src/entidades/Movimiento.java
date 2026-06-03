package entidades;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Movimiento {

	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private Integer id;
	private LocalDateTime fecha;
	private Double monto;
	private TipoMovimiento tipo;
	private String descripcion;
	private Cuenta cuenta;
	private Tarjeta tarjeta;

	public Movimiento() {
	}

	public Movimiento(Integer id, LocalDateTime fecha, Double monto, TipoMovimiento tipo, String descripcion, Cuenta cuenta) {
		this(id, fecha, monto, tipo, descripcion, cuenta, null);
	}

	public Movimiento(Integer id, LocalDateTime fecha, Double monto, TipoMovimiento tipo, String descripcion,
			Cuenta cuenta, Tarjeta tarjeta) {
		this.id = id;
		this.fecha = fecha;
		this.monto = monto;
		this.tipo = tipo;
		this.descripcion = descripcion;
		this.cuenta = cuenta;
		this.tarjeta = tarjeta;
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

	public Tarjeta getTarjeta() {
		return tarjeta;
	}

	public void setTarjeta(Tarjeta tarjeta) {
		this.tarjeta = tarjeta;
	}

	@Override
	public String toString() {
		String fechaFormateada = fecha != null ? fecha.format(FORMATO_FECHA) : "(sin fecha)";
		return fechaFormateada + "  " + tipo + "  $" + monto + (descripcion != null ? "  -  " + descripcion : "");
	}
}
