package vista;

public class CampoVacioException extends Exception {
	private static final long serialVersionUID = 1L;

	public CampoVacioException(String campo) {
		super("El campo '" + campo + "' no puede estar vacío");
	}
}
