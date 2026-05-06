package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Administrador;

public class AdministradorDao extends BaseH2 implements ICrud<Administrador> {

	public AdministradorDao() {
		super();
	}

	@Override
	public void grabar(Administrador a) throws SQLException {
		String sql = "INSERT INTO USUARIOS (USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL) VALUES (?, ?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			a.getUsername(), a.getPassword(), a.getNombre(), a.getApellido(), a.getDni(), "ADMIN"
		);
	}

	@Override
	public Administrador leer(Integer id) throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI FROM USUARIOS WHERE ID = ? AND ROL = 'ADMIN'";
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
	public List<Administrador> leer() throws SQLException {
		String sql = "SELECT ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI FROM USUARIOS WHERE ROL = 'ADMIN' ORDER BY ID";
		ResultSet rs = selectSql(sql);
		List<Administrador> lista = new ArrayList<>();
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
	public void modificar(Administrador a) throws SQLException {
		String sql = "UPDATE USUARIOS SET USERNAME = ?, PASSWORD = ?, NOMBRE = ?, APELLIDO = ?, DNI = ? WHERE ID = ? AND ROL = 'ADMIN'";
		updateDeleteInsertSql(sql,
			a.getUsername(), a.getPassword(), a.getNombre(), a.getApellido(), a.getDni(), a.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM USUARIOS WHERE ID = ? AND ROL = 'ADMIN'";
		updateDeleteInsertSql(sql, id);
	}

	private Administrador mapear(ResultSet rs) throws SQLException {
		return new Administrador(
			rs.getInt("ID"),
			rs.getString("USERNAME"),
			rs.getString("PASSWORD"),
			rs.getString("NOMBRE"),
			rs.getString("APELLIDO"),
			rs.getString("DNI")
		);
	}
}
