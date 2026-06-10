package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entidades.Cliente;
import entidades.Movimiento;
import entidades.Tarjeta;

public class TarjetaDao extends BaseH2 implements ICrud<Tarjeta> {

	private final ClienteDao clienteDao;
	private final MovimientoDao movimientoDao;

	public TarjetaDao() {
		super();
		this.clienteDao = new ClienteDao();
		this.movimientoDao = new MovimientoDao();
	}

	@Override
	public void grabar(Tarjeta t) throws SQLException {
		String sql = "INSERT INTO TARJETAS (NUMERO, ID_TITULAR, DISPONIBLE, SALDO_A_PAGAR) VALUES (?, ?, ?, ?)";
		updateDeleteInsertSql(sql,
			t.getNumero(),
			t.getTitular().getId(),
			t.getDisponible(),
			t.getSaldoAPagar()
		);
	}

	public Tarjeta buscarPorNumero(String numero) throws SQLException {
		String sql = "SELECT ID, NUMERO, ID_TITULAR, DISPONIBLE, SALDO_A_PAGAR FROM TARJETAS WHERE NUMERO = ?";
		ResultSet rs = selectSql(sql, numero);
		Tarjeta t = null;
		Integer idTitular = null;
		try {
			if (rs.next()) {
				t = mapearSinTitular(rs);
				idTitular = rs.getInt("ID_TITULAR");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (t != null && idTitular != null) {
			t.setTitular(clienteDao.leer(idTitular));
		}
		return t;
	}

	@Override
	public Tarjeta leer(Integer id) throws SQLException {
		String sql = "SELECT ID, NUMERO, ID_TITULAR, DISPONIBLE, SALDO_A_PAGAR FROM TARJETAS WHERE ID = ?";
		ResultSet rs = selectSql(sql, id);
		Tarjeta t = null;
		Integer idTitular = null;
		try {
			if (rs.next()) {
				t = mapearSinTitular(rs);
				idTitular = rs.getInt("ID_TITULAR");
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		if (t != null && idTitular != null) {
			t.setTitular(clienteDao.leer(idTitular));
		}
		return t;
	}

	@Override
	public List<Tarjeta> leer() throws SQLException {
		String sql = "SELECT ID, NUMERO, ID_TITULAR, DISPONIBLE, SALDO_A_PAGAR FROM TARJETAS ORDER BY ID";
		ResultSet rs = selectSql(sql);
		List<Tarjeta> lista = new ArrayList<>();
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

	public List<Tarjeta> leerPorTitular(Integer idTitular) throws SQLException {
		String sql = "SELECT ID, NUMERO, ID_TITULAR, DISPONIBLE, SALDO_A_PAGAR FROM TARJETAS WHERE ID_TITULAR = ? ORDER BY ID";
		ResultSet rs = selectSql(sql, idTitular);
		List<Tarjeta> lista = new ArrayList<>();
		try {
			while (rs.next()) {
				lista.add(mapearSinTitular(rs));
			}
		} finally {
			if (rs != null) rs.close();
			cerrarConexion();
		}
		Cliente titular = clienteDao.leer(idTitular);
		for (Tarjeta t : lista) {
			t.setTitular(titular);
		}
		return lista;
	}

	@Override
	public void modificar(Tarjeta t) throws SQLException {
		String sql = "UPDATE TARJETAS SET NUMERO = ?, ID_TITULAR = ? WHERE ID = ?";
		updateDeleteInsertSql(sql,
			t.getNumero(),
			t.getTitular().getId(),
			t.getId()
		);
	}

	public void registrarOperacion(Tarjeta t, Movimiento m) throws SQLException {
		String[] sqls = {
			"UPDATE TARJETAS SET DISPONIBLE = ?, SALDO_A_PAGAR = ? WHERE ID = ?",
			MovimientoDao.SQL_INSERT
		};
		Object[][] params = {
			{ t.getDisponible(), t.getSaldoAPagar(), t.getId() },
			movimientoDao.parametros(m)
		};
		transaccionSql(sqls, params);
	}

	@Override
	public void borrar(Integer id) throws SQLException {
		String sql = "DELETE FROM TARJETAS WHERE ID = ?";
		updateDeleteInsertSql(sql, id);
	}

	private Tarjeta mapearSinTitular(ResultSet rs) throws SQLException {
		Tarjeta t = new Tarjeta();
		t.setId(rs.getInt("ID"));
		t.setNumero(rs.getString("NUMERO"));
		t.setDisponible(rs.getDouble("DISPONIBLE"));
		t.setSaldoAPagar(rs.getDouble("SALDO_A_PAGAR"));
		return t;
	}
}
