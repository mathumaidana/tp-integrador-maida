package persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseH2 {

	private final String driver;
	private final String url;
	private final String username;
	private final String passwd;

	private Connection connection;

	protected BaseH2() {
		driver = "org.h2.Driver";
		url = "jdbc:h2:./data/banco";
		username = "sa";
		passwd = "";
	}

	private final void cargarDriver() throws SQLException {
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			throw new SQLException("No se pudo cargar el driver de la base de datos", e);
		}
	}

	private final void obtenerConexion() throws SQLException {
		connection = DriverManager.getConnection(url, username, passwd);
	}

	protected final void cerrarConexion() throws SQLException {
		if (connection != null) connection.close();
	}

	protected final int updateDeleteInsertSql(String sql, Object... params) throws SQLException {
		cargarDriver();
		obtenerConexion();
		try {
			PreparedStatement s = preparedStatement_v20(sql, params);
			int count = s.executeUpdate();
			s.close();
			return count;
		} finally {
			cerrarConexion();
		}
	}

	protected final ResultSet selectSql(String sql, Object... params) throws SQLException {
		cargarDriver();
		obtenerConexion();
		try {
			PreparedStatement s = preparedStatement_v20(sql, params);
			return s.executeQuery();
		} catch (SQLException e) {
			cerrarConexion();
			throw e;
		}
	}

	protected final void transaccionSql(String[] sqls, Object[][] params) throws SQLException {
		cargarDriver();
		obtenerConexion();
		try {
			connection.setAutoCommit(false);
			for (int i = 0; i < sqls.length; i++) {
				PreparedStatement s = preparedStatement_v20(sqls[i], params[i]);
				s.executeUpdate();
				s.close();
			}
			connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException alRevertir) {
				e.addSuppressed(alRevertir);
			}
			throw e;
		} finally {
			cerrarConexion();
		}
	}

	private PreparedStatement preparedStatement_v20(String sql, Object... params) throws SQLException {
		PreparedStatement s;
		s = connection.prepareStatement(sql);
		int i = 1;
		for (Object param : params) {
			if (param == null) s.setObject(i++, null);
			else if (param instanceof Integer) s.setInt(i++, (Integer) param);
			else if (param instanceof String) s.setString(i++, (String) param);
			else if (param instanceof Double) s.setDouble(i++, (Double) param);
			else if (param instanceof Long) s.setLong(i++, (Long) param);
			else throw new IllegalArgumentException("Unexpected value: " + param);
		}
		return s;
	}
}
