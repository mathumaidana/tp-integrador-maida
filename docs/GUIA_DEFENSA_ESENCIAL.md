# Guía esencial de defensa — Mini Home Banking

Versión condensada: solo lo 100% necesario para defender el TP. Si querés el detalle completo, está en la guía larga. Esto es para repasar la noche anterior y tener a mano el día de la defensa.

---

## 1. Qué es el proyecto (speech de arranque, ~90 segundos)

> Es un mini home banking de escritorio en **Java + Swing**, con persistencia local en **H2 vía JDBC** (sin ORM). Modela **5 entidades persistidas**: `Usuario` (con subclases `Empleado` y `Cliente`), `Cuenta` (con subclases `CajaAhorro` y `CuentaCorriente`), `Tarjeta`, `Movimiento` y `Cheque`.
>
> Está organizado en **4 capas con dependencias unidireccionales**: `vista → servicio → persistencia → entidades`.
>
> Aplico los pilares de POO: **herencia** en dos jerarquías, **polimorfismo** en cuatro puntos (dispatch de menú con `crearMenu()`, signo del movimiento, saldo mínimo por tipo de cuenta, y factory de cuentas), **encapsulamiento** de las reglas de negocio dentro de las entidades, **abstracción** con clases e interfaces abstractas, **interfaz genérica** `ICrud<T>` para el patrón DAO, y **excepciones tipadas en tres niveles** (SQL → infraestructura → dominio).
>
> Hay dos roles: el **empleado** opera el banco (da de alta clientes, abre cuentas y tarjetas, registra movimientos); el **cliente** opera lo suyo (ve saldos, transfiere, consulta movimientos). La operación más compleja es la **transferencia**, que persiste dos saldos y dos movimientos en **una única transacción** con rollback ante fallo.

---

## 2. Arquitectura en capas

```
vista  (Swing: formularios, menús, login)
  │  depende de
  ▼
servicio  (reglas de negocio, validaciones, traduce excepciones)
  │  depende de
  ▼
persistencia  (DAOs, JDBC, H2) ──► entidades
  │                                    ▲
  └────────────────────────────────────┘
entidades  (modelo de dominio + reglas que le son propias)
```

- **entidades**: el modelo. Saben sus propias reglas (`Cuenta.debitar` valida el saldo mínimo). No conocen la BD ni Swing (salvo `Usuario.crearMenu()`, un trade-off consciente que se explica abajo).
- **persistencia**: los DAOs. Hablan SQL, devuelven entidades. No tienen reglas de negocio.
- **servicio**: orquesta. Valida, llama al DAO, y **traduce** `SQLException` (técnica) a excepciones de dominio que la vista entiende.
- **vista**: Swing puro. Captura excepciones de dominio y las muestra con `JOptionPane`. Nunca ve un `SQLException` crudo.

**Por qué importa:** cada capa tiene una sola responsabilidad. Si te preguntan "¿dónde pondrías X?", la respuesta sale de esta separación.

---

## 3. Los pilares de POO y dónde están

| Pilar | Dónde | Cómo |
| --- | --- | --- |
| **Encapsulamiento** | `Cuenta.debitar/acreditar`, `Usuario.autenticaCon` | campos `private`/`protected`; las reglas viven dentro del objeto |
| **Herencia** | `Usuario→Empleado/Cliente`, `Cuenta→CajaAhorro/CuentaCorriente` | atributos comunes en el padre, comportamiento propio en el hijo |
| **Polimorfismo** | `usuario.crearMenu()`, `cuenta.saldoMinimo()`, `tipo.signo()`, `tipo.nuevaCuenta()` | misma llamada, distinto comportamiento según el tipo real |
| **Abstracción** | `abstract Usuario`, `abstract Cuenta`, `abstract MenuView` | clases que no se instancian; definen contrato |
| **Interfaz + genéricos** | `ICrud<T>` | contrato puro de CRUD, reusable para cualquier entidad |
| **Excepciones** | 3 niveles: `SQLException` → `GrabandoException` → dominio | cada capa lanza/traduce lo que le corresponde |

---

## 4. Los 8 fragmentos de código que tenés que saber explicar

### 4.1 `Cuenta.debitar()` — encapsulamiento + polimorfismo + excepciones

