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
		try {
			Usuario u = usuarioDao.buscarPorUsername(username);
			if (u == null || !u.autenticaCon(password)) {
				throw new AutenticacionException("Usuario o contraseña inválidos");
			}
			return u;
		} catch (SQLException e) {
			throw new AutenticacionException("Error consultando la base: " + e.getMessage());
		}
	}
}
