package entidades;

public class Cliente extends Usuario {

	public Cliente() {
		super();
	}

	public Cliente(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
	}
}
