package entidades;

public abstract class Usuario {
	private Integer id;
	private String username;
	private String password;
	private String nombre;
	private String apellido;
	private String dni;

	public Usuario() {
	}

	public Usuario(Integer id, String username, String password, String nombre, String apellido, String dni) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.nombre = nombre;
		this.apellido = apellido;
		this.dni = dni;
	}

	public boolean autenticaCon(String password) {
		return this.password != null && this.password.equals(password);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getDni() {
		return dni;
	}

	public void setDni(String dni) {
		this.dni = dni;
	}

	public String getNombreCompleto() {
		return nombre + " " + apellido;
	}

	@Override
	public String toString() {
		return id + " - " + apellido + ", " + nombre + " (" + username + ")";
	}
}
