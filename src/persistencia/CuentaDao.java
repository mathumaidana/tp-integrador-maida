package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Cliente;
import entidades.Cuenta;
import entidades.TipoCuenta;

public class CuentaDao extends BaseH2 implements ICrud<Cuenta> {

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
		String sql = "SELECT ID, ALIAS, CBU, SALDO, TIPO, ID_TITULAR FROM CUENTAS WHERE ID = ?";
		ResultSet rs = selectSql(sql, id);
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
			Cliente titular = clienteDao.leer(idTitular);
			cuenta.setTitular(titular);
		}
		return cuenta;
	}

	@Override
	public List<Cuenta> leer() throws SQLException {
		String sql = "SELECT ID, ALIAS, CBU, SALDO, TIPO, ID_TITULAR FROM CUENTAS ORDER BY ID";
		ResultSet rs = selectSql(sql);
		List<Cuenta> lista = new ArrayList<>();
		List<Integer> idsTitulares = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinTitular(rs));
				idsTitulares.add(rs.getInt("ID_TITULAR"));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		for (int i = 0; i < lista.size(); i++) {
			lista.get(i).setTitular(clienteDao.leer(idsTitulares.get(i)));
		}
		return lista;
	}

	public List<Cuenta> leerPorTitular(Integer idTitular) throws SQLException {
		String sql = "SELECT ID, ALIAS, CBU, SALDO, TIPO, ID_TITULAR FROM CUENTAS WHERE ID_TITULAR = ? ORDER BY ID";
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
		String sql = "UPDATE CUENTAS SET ALIAS = ?, CBU = ?, SALDO = ?, TIPO = ?, ID_TITULAR = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			c.getAlias(),
			c.getCbu(),
			c.getSaldo(),
			c.getTipo().name(),
			c.getTitular().getId(),
			c.getId()
		);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM CUENTAS WHERE ID = ?";
		updateDeleteInsertSql(sql, id);
	}

	private Cuenta mapearSinTitular(ResultSet rs) throws SQLException {
		Cuenta c = new Cuenta();
		c.setId(rs.getInt("ID"));
		c.setAlias(rs.getString("ALIAS"));
		c.setCbu(rs.getString("CBU"));
		c.setSaldo(rs.getDouble("SALDO"));
		c.setTipo(TipoCuenta.valueOf(rs.getString("TIPO")));
		return c;
	}
}
