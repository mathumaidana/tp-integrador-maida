package servicio;

public class EmpleadoInexistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public EmpleadoInexistenteException(String mensaje) {
		super(mensaje);
	}
}
