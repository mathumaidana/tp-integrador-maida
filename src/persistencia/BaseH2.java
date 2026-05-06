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

	protected BaseH2(String driver, String url, String username, String passwd) {
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.passwd = passwd;
	}

	private final void cargarDriver() {
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			System.exit(0);
		}
	}

	private final void obtenerConexion() throws SQLException {
		try {
			connection = DriverManager.getConnection(url, username, passwd);
		} catch (SQLException e) {
			throw e;
		}
	}

	protected final void cerrarConexion() throws SQLException {
		try {
			if (connection != null) connection.close();
		} catch (SQLException e) {
			throw e;
		}
	}

	protected final int updateDeleteInsertSql(String sql, Object... params) throws SQLException {
		PreparedStatement s;
		int count = 0;
		cargarDriver();
		obtenerConexion();
		try {
			s = preparedStatement_v20(sql, params);
			count = s.executeUpdate();
			s.close();
			return count;
		} catch (SQLException e) {
			throw e;
		} finally {
			cerrarConexion();
		}
	}

	protected final ResultSet selectSql(String sql, Object... params) throws SQLException {
		ResultSet rs;
		cargarDriver();
		obtenerConexion();
		PreparedStatement s;
		try {
			s = preparedStatement_v20(sql, params);
			rs = s.executeQuery();
			return rs;
		} catch (SQLException e) {
			throw e;
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
