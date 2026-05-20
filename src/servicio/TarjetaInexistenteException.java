package servicio;

public class TarjetaInexistenteException extends Exception {
	private static final long serialVersionUID = 1L;

	public TarjetaInexistenteException(String mensaje) {
		super(mensaje);
	}
}
