package entidades;

import vista.MenuEmpleadoView;
import vista.MenuView;

public class Empleado extends Usuario {

	public Empleado() {
		super();
	}

	public Empleado(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
	}

	@Override
	public MenuView crearMenu() {
		return new MenuEmpleadoView(this);
	}
}
