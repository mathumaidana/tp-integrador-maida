package servicio;

public class EmpleadoExistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public EmpleadoExistenteException(String mensaje) {
		super(mensaje);
	}
}
