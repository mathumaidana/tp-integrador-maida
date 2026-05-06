package servicio;

public class ClienteInexistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public ClienteInexistenteException(String mensaje) {
		super(mensaje);
	}
}
