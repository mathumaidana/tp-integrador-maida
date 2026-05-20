package servicio;

public class AdministradorInexistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public AdministradorInexistenteException(String mensaje) {
		super(mensaje);
	}
}
