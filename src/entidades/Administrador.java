package entidades;

import vista.MenuAdminView;
import vista.MenuView;

public class Administrador extends Empleado {

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

	@Override
	public MenuView crearMenu() {
		return new MenuAdminView(this);
	}
}
