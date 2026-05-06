package persistencia;

import java.sql.SQLException;
import java.util.List;

public interface ICrud<T> {
	void grabar(T t) throws SQLException;
	T leer(Integer id) throws SQLException;
	List<T> leer() throws SQLException;
	void modificar(T t) throws SQLException;
	void borrar(Integer id) throws SQLException;
}