```java
// src/entidades/Cuenta.java
public abstract class Cuenta {
    protected Double saldo;
    protected final TipoCuenta tipo;   // ← final: el tipo es identidad, no cambia

    public abstract double saldoMinimo();        // ← cada subclase lo define

    public void debitar(Double monto) throws SaldoInsuficienteException {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
        }
        if (saldo - monto < saldoMinimo()) {     // ← llamada POLIMÓRFICA
            throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + referencia());
        }
        this.saldo -= monto;
    }
}
```

**Qué decir:** `debitar` no sabe de qué tipo de cuenta es. Llama a `saldoMinimo()` y el JVM ejecuta el override correcto en runtime: `CajaAhorro` devuelve `0.0`, `CuentaCorriente` devuelve `-50000.0`. La regla de "no podés debitar por debajo del mínimo" vive **dentro** de la entidad (encapsulamiento), no en el servicio ni en la vista.

- `IllegalArgumentException` (unchecked): error del programador, monto inválido. No es recuperable.
- `SaldoInsuficienteException` (checked): error de negocio esperable. El caller **debe** manejarlo.

---

### 4.2 `CajaAhorro` y `CuentaCorriente` — herencia + polimorfismo

```java
// src/entidades/CajaAhorro.java
public class CajaAhorro extends Cuenta {
    public CajaAhorro(TipoCuenta tipo) { super(tipo); }
    @Override public double saldoMinimo()    { return 0.0; }      // no puede quedar en rojo
    @Override public boolean permiteCheques() { return false; }
}

// src/entidades/CuentaCorriente.java
public class CuentaCorriente extends Cuenta {
    private static final double ACUERDO_POR_DEFECTO = 50000.0;
    private final double acuerdoDescubierto;

    public CuentaCorriente(TipoCuenta tipo) { this(tipo, ACUERDO_POR_DEFECTO); }
    public CuentaCorriente(TipoCuenta tipo, double acuerdoDescubierto) {
        super(tipo);
        this.acuerdoDescubierto = acuerdoDescubierto;
    }
    @Override public double saldoMinimo()    { return -acuerdoDescubierto; }  // puede ir en rojo
    @Override public boolean permiteCheques() { return true; }
}
```

**Qué decir:** las dos comparten todo lo de `Cuenta` (saldo, alias, cbu, debitar, acreditar) y solo difieren en dos métodos. La caja de ahorro no puede quedar negativa; la cuenta corriente sí, hasta su acuerdo de descubierto. Eso es **herencia** (reuso) + **polimorfismo** (cada una redefine `saldoMinimo`).

---

### 4.3 `usuario.crearMenu()` — polimorfismo de dispatch (el ejemplo más claro)

```java
// src/entidades/Usuario.java
public abstract MenuView crearMenu();

// src/entidades/Empleado.java
@Override public MenuView crearMenu() { return new MenuEmpleadoView(this); }

// src/entidades/Cliente.java
@Override public MenuView crearMenu() { return new MenuClienteView(this); }

// src/vista/LoginView.java
Usuario usuario = autenticacionServicio.autenticar(username, password);
MenuView menu = usuario.crearMenu();   // ← SIN if, SIN instanceof
frame.dispose();
menu.alCerrar(LoginView::new);
menu.mostrar();
```

**Qué decir:** `LoginView` recibe un `Usuario` y no sabe si es `Empleado` o `Cliente`. Llama `crearMenu()` y el JVM resuelve el override correcto. **Esto es exactamente lo que el polimorfismo reemplaza al `instanceof`.** Si mañana agrego un rol `Auditor`, solo creo `Auditor extends Usuario` con su `crearMenu()` — `LoginView` no cambia (Open/Closed).

**El trade-off (decílo vos antes de que pregunten):** `Empleado` y `Cliente` importan clases de `vista`, así que `entidades` depende de `vista`, rompiendo la regla de capas. Lo elegí porque en un TP cuyo objetivo es demostrar POO, el polimorfismo puro pesa más que la pureza de capas. La alternativa "limpia" sería una interfaz `MenuFactory` en una capa intermedia, pero agrega complejidad innecesaria para esta escala.

---

