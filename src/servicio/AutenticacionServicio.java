package servicio;

import java.sql.SQLException;

import entidades.Usuario;
import persistencia.UsuarioDao;

public class AutenticacionServicio {

	private final UsuarioDao usuarioDao;

	public AutenticacionServicio(UsuarioDao usuarioDao) {
		this.usuarioDao = usuarioDao;
	}

	public Usuario autenticar(String username, String password) throws AutenticacionException {
		if (username == null || username.trim().isEmpty()) {
			throw new AutenticacionException("El usuario no puede estar vacío");
		}
		if (password == null || password.isEmpty()) {
			throw new AutenticacionException("La contraseña no puede estar vacía");
		}
		try {
			Usuario u = usuarioDao.buscarPorUsername(username);
			if (u == null) {
				throw new AutenticacionException("Usuario o contraseña inválidos");
			}
			if (!u.getPassword().equals(password)) {
				throw new AutenticacionException("Usuario o contraseña inválidos");
			}
			return u;
		} catch (SQLException e) {
			throw new AutenticacionException("Error consultando la base: " + e.getMessage());
		}
	}
}
