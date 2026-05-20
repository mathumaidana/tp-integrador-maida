package servicio;

public class AdministradorExistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public AdministradorExistenteException(String mensaje) {
		super(mensaje);
	}
}
