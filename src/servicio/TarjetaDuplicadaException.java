package servicio;

public class TarjetaDuplicadaException extends Exception {
	private static final long serialVersionUID = 1L;

	public TarjetaDuplicadaException(String mensaje) {
		super(mensaje);
	}
}
