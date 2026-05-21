package entidades;

import java.util.ArrayList;
import java.util.List;

import vista.MenuClienteView;
import vista.MenuView;

public class Cliente extends Usuario {
	private List<Cuenta> cuentas;
	private List<Tarjeta> tarjetas;

	public Cliente() {
		super();
		this.cuentas = new ArrayList<>();
		this.tarjetas = new ArrayList<>();
	}

	public Cliente(Integer id, String username, String password, String nombre, String apellido, String dni) {
		super(id, username, password, nombre, apellido, dni);
		this.cuentas = new ArrayList<>();
		this.tarjetas = new ArrayList<>();
	}

	@Override
	public String getRol() {
		return "CLIENTE";
	}

	@Override
	public MenuView crearMenu() {
		return new MenuClienteView(this);
	}

	public List<Cuenta> getCuentas() {
		return cuentas;
	}

	public void setCuentas(List<Cuenta> cuentas) {
		this.cuentas = cuentas;
	}

	public void agregarCuenta(Cuenta cuenta) {
		this.cuentas.add(cuenta);
	}

	public List<Tarjeta> getTarjetas() {
		return tarjetas;
	}

	public void setTarjetas(List<Tarjeta> tarjetas) {
		this.tarjetas = tarjetas;
	}

	public void agregarTarjeta(Tarjeta tarjeta) {
		this.tarjetas.add(tarjeta);
	}
}
