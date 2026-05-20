package servicio;

public class CuentaDuplicadaException extends Exception {
	private static final long serialVersionUID = 1L;

	public CuentaDuplicadaException(String mensaje) {
		super(mensaje);
	}
}