### 4.4 `TipoMovimiento.signo()` — polimorfismo en enum + Open/Closed

```java
// src/entidades/TipoMovimiento.java
public enum TipoMovimiento {
    TRANSFERENCIA_ENVIADA  { @Override public int signo() { return -1; } },
    TRANSFERENCIA_RECIBIDA { @Override public int signo() { return  1; } },
    DEBITO_TARJETA         { @Override public int signo() { return -1; } },
    PAGO_TARJETA           { @Override public int signo() { return  1; } };

    public abstract int signo();
}

// uso en ResumenView — sin switch:
total += m.getTipo().signo() * m.getMonto();
```

**Qué decir:** cada valor del enum sabe si suma o resta al balance. Antes había un `switch` en la vista; si agregaba un tipo nuevo y me olvidaba de actualizar el switch, el cálculo quedaba mal en silencio. Ahora `signo()` es **abstracto**: el compilador **obliga** a implementarlo en cada valor nuevo. Eso es **Open/Closed** garantizado por construcción.

---

### 4.5 `TipoCuenta.nuevaCuenta()` — Factory Method en enum

```java
// src/entidades/TipoCuenta.java
public enum TipoCuenta {
    CAJA_AHORRO_PESOS(Moneda.PESOS)     { public Cuenta nuevaCuenta() { return new CajaAhorro(this); } },
    CAJA_AHORRO_DOLARES(Moneda.DOLARES) { public Cuenta nuevaCuenta() { return new CajaAhorro(this); } },
    CUENTA_CORRIENTE(Moneda.PESOS)      { public Cuenta nuevaCuenta() { return new CuentaCorriente(this); } };

    public abstract Cuenta nuevaCuenta();
}

// uso en CuentaDao — sin switch:
Cuenta c = tipo.nuevaCuenta();   // construye la subclase correcta sola
```

**Qué decir:** es el patrón **Factory Method**, pero el factory vive dentro del enum. Cuando el DAO lee una cuenta de la BD, hace `tipo.nuevaCuenta()` y obtiene la subclase correcta sin un `switch(tipo)`. Agregar un tipo nuevo = agregar un valor al enum; el DAO no se toca.

---

### 4.6 `ICrud<T>` — interfaz genérica + DIP + overloading

```java
// src/persistencia/ICrud.java
public interface ICrud<T> {
    void grabar(T t) throws SQLException;
    T leer(Integer id) throws SQLException;      // overload: con id
    List<T> leer() throws SQLException;          // overload: todos
    void modificar(T t) throws SQLException;
    void borrar(Integer id) throws SQLException;
}

// implementación: hereda infraestructura + cumple contrato
public class EmpleadoDao extends BaseH2 implements ICrud<Empleado> { ... }

// el servicio depende de la INTERFAZ, no del DAO concreto:
public class EmpleadoServicio {
    private final ICrud<Empleado> empleadoDao;   // ← abstracción, no implementación
}
```

**Qué decir:**
- **Genéricos**: un solo contrato sirve para `Empleado`, `Cliente`, etc. `ICrud<Empleado>` garantiza que `leer()` devuelve `List<Empleado>`, no `List<Object>`.
- **Overloading**: dos métodos `leer` con distinta firma (uno con `Integer id`, otro sin). El compilador los distingue por parámetros (polimorfismo estático).
- **DIP**: `EmpleadoServicio` no conoce `EmpleadoDao`, conoce `ICrud<Empleado>`. Puedo inyectarle un mock para testear sin BD.
- `EmpleadoDao extends BaseH2 implements ICrud<Empleado>`: herencia simple (1 padre) + implementación de interfaz a la vez.

---

### 4.7 `BaseH2.transaccionSql()` + transferencia — atomicidad (el más importante)

```java
// src/persistencia/BaseH2.java
protected final void transaccionSql(String[] sqls, Object[][] params) throws SQLException {
    cargarDriver();
    obtenerConexion();
    try {
        connection.setAutoCommit(false);          // ← abre transacción
        for (int i = 0; i < sqls.length; i++) {
            PreparedStatement s = preparedStatement_v20(sqls[i], params[i]);
            s.executeUpdate();
            s.close();
        }
        connection.commit();                       // ← todo o nada
    } catch (SQLException e) {
        connection.rollback();                     // ← si algo falla, revierte TODO
        throw e;
    } finally {
        cerrarConexion();                          // ← la conexión SIEMPRE se cierra
    }
}
```

