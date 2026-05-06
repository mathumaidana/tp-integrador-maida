package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import entidades.Cuenta;
import entidades.Movimiento;
import entidades.TipoMovimiento;

public class MovimientoDao extends BaseH2 implements ICrud<Movimiento> {

	private final CuentaDao cuentaDao;

	public MovimientoDao() {
		super();
		this.cuentaDao = new CuentaDao();
	}

	@Override
	public void grabar(Movimiento m) throws SQLException {
		String sql = "INSERT INTO MOVIMIENTOS (FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA) VALUES (?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			Timestamp.valueOf(m.getFecha()).toString(),
			m.getMonto(),
			m.getTipo().name(),
			m.getDescripcion(),
			m.getCuenta().getId()
		);
	}

	@Override
	public Movimiento leer(Integer id) throws SQLException {
		String sql = "SELECT ID, FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA FROM MOVIMIENTOS WHERE ID = ?";
		ResultSet rs = selectSql(sql, id);
		Movimiento mov = null;
		Integer idCuenta = null;
		try {
			if (rs.next()) {
				mov = mapearSinCuenta(rs);
				idCuenta = rs.getInt("ID_CUENTA");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (mov != null && idCuenta != null) {
			mov.setCuenta(cuentaDao.leer(idCuenta));
		}
		return mov;
	}

	@Override
	public List<Movimiento> leer() throws SQLException {
		String sql = "SELECT ID, FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA FROM MOVIMIENTOS ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql);
		List<Movimiento> lista = new ArrayList<>();
		List<Integer> idsCuentas = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinCuenta(rs));
				idsCuentas.add(rs.getInt("ID_CUENTA"));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		for (int i = 0; i < lista.size(); i++) {
			lista.get(i).setCuenta(cuentaDao.leer(idsCuentas.get(i)));
		}
		return lista;
	}

	public List<Movimiento> leerPorCuenta(Cuenta cuenta) throws SQLException {
		String sql = "SELECT ID, FECHA, MONTO, TIPO, DESCRIPCION, ID_CUENTA FROM MOVIMIENTOS WHERE ID_CUENTA = ? ORDER BY FECHA DESC";
		ResultSet rs = selectSql(sql, cuenta.getId());
		List<Movimiento> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				Movimiento m = mapearSinCuenta(rs);
				m.setCuenta(cuenta);
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
		String sql = "UPDATE MOVIMIENTOS SET FECHA = ?, MONTO = ?, TIPO = ?, DESCRIPCION = ?, ID_CUENTA = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			Timestamp.valueOf(m.getFecha()).toString(),
			m.getMonto(),
			m.getTipo().name(),
			m.getDescripcion(),
			m.getCuenta().getId(),
			m.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM MOVIMIENTOS WHERE ID = ?";
		updateDeleteInsertSql(sql, id);
	}

	private Movimiento mapearSinCuenta(ResultSet rs) throws SQLException {
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
