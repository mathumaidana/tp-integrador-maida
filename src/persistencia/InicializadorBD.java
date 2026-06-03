package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InicializadorBD extends BaseH2 {

	public InicializadorBD() {
		super();
	}

	public void crear() throws SQLException {
		String usuarios = "CREATE TABLE IF NOT EXISTS USUARIOS ("
			+ "ID INT AUTO_INCREMENT PRIMARY KEY, "
			+ "USERNAME VARCHAR(50) NOT NULL UNIQUE, "
			+ "PASSWORD VARCHAR(100) NOT NULL, "
			+ "NOMBRE VARCHAR(100) NOT NULL, "
			+ "APELLIDO VARCHAR(100) NOT NULL, "
			+ "DNI VARCHAR(20) NOT NULL, "
			+ "ROL VARCHAR(20) NOT NULL"
			+ ")";

		String cuentas = "CREATE TABLE IF NOT EXISTS CUENTAS ("
			+ "ID INT AUTO_INCREMENT PRIMARY KEY, "
			+ "ALIAS VARCHAR(50) UNIQUE, "
			+ "CBU VARCHAR(30) UNIQUE, "
			+ "SALDO DOUBLE NOT NULL DEFAULT 0, "
			+ "TIPO VARCHAR(30) NOT NULL, "
			+ "ID_TITULAR INT NOT NULL, "
			+ "FOREIGN KEY (ID_TITULAR) REFERENCES USUARIOS(ID)"
			+ ")";

		String movimientos = "CREATE TABLE IF NOT EXISTS MOVIMIENTOS ("
			+ "ID INT AUTO_INCREMENT PRIMARY KEY, "
			+ "FECHA TIMESTAMP NOT NULL, "
			+ "MONTO DOUBLE NOT NULL, "
			+ "TIPO VARCHAR(30) NOT NULL, "
			+ "DESCRIPCION VARCHAR(255), "
			+ "ID_CUENTA INT, "
			+ "ID_TARJETA INT"
			+ ")";

		String tarjetas = "CREATE TABLE IF NOT EXISTS TARJETAS ("
			+ "ID INT AUTO_INCREMENT PRIMARY KEY, "
			+ "NUMERO VARCHAR(20) NOT NULL UNIQUE, "
			+ "ID_TITULAR INT NOT NULL, "
			+ "DISPONIBLE DOUBLE NOT NULL DEFAULT 0, "
			+ "SALDO_A_PAGAR DOUBLE NOT NULL DEFAULT 0, "
			+ "FOREIGN KEY (ID_TITULAR) REFERENCES USUARIOS(ID)"
			+ ")";

		updateDeleteInsertSql(usuarios);
		updateDeleteInsertSql(cuentas);
		updateDeleteInsertSql(movimientos);
		updateDeleteInsertSql(tarjetas);

		seedEmpleadoInicial();
	}

	private void seedEmpleadoInicial() throws SQLException {
		ResultSet rs = selectSql("SELECT COUNT(*) FROM USUARIOS WHERE ROL = ?", "EMPLEADO");
		boolean existe = false;
		try {
			if (rs.next()) {
				existe = rs.getInt(1) > 0;
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (!existe) {
			updateDeleteInsertSql(
				"INSERT INTO USUARIOS (USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI, ROL) VALUES (?,?,?,?,?,?)",
				"admin", "admin", "Admin", "Sistema", "00000000", "EMPLEADO"
			);
		}
	}
}