```java
// src/persistencia/TransferenciaDao.java — 4 statements en 1 transacción
public void transferir(...) throws SQLException {
    String[] sqls = { SQL_ACTUALIZAR_SALDO, SQL_ACTUALIZAR_SALDO,
                      MovimientoDao.SQL_INSERT, MovimientoDao.SQL_INSERT };
    Object[][] params = { {saldoOrigen, idOrigen}, {saldoDestino, idDestino},
                          movimientoDao.parametros(enviado), movimientoDao.parametros(recibido) };
    transaccionSql(sqls, params);
}
```

**Qué decir:** una transferencia toca 4 filas (2 saldos + 2 movimientos). Con `setAutoCommit(false)` + `commit()`, o se aplican las 4 o ninguna. Si falla la tercera, `rollback()` deja la BD como estaba: **nunca** se debita el origen sin acreditar el destino. La misma `transaccionSql` se reusa para débitos de tarjeta y borrados en cascada.

---

### 4.8 `UsuarioDao.mapear()` — herencia de tabla única + Template Method

```java
// src/persistencia/UsuarioDao.java — el ROL decide la subclase
private Usuario mapear(ResultSet rs) throws SQLException {
    ...
    String rol = rs.getString("ROL");
    if ("EMPLEADO".equals(rol)) return new Empleado(id, username, password, nombre, apellido, dni);
    if ("CLIENTE".equals(rol))  return new Cliente(id, username, password, nombre, apellido, dni);
    throw new SQLException("Rol desconocido en USUARIOS: " + rol);
}
```

```java
// src/vista/MenuView.java — Template Method
protected MenuView(Usuario usuario) {        // el constructor es el "algoritmo" fijo
    this.frame = new JFrame(tituloVentana());           // ← hook abstracto
    root.add(crearPanelOpciones(), BorderLayout.CENTER); // ← hook abstracto
    ...
}
protected abstract String tituloVentana();      // cada subclase rellena los huecos
protected abstract JPanel crearPanelOpciones();
```

**Qué decir (mapear):** `Empleado` y `Cliente` viven en **una sola tabla** `USUARIOS`, discriminadas por la columna `ROL` (*single-table inheritance*). `mapear` es el único lugar donde se decide el tipo concreto; de ahí en adelante el polimorfismo (`crearMenu()`) hace el resto. `"EMPLEADO".equals(rol)` y no `rol.equals(...)` para ser null-safe.

**Qué decir (Template Method):** el constructor de `MenuView` define el orden de armado (esqueleto fijo) y delega los pasos variables (`tituloVentana`, `crearPanelOpciones`) a las subclases. Mismo patrón en `FormularioUsuario`.

---

## 5. El flujo de una transferencia (end-to-end)

El cliente completa origen, destino y monto, y aprieta "Transferir":

1. **`FormularioTransferencia`** llama a `transferenciaServicio.transferir(origen, destino, monto)`.
2. **`TransferenciaServicio`**:
   - Valida que las cuentas sean **distintas** (`esLaMismaQue`) y de la **misma moneda** (`mismaMonedaQue`) → si no, `TransferenciaInvalidaException`.
   - Llama `origen.debitar(monto)` → si no alcanza, `SaldoInsuficienteException` (la entidad valida).
   - Llama `destino.acreditar(monto)`.
   - Arma los dos `Movimiento` (ENVIADA / RECIBIDA) y llama `transferenciaDao.transferir(...)`.
3. **`TransferenciaDao`** persiste los 2 saldos + 2 movimientos en **una transacción** (4.7).
4. Si la transacción falla, el servicio **restaura los saldos en memoria** (los objetos ya estaban modificados) y re-lanza la excepción.
5. La vista captura la excepción de dominio y muestra el mensaje con `JOptionPane`.

**Por qué es defendible:** la validación de negocio (misma moneda, saldo) está en el servicio y la entidad; la atomicidad está en el DAO. Cada capa hace lo suyo.

---

## 6. Preguntas que casi seguro te hacen (y la respuesta corta)

