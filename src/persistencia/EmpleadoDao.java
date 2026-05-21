package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Empleado;

public class EmpleadoDao extends BaseH2 implements ICrud<Empleado> {

	private static final String COLS = "ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI";
	private static final String FILTRO_ROL = "ROL = 'EMPLEADO'";

	public EmpleadoDao() {
		super();
	}

	@Override
	public void grabar(Empleado e) throws SQLException {
		String sql = "INSERT INTO USUARIOS (USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL) "
			+ "VALUES (?, ?, ?, ?, ?, 'EMPLEADO')";
		updateDeleteInsertSql(sql,
			e.getUsername(), e.getPassword(), e.getNombre(), e.getApellido(), e.getDni()
		);
	}

	@Override
	public Empleado leer(Integer id) throws SQLException {
		String sql = "SELECT " + COLS + " FROM USUARIOS WHERE ID = ? AND " + FILTRO_ROL;
		ResultSet rs = selectSql(sql, id);
		try {
			if (rs.next()) return mapear(rs);
			return null;
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
	}

	@Override
	public List<Empleado> leer() throws SQLException {
		String sql = "SELECT " + COLS + " FROM USUARIOS WHERE " + FILTRO_ROL + " ORDER BY ID";
		ResultSet rs = selectSql(sql);
		List<Empleado> lista = new ArrayList<>();
		try {
			while (rs.next()) lista.add(mapear(rs));
			return lista;
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
	}

	@Override
	public void modificar(Empleado e) throws SQLException {
		String sql = "UPDATE USUARIOS SET USERNAME = ?, PASSWORD = ?, NOMBRE = ?, APELLIDO = ?, DNI = ? "
			+ "WHERE ID = ? AND " + FILTRO_ROL;
		updateDeleteInsertSql(sql,
			e.getUsername(), e.getPassword(), e.getNombre(), e.getApellido(), e.getDni(), e.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		updateDeleteInsertSql("DELETE FROM USUARIOS WHERE ID = ? AND " + FILTRO_ROL, id);
	}

	private Empleado mapear(ResultSet rs) throws SQLException {
		return new Empleado(
			rs.getInt("ID"),
			rs.getString("USERNAME"),
			rs.getString("PASSWORD"),
			rs.getString("NOMBRE"),
			rs.getString("APELLIDO"),
			rs.getString("DNI")
		);
	}
}
