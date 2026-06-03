package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import entidades.Cuenta;
import entidades.Movimiento;
import entidades.Tarjeta;
import entidades.TipoMovimiento;

public class MovimientoDao extends BaseH2 {

	private static final String COLS = "ID, FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA, ID_TARJETA";

	public MovimientoDao() {
		super();
	}

	public void grabar(Movimiento m) throws SQLException {
		String sql = "INSERT INTO MOVIMIENTOS (FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA, ID_TARJETA) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			Timestamp.valueOf(m.getFecha()).toString(),
			m.getMonto(),
			m.getTipo().name(),
			m.getDescripcion(),
			m.getCuenta() != null ? m.getCuenta().getId() : null,
			m.getTarjeta() != null ? m.getTarjeta().getId() : null
		);
	}

	public List<Movimiento> leerPorCuenta(Cuenta cuenta) throws SQLException {
		String sql = "SELECT " + COLS + " FROM MOVIMIENTOS WHERE ID_CUENTA = ? ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql, cuenta.getId());
		List<Movimiento> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				Movimiento m = mapearSinRelaciones(rs);
				m.setCuenta(cuenta);
				lista.add(m);
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		return lista;
	}

	public List<Movimiento> leerPorTarjeta(Tarjeta tarjeta) throws SQLException {
		String sql = "SELECT " + COLS + " FROM MOVIMIENTOS WHERE ID_TARJETA = ? ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql, tarjeta.getId());
		List<Movimiento> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				Movimiento m = mapearSinRelaciones(rs);
				m.setTarjeta(tarjeta);
				lista.add(m);
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		return lista;
	}

	public List<Movimiento> leerPorTarjetaYMes(Tarjeta tarjeta, YearMonth mes) throws SQLException {
		LocalDateTime desde = mes.atDay(1).atStartOfDay();
		LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay();
		String sql = "SELECT " + COLS + " FROM MOVIMIENTOS "
			+ "WHERE ID_TARJETA = ? AND FECHA >= ? AND FECHA < ? ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql, tarjeta.getId(),
			Timestamp.valueOf(desde).toString(), Timestamp.valueOf(hasta).toString());
		List<Movimiento> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				Movimiento m = mapearSinRelaciones(rs);
				m.setTarjeta(tarjeta);
				lista.add(m);
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		return lista;
	}

	public void borrarPorCuenta(Integer idCuenta) throws SQLException {
		updateDeleteInsertSql("DELETE FROM MOVIMIENTOS WHERE ID_CUENTA = ?", idCuenta);
	}

	public void borrarPorTarjeta(Integer idTarjeta) throws SQLException {
		updateDeleteInsertSql("DELETE FROM MOVIMIENTOS WHERE ID_TARJETA = ?", idTarjeta);
	}

	private Movimiento mapearSinRelaciones(ResultSet rs) throws SQLException {
		Movimiento m = new Movimiento();
		m.setId(rs.getInt("ID"));
		Timestamp ts = rs.getTimestamp("FECHA");
		if (ts != null) m.setFecha(ts.toLocalDateTime());
		m.setMonto(rs.getDouble("MONTO"));
		m.setTipo(TipoMovimiento.valueOf(rs.getString("TIPO")));
		m.setDescripcion(rs.getString("DESCRIPCION"));
		return m;
	}
}
