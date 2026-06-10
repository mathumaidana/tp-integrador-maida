package persistencia;

import java.sql.SQLException;

import entidades.Movimiento;

public class TransferenciaDao extends BaseH2 {

	private static final String SQL_ACTUALIZAR_SALDO = "UPDATE CUENTAS SET SALDO = ? WHERE ID = ?";

	private final MovimientoDao movimientoDao;

	public TransferenciaDao() {
		super();
		this.movimientoDao = new MovimientoDao();
	}

	public void transferir(Integer idOrigen, Double saldoOrigen, Integer idDestino, Double saldoDestino,
			Movimiento enviado, Movimiento recibido) throws SQLException {
		String[] sqls = {
			SQL_ACTUALIZAR_SALDO,
			SQL_ACTUALIZAR_SALDO,
			MovimientoDao.SQL_INSERT,
			MovimientoDao.SQL_INSERT
		};
		Object[][] params = {
			{ saldoOrigen, idOrigen },
			{ saldoDestino, idDestino },
			movimientoDao.parametros(enviado),
			movimientoDao.parametros(recibido)
		};
		transaccionSql(sqls, params);
	}
}
