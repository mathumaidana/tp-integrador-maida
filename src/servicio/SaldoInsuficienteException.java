package servicio;

/** @deprecated Movida a {@link entidades.SaldoInsuficienteException} */
@Deprecated
public class SaldoInsuficienteException extends entidades.SaldoInsuficienteException {
	private static final long serialVersionUID = 1L;

	public SaldoInsuficienteException(String mensaje) {
		super(mensaje);
	}
}
