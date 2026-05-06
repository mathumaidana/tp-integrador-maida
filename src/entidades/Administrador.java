package entidades;

public class Administrador extends Usuario {

	public Administrador() {
		super();
	}

	public Administrador(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
	}

	@Override
	public String getRol() {
		return "ADMIN";
	}
}
