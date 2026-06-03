package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Cliente;

public class ClienteDao extends BaseH2 implements ICrud<Cliente> {

	public ClienteDao() {
		super();
	}

	@Override
	public void grabar(Cliente c) throws SQLException {
		String sql = "INSERT INTO USUARIOS (USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL) VALUES (?, ?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			c.getUsername(),
			c.getPassword(),
			c.getNombre(),
			c.getApellido(),
			c.getDni(),
			"CLIENTE"
		);
	}

	@Override
	public Cliente leer(Integer id) throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI FROM USUARIOS WHERE ID = ? AND ROL = 'CLIENTE'";
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

	@Override
	public List<Cliente> leer() throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI FROM USUARIOS WHERE ROL = 'CLIENTE' ORDER BY ID";
		ResultSet rs = selectSql(sql);
		List<Cliente> lista = new ArrayList<>();
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

	@Override
	public void modificar(Cliente c) throws SQLException {
		String sql = "UPDATE USUARIOS SET USERNAME = ?, PASSWORD = ?, NOMBRE = ?, APELLIDO = ?, DNI = ? WHERE ID = ? AND ROL = 'CLIENTE'";
		updateDeleteInsertSql(sql,
			c.getUsername(),
			c.getPassword(),
			c.getNombre(),
			c.getApellido(),
			c.getDni(),
			c.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM USUARIOS WHERE ID = ? AND ROL = 'CLIENTE'";
		updateDeleteInsertSql(sql, id);
	}

	private Cliente mapear(ResultSet rs) throws SQLException {
		return new Cliente(
			rs.getInt("ID"),
			rs.getString("USERNAME"),
			rs.getString("PASSWORD"),
			rs.getString("NOMBRE"),
			rs.getString("APELLIDO"),
			rs.getString("DNI")
		);
	}
}
