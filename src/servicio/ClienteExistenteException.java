package servicio;

public class ClienteExistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public ClienteExistenteException(String mensaje) {
		super(mensaje);
	}
}
