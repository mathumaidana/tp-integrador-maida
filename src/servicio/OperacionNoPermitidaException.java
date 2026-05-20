package servicio;

public class OperacionNoPermitidaException extends Exception {
	private static final long serialVersionUID = 1L;

	public OperacionNoPermitidaException(String mensaje) {
		super(mensaje);
	}
}
