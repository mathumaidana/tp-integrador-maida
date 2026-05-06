package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Administrador;
import entidades.Cliente;
import entidades.Usuario;

public class UsuarioDao extends BaseH2 {

	public UsuarioDao() {
		super();
	}

	public Usuario buscarPorUsername(String username) throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL FROM USUARIOS WHERE USERNAME = ?";
		ResultSet rs = selectSql(sql, username);
		try {
			if (rs.next()) {
				return mapear(rs);
			}
			return null;
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
	}

	public Usuario leer(Integer id) throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL FROM USUARIOS WHERE ID = ?";
		ResultSet rs = selectSql(sql, id);
		try {
			if (rs.next()) {
				return mapear(rs);
			}
			return null;
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
	}

	public List<Usuario> leerPorRol(String rol) throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL FROM USUARIOS WHERE ROL = ? ORDER BY ID";
		ResultSet rs = selectSql(sql, rol);
		List<Usuario> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapear(rs));
			}
			return lista;
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
	}

	private Usuario mapear(ResultSet rs) throws SQLException {
		Integer id = rs.getInt("ID");
		String username = rs.getString("USERNAME");
		String password = rs.getString("PASSWORD");
		String nombre = rs.getString("NOMBRE");
		String apellido = rs.getString("APELLIDO");
		String dni = rs.getString("DNI");
		String rol = rs.getString("ROL");
		if ("ADMIN".equals(rol)) {
			return new Administrador(id, username, password, nombre, apellido, dni);
		}
		return new Cliente(id, username, password, nombre, apellido, dni);
	}
}
