package servicio;

public class CuentaInexistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public CuentaInexistenteException(String mensaje) {
		super(mensaje);
	}
}
