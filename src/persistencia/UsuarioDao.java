package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;

import entidades.Cliente;
import entidades.Empleado;
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

	private Usuario mapear(ResultSet rs) throws SQLException {
		Integer id = rs.getInt("ID");
		String username = rs.getString("USERNAME");
		String password = rs.getString("PASSWORD");
		String nombre = rs.getString("NOMBRE");
		String apellido = rs.getString("APELLIDO");
		String dni = rs.getString("DNI");
		String rol = rs.getString("ROL");
		if ("EMPLEADO".equals(rol)) {
			return new Empleado(id, username, password, nombre, apellido, dni);
		}
		return new Cliente(id, username, password, nombre, apellido, dni);
	}
}
