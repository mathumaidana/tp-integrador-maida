package entidades;

import vista.MenuClienteView;
import vista.MenuView;

public class Cliente extends Usuario {

	public Cliente() {
		super();
	}

	public Cliente(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
	}

	@Override
	public String getRol() {
		return "CLIENTE";
	}

	@Override
	public MenuView crearMenu() {
		return new MenuClienteView(this);
	}
}
