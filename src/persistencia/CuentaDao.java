package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.TipoCuenta;

public class CuentaDao extends BaseH2 implements ICrud<Cuenta> {

	private static final String COLS = "ID, ALIAS, CBU, SALDO, TIPO, ID_TITULAR";

	private final ClienteDao clienteDao;

	public CuentaDao() {
		super();
		this.clienteDao = new ClienteDao();
	}

	@Override
	public void grabar(Cuenta c) throws SQLException {
		String sql = "INSERT INTO CUENTAS (ALIAS, CBU, SALDO, TIPO, ID_TITULAR) VALUES (?, ?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			c.getAlias(),
			c.getCbu(),
			c.getSaldo(),
			c.getTipo().name(),
			c.getTitular().getId()
		);
	}

	@Override
	public Cuenta leer(Integer id) throws SQLException {
		return cargarUna("SELECT " + COLS + " FROM CUENTAS WHERE ID = ?", id);
	}

	public Cuenta buscarPorCbu(String cbu) throws SQLException {
		return cargarUna("SELECT " + COLS + " FROM CUENTAS WHERE CBU = ?", cbu);
	}

	public Cuenta buscarPorAlias(String alias) throws SQLException {
		return cargarUna("SELECT " + COLS + " FROM CUENTAS WHERE ALIAS = ?", alias);
	}

	@Override
	public List<Cuenta> leer() throws SQLException {
		return cargarVarias("SELECT " + COLS + " FROM CUENTAS ORDER BY ID");
	}

	public List<Cuenta> leerPorTitular(Integer idTitular) throws SQLException {
		String sql = "SELECT " + COLS + " FROM CUENTAS WHERE ID_TITULAR = ? ORDER BY ID";
		ResultSet rs = selectSql(sql, idTitular);
		List<Cuenta> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinTitular(rs));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		Cliente titular = clienteDao.leer(idTitular);
		for (Cuenta c : lista) {
			c.setTitular(titular);
		}
		return lista;
	}

	@Override
	public void modificar(Cuenta c) throws SQLException {
		String sql = "UPDATE CUENTAS SET ALIAS = ?, CBU = ?, ID_TITULAR = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			c.getAlias(),
			c.getCbu(),
			c.getTitular().getId(),
			c.getId()
		);
	}

	public void actualizarSaldo(Integer idCuenta, Double saldo) throws SQLException {
		updateDeleteInsertSql("UPDATE CUENTAS SET SALDO = ? WHERE ID = ?", saldo, idCuenta);
	}

	public void borrarEnCascada(Integer id) throws SQLException {
		String[] sqls = {
			"DELETE FROM MOVIMIENTOS WHERE ID_CUENTA = ?",
			"DELETE FROM CUENTAS WHERE ID = ?"
		};
		Object[][] params = { { id }, { id } };
		transaccionSql(sqls, params);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		updateDeleteInsertSql("DELETE FROM CUENTAS WHERE ID = ?", id);
	}

	private Cuenta cargarUna(String sql, Object param) throws SQLException {
		ResultSet rs = selectSql(sql, param);
		Cuenta cuenta = null;
		Integer idTitular = null;
		try {
			if (rs.next()) {
				cuenta = mapearSinTitular(rs);
				idTitular = rs.getInt("ID_TITULAR");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (cuenta != null && idTitular != null) {
			cuenta.setTitular(clienteDao.leer(idTitular));
		}
		return cuenta;
	}

	private List<Cuenta> cargarVarias(String sql) throws SQLException {
		ResultSet rs = selectSql(sql);
		List<Cuenta> lista = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinTitular(rs));
				ids.add(rs.getInt("ID_TITULAR"));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		for (int i = 0; i < lista.size(); i++) {
			lista.get(i).setTitular(clienteDao.leer(ids.get(i)));
		}
		return lista;
	}

	private Cuenta mapearSinTitular(ResultSet rs) throws SQLException {
		TipoCuenta tipo = TipoCuenta.valueOf(rs.getString("TIPO"));
		Cuenta c = tipo.nuevaCuenta();
		c.setId(rs.getInt("ID"));
		c.setAlias(rs.getString("ALIAS"));
		c.setCbu(rs.getString("CBU"));
		c.setSaldo(rs.getDouble("SALDO"));
		return c;
	}
}