**¿Cómo aplicaste polimorfismo?**
En cuatro puntos: `usuario.crearMenu()` (dispatch de menú sin instanceof), `cuenta.saldoMinimo()` (cada tipo de cuenta su mínimo), `tipo.signo()` (cada movimiento su signo), `tipo.nuevaCuenta()` (cada tipo construye su subclase). En todos, la misma llamada ejecuta código distinto según el tipo real, resuelto en runtime.

**¿Por qué `Usuario` y `Cuenta` son abstractas?**
Porque nunca se instancian solas: siempre es un `Empleado`/`Cliente` o una `CajaAhorro`/`CuentaCorriente`. `abstract` lo documenta y lo fuerza en compilación (`new Usuario(...)` no compila). Además tienen métodos abstractos (`crearMenu`, `saldoMinimo`) que las subclases **deben** implementar.

**Diferencia entre clase abstracta e interfaz, ¿cuándo usaste cada una?**
Clase abstracta cuando hay **estado y comportamiento compartido**: `Usuario` (campos id, username...), `Cuenta` (saldo, debitar...). Interfaz cuando es un **contrato puro sin estado**: `ICrud<T>`. Además, una clase hereda de UNA sola superclase pero implementa N interfaces — por eso `EmpleadoDao extends BaseH2 implements ICrud` puede tener las dos cosas.

**¿Por qué `LoginView` no usa `instanceof`?**
Porque `instanceof` es el antipatrón que el polimorfismo elimina. Con `usuario.crearMenu()` cada clase sabe qué menú abrir; agregar un rol no toca `LoginView`. El `instanceof` me obligaría a modificar `LoginView` por cada rol nuevo.

**¿Dónde aplicaste el Principio de Inversión de Dependencias (DIP)?**
`EmpleadoServicio` depende de `ICrud<Empleado>` (abstracción), no de `EmpleadoDao` (implementación). Y `LoginFrame` depende de callbacks (`BiConsumer`, `Runnable`), no de `LoginView`. En ambos, el módulo de alto nivel depende de una abstracción.

**¿Herencia o composición? ¿Cuándo cada una?**
Herencia donde hay "es un": `Empleado` **es un** `Usuario`, `CajaAhorro` **es una** `Cuenta`. Composición donde hay "tiene un": `Cuenta` **tiene un** `Cliente` titular, `Movimiento` **tiene una** `Cuenta`. Regla: comparten identidad y comportamiento → herencia; uno contiene al otro → composición.

**¿Qué pasa si falla una transferencia a la mitad?**
Nada se persiste. Como uso `setAutoCommit(false)` y solo confirmo con `commit()` al final, si cualquiera de los 4 statements falla, `rollback()` revierte todo. Los saldos en memoria los restaura el servicio. Atomicidad total.

**¿Por qué excepciones checked y tres niveles?**
Tres niveles: (1) `SQLException` técnica del JDBC; (2) el servicio la traduce a infraestructura (`GrabandoException`, `LeyendoException`); (3) excepciones de dominio (`SaldoInsuficienteException`, `TransferenciaInvalidaException`). Son **checked** (extienden `Exception`, no `RuntimeException`) para que el compilador obligue a manejarlas — un error de negocio no se puede ignorar por descuido.

**¿Por qué una sola tabla `USUARIOS` con columna `ROL`?**
Es *single-table inheritance*. `Empleado` y `Cliente` comparten los 6 campos y no agregan ninguno propio, así que una tabla con discriminador es más simple que dos tablas idénticas. El DAO lee `ROL` e instancia la subclase correcta.

**¿Cómo agregás un tipo de cuenta / movimiento / rol nuevo? (Open/Closed)**
- Cuenta: nueva subclase de `Cuenta` + valor en `TipoCuenta`. El DAO se mapea solo con `nuevaCuenta()`.
- Movimiento: nuevo valor en `TipoMovimiento`; el compilador me obliga a darle `signo()`.
- Rol: nueva subclase de `Usuario` con su `crearMenu()`; `LoginView` no cambia.
En los tres, agrego código nuevo sin modificar el existente.

