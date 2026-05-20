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

public class MovimientoDao extends BaseH2 implements ICrud<Movimiento> {

	private static final String COLS = "ID, FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA, ID_TARJETA";

	private final CuentaDao cuentaDao;
	private final TarjetaDao tarjetaDao;

	public MovimientoDao() {
		super();
		this.cuentaDao = new CuentaDao();
		this.tarjetaDao = new TarjetaDao();
	}

	@Override
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

	@Override
	public Movimiento leer(Integer id) throws SQLException {
		String sql = "SELECT " + COLS + " FROM MOVIMIENTOS WHERE ID = ?";
		ResultSet rs = selectSql(sql, id);
		Movimiento mov = null;
		Integer idCuenta = null;
		Integer idTarjeta = null;
		try {
			if (rs.next()) {
				mov = mapearSinRelaciones(rs);
				idCuenta = (Integer) rs.getObject("ID_CUENTA");
				idTarjeta = (Integer) rs.getObject("ID_TARJETA");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (mov != null) {
			if (idCuenta != null) mov.setCuenta(cuentaDao.leer(idCuenta));
			if (idTarjeta != null) mov.setTarjeta(tarjetaDao.leer(idTarjeta));
		}
		return mov;
	}

	@Override
	public List<Movimiento> leer() throws SQLException {
		String sql = "SELECT " + COLS + " FROM MOVIMIENTOS ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql);
		List<Movimiento> lista = new ArrayList<>();
		List<Integer> idsCuentas = new ArrayList<>();
		List<Integer> idsTarjetas = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinRelaciones(rs));
				idsCuentas.add((Integer) rs.getObject("ID_CUENTA"));
				idsTarjetas.add((Integer) rs.getObject("ID_TARJETA"));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		for (int i = 0; i < lista.size(); i++) {
			if (idsCuentas.get(i) != null) lista.get(i).setCuenta(cuentaDao.leer(idsCuentas.get(i)));
			if (idsTarjetas.get(i) != null) lista.get(i).setTarjeta(tarjetaDao.leer(idsTarjetas.get(i)));
		}
		return lista;
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

	@Override
	public void modificar(Movimiento m) throws SQLException {
		String sql = "UPDATE MOVIMIENTOS SET FECHA = ?, MONTO = ?, TIPO = ?, DESCRIPCION = ?, "
			+ "ID_CUENTA = ?, ID_TARJETA = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			Timestamp.valueOf(m.getFecha()).toString(),
			m.getMonto(),
			m.getTipo().name(),
			m.getDescripcion(),
			m.getCuenta() != null ? m.getCuenta().getId() : null,
			m.getTarjeta() != null ? m.getTarjeta().getId() : null,
			m.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM MOVIMIENTOS WHERE ID = ?";
		updateDeleteInsertSql(sql, id);
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
