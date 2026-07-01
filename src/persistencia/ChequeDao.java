package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import entidades.Cheque;
import entidades.EstadoCheque;
import entidades.Movimiento;

public class ChequeDao extends BaseH2 implements ICrud<Cheque> {

	private static final String COLS = "ID, NUMERO, MONTO, BENEFICIARIO, FECHA_EMISION, ESTADO, ID_CUENTA";

	private final CuentaDao cuentaDao;
	private final MovimientoDao movimientoDao;

	public ChequeDao() {
		super();
		this.cuentaDao = new CuentaDao();
		this.movimientoDao = new MovimientoDao();
	}

	@Override
	public void grabar(Cheque ch) throws SQLException {
		String sql = "INSERT INTO CHEQUES (NUMERO, MONTO, BENEFICIARIO, FECHA_EMISION, ESTADO, ID_CUENTA) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			ch.getNumero(),
			ch.getMonto(),
			ch.getBeneficiario(),
			Timestamp.valueOf(ch.getFechaEmision()).toString(),
			ch.getEstado().name(),
			ch.getCuenta().getId()
		);
	}

	@Override
	public Cheque leer(Integer id) throws SQLException {
		return cargarUno("SELECT " + COLS + " FROM CHEQUES WHERE ID = ?", id);
	}

	public Cheque buscarPorNumero(String numero) throws SQLException {
		return cargarUno("SELECT " + COLS + " FROM CHEQUES WHERE NUMERO = ?", numero);
	}

	@Override
	public List<Cheque> leer() throws SQLException {
		return cargarVarias("SELECT " + COLS + " FROM CHEQUES ORDER BY ID", null);
	}

	public List<Cheque> leerPorCuenta(Integer idCuenta) throws SQLException {
		return cargarVarias("SELECT " + COLS + " FROM CHEQUES WHERE ID_CUENTA = ? ORDER BY ID", idCuenta);
	}

	@Override
	public void modificar(Cheque ch) throws SQLException {
		String sql = "UPDATE CHEQUES SET NUMERO = ?, MONTO = ?, BENEFICIARIO = ?, ESTADO = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			ch.getNumero(),
			ch.getMonto(),
			ch.getBeneficiario(),
			ch.getEstado().name(),
			ch.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		updateDeleteInsertSql("DELETE FROM CHEQUES WHERE ID = ?", id);
	}

	public void cobrar(Cheque ch, Movimiento m) throws SQLException {
		String[] sqls = {
			"UPDATE CUENTAS SET SALDO = ? WHERE ID = ?",
			MovimientoDao.SQL_INSERT,
			"UPDATE CHEQUES SET ESTADO = ? WHERE ID = ?"
		};
		Object[][] params = {
			{ ch.getCuenta().getSaldo(), ch.getCuenta().getId() },
			movimientoDao.parametros(m),
			{ EstadoCheque.COBRADO.name(), ch.getId() }
		};
		transaccionSql(sqls, params);
	}

	private Cheque cargarUno(String sql, Object param) throws SQLException {
		ResultSet rs = selectSql(sql, param);
		Cheque ch = null;
		Integer idCuenta = null;
		try {
			if (rs.next()) {
				ch = mapearSinCuenta(rs);
				idCuenta = rs.getInt("ID_CUENTA");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (ch != null && idCuenta != null) {
			ch.setCuenta(cuentaDao.leer(idCuenta));
		}
		return ch;
	}

	private List<Cheque> cargarVarias(String sql, Object param) throws SQLException {
		ResultSet rs = (param == null) ? selectSql(sql) : selectSql(sql, param);
		List<Cheque> lista = new ArrayList<>();
		List<Integer> idsCuenta = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinCuenta(rs));
				idsCuenta.add(rs.getInt("ID_CUENTA"));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		for (int i = 0; i < lista.size(); i++) {
			lista.get(i).setCuenta(cuentaDao.leer(idsCuenta.get(i)));
		}
		return lista;
	}

	private Cheque mapearSinCuenta(ResultSet rs) throws SQLException {
		Cheque ch = new Cheque();
		ch.setId(rs.getInt("ID"));
		ch.setNumero(rs.getString("NUMERO"));
		ch.setMonto(rs.getDouble("MONTO"));
		ch.setBeneficiario(rs.getString("BENEFICIARIO"));
		Timestamp ts = rs.getTimestamp("FECHA_EMISION");
		if (ts != null) ch.setFechaEmision(ts.toLocalDateTime());
		ch.setEstado(EstadoCheque.valueOf(rs.getString("ESTADO")));
		return ch;
	}
}
