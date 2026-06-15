package entidades;

public class Empleado extends Usuario {

	public Empleado() {
		super();
	}

	public Empleado(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
	}
}
