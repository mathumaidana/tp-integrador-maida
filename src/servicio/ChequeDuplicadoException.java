package servicio;

public class ChequeDuplicadoException extends Exception {
	private static final long serialVersionUID = 1L;

	public ChequeDuplicadoException(String mensaje) {
		super(mensaje);
	}
}