**¿Qué es Template Method y dónde lo usás?**
Un método que define el esqueleto fijo de un algoritmo y delega pasos a las subclases. En `MenuView` y `FormularioUsuario`: el constructor arma la ventana en orden fijo y llama a hooks abstractos (`crearPanelOpciones`, `tituloVentana`) que cada subclase rellena.

**¿Por qué `Cliente` no tiene una colección de sus cuentas/tarjetas, y `Cuenta` de sus movimientos?**
Porque es una arquitectura DAO sin ORM, y ahí la navegación top-down idiomática es por query: `cuentaDao.leerPorTitular(idCliente)`, `movimientoDao.leerPorCuenta(cuenta)` — el equivalente manual a un repository method / `JOIN FETCH`. La relación es unidireccional hijo→padre (`Cuenta` conoce su `Cliente`) porque esa dirección la necesito **siempre** (para mapear desde la BD y mostrar el titular); la inversa la necesito **a veces** y la resuelvo con una query puntual. Si `Cliente` cacheara `List<Cuenta>`, sin Unit of Work esa lista quedaría **rancia** apenas se crea una cuenta, y crearía **referencia circular** con `Cuenta.titular` (riesgo de recursión en `toString`/`equals`). Además ya evité el N+1: `leerPorTitular` hace 2 queries fijas, no una por cuenta. Las colecciones las pongo donde el dato es fresco y acotado: el servicio devuelve `List<Cuenta>` cuando la vista lo pide.

**¿Dónde se usa `permiteCheques()`? (feature de cheques)**
En `ChequeServicio.emitir()`: `if (!ch.getCuenta().permiteCheques()) throw new OperacionNoPermitidaException(...)`. Es despacho polimórfico — `CuentaCorriente` devuelve `true`, `CajaAhorro` `false`, así que solo una cuenta corriente puede emitir cheques. El flujo es de dos pasos: **emitir** crea el cheque PENDIENTE (acá vive la validación de `permiteCheques()`), y **cobrar** ejecuta `cuenta.debitar(monto)` (que reusa `saldoMinimo()` polimórfico) e inserta el movimiento `CHEQUE_COBRADO`, todo en una transacción atómica (`transaccionSql`). Anular pasa el cheque PENDIENTE → ANULADO.

**¿Qué cambiarías o qué no quedó perfecto? (la pregunta honesta)**
`EmpleadoDao` y `ClienteDao` comparten ~85% del SQL (solo cambia el rol). Lo refactorizaría a un `UsuarioDaoBase<T extends Usuario>` con el SQL parametrizado (lo tengo identificado). No lo hice por tiempo, no por no verlo. También: passwords en texto plano (faltaría hashear con BCrypt) y un N+1 al listar cuentas con su titular (se resuelve con un JOIN). Ninguno afecta la demo.

---

## 7. Datos sueltos por si preguntan

- **Compilar:** `javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $(find src -name "*.java")`
- **Ejecutar:** `java -cp "bin:lib/*" app.Main`
- **BD:** H2 embebida, archivo en `./data/banco`. `InicializadorBD` crea las tablas con `CREATE TABLE IF NOT EXISTS` y siembra `admin/admin` si no hay empleados (idempotente).
- **`==` vs `.equals()` en enums:** uso `==` para comparar `Moneda` porque cada constante de enum es una única instancia en la JVM (singleton). `==` es idiomático y más eficiente.
- **`final` en `Cuenta.tipo`:** el tipo es la identidad de la cuenta y no cambia; `final` lo garantiza en compilación (no hay setter).
- **`protected` vs `private`:** campos de `Usuario` son `private` (las subclases usan getters); campos de `Cuenta` son `protected` (las subclases podrían necesitarlos). Mínimo privilegio en cada caso.

---

## 8. Novedades: qué se agregó al final (y qué decidí NO hacer)

Esta sección resume los dos últimos cambios. Si el docente pregunta "¿qué hiciste recién?" o "¿por qué no hiciste X?", esto es lo que tenés que poder contar.

### 8.1 Feature nueva: Cheques

**Por qué la agregué:** el método polimórfico `permiteCheques()` (abstracto en `Cuenta`, `true` en `CuentaCorriente`, `false` en `CajaAhorro`) estaba declarado pero **sin usar**. Los cheques le dan un uso real y de paso demuestran varios conceptos juntos.

