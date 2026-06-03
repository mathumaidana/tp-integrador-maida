package servicio;

public class SaldoInicialInvalidoException extends Exception {
	private static final long serialVersionUID = 1L;

	public SaldoInicialInvalidoException(String mensaje) {
		super(mensaje);
	}
}