**Qué hace:** un cheque se **emite** desde una cuenta y luego se **cobra** (dos pasos):

```java
// servicio/ChequeServicio.java — el corazón de la feature
public void emitir(Cheque ch) throws OperacionNoPermitidaException, ... {
    if (!ch.getCuenta().permiteCheques())            // ← despacho polimórfico
        throw new OperacionNoPermitidaException(
            "Solo las cuentas corrientes pueden emitir cheques.");
    if (ch.getMonto() == null || ch.getMonto() <= 0) // ← validación de datos
        throw new DatosInvalidosException("El monto debe ser mayor a cero.");
    // ... número único, graba el cheque PENDIENTE (NO toca saldo)
}

public void cobrar(Cheque ch) throws SaldoInsuficienteException, ... {
    cuenta.debitar(ch.getMonto());                   // ← reusa saldoMinimo() polimórfico
    chequeDao.cobrar(ch, movimientoChequeCobrado);   // ← transacción atómica
    ch.setEstado(EstadoCheque.COBRADO);              // ← solo si la persistencia tuvo éxito
}
```

**Qué demuestra (puntos para la defensa):**
- **Polimorfismo dos veces:** `permiteCheques()` al emitir (cuenta corriente sí, caja de ahorro no) y `saldoMinimo()` vía `debitar()` al cobrar (respeta el acuerdo de descubierto de la cuenta corriente).
- **Open/Closed:** agregué un valor nuevo al enum `TipoMovimiento` (`CHEQUE_COBRADO`) y el compilador me obligó a darle su `signo()` (-1). `ResumenView` no se tocó.
- **Atomicidad:** `cobrar` actualiza saldo + inserta movimiento + marca el cheque, todo en una `transaccionSql` (o las tres o ninguna). Mismo patrón que la transferencia.
- **Mismas capas:** entidad `Cheque` + enum `EstadoCheque`, `ChequeDao`, `ChequeServicio`, `FormularioCheque`. Tabla `CHEQUES` con FK a `CUENTAS`. Botón "Cheques" en el menú del cliente y del empleado.
- **Integridad referencial:** como `CHEQUES` referencia a `CUENTAS`, actualicé `borrarEnCascada` (cliente y cuenta) para que borre primero los cheques. Si no, borrar una cuenta con cheques violaría la FK.

**Estados del cheque** (`EstadoCheque`): `PENDIENTE → COBRADO` o `PENDIENTE → ANULADO`. No se puede cobrar dos veces ni cobrar uno anulado (lo valida `estaPendiente()`).

### 8.2 Decisión: por qué NO puse colecciones en `Cliente`/`Cuenta`

**La idea que evalué:** que `Cliente` tuviera `List<Cuenta>`/`List<Tarjeta>` y `Cuenta` tuviera `List<Movimiento>`, para navegar "de arriba hacia abajo" (`cliente.getCuentas()`) en vez de pedírselo al DAO.

**Por qué decidí NO hacerlo** (y esto es lo importante de poder defender):
- Es una arquitectura **DAO sin ORM**. Ahí la navegación top-down idiomática es **por query**: `cuentaDao.leerPorTitular(idCliente)`, `movimientoDao.leerPorCuenta(cuenta)`. Es el equivalente manual a un repository method / `JOIN FETCH`.
- Sin un Unit of Work (sesión de persistencia tipo JPA), una `List<Cuenta>` cacheada en `Cliente` quedaría **rancia** apenas se crea o borra una cuenta.
- Crearía **referencia circular** con `Cuenta.titular` (que sí necesito y no puedo quitar), con riesgo de recursión infinita en `toString`/`equals`.
- Habría **dos formas** de obtener lo mismo (el DAO y la colección), difícil de mantener consistente.
- El N+1 que motivaría cachear **ya está resuelto**: `leerPorTitular` hace 2 queries fijas, no una por cuenta.

**La frase clave:** "lo que demuestra madurez de diseño no es agregar las colecciones, sino entender por qué en esta arquitectura NO conviene". La relación la dejo unidireccional hijo→padre (`Cuenta` conoce su `Cliente`) porque esa dirección la necesito siempre; la inversa la resuelvo con una query cuando la vista la pide.
