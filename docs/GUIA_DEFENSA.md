# Guía de defensa — Mini Home Banking

Documento personal para estudiar el proyecto a fondo antes de defenderlo. Sin filtros: explica el qué, el cómo y sobre todo el **por qué** de cada decisión.

> Si vas con poco tiempo: leé las secciones [2](#2-vista-de-pájaro), [4](#4-poo-en-este-proyecto), [7](#7-decisiones-defendibles--por-qué-no-lo-hice-distinto) y [9](#9-qa-típico-de-defensa).

---

## Índice

0. [Cómo usar esta guía](#0-cómo-usar-esta-guía)
1. [Contexto del TP](#1-contexto-del-tp)
2. [Vista de pájaro](#2-vista-de-pájaro)
3. [Arquitectura en capas](#3-arquitectura-en-capas)
4. [POO en este proyecto](#4-poo-en-este-proyecto)
5. [Flujo end-to-end](#5-flujo-end-to-end)
6. [Recorrido archivo por archivo](#6-recorrido-archivo-por-archivo)
7. [Decisiones defendibles — por qué no lo hice distinto](#7-decisiones-defendibles--por-qué-no-lo-hice-distinto)
8. [Posibles extensiones](#8-posibles-extensiones)
9. [Q&A típico de defensa](#9-qa-típico-de-defensa)
10. [Glosario rápido](#10-glosario-rápido)
11. [Endurecimiento post-revisión: qué se encontró y qué se cambió](#11-endurecimiento-post-revisión-qué-se-encontró-y-qué-se-cambió)

---

## 0. Cómo usar esta guía

Este documento es para vos. No tiene introducciones formales. Los paths a archivos están como `src/...` para que puedas abrirlos al lado mientras leés. Cuando veas un nombre de clase, sé que ese nombre coincide con el nombre de archivo (Java requiere que `class Foo` viva en `Foo.java`).

Tres lentes desde donde leer cada decisión:

- **Lente POO**: qué concepto del paradigma se está aplicando (encapsulamiento, herencia, polimorfismo, abstracción).
- **Lente arquitectura**: en qué capa vive la responsabilidad y por qué (entidad / persistencia / servicio / vista).
- **Lente dominio**: qué pasa en un banco real y cómo lo modelamos.

Cuando defiendas, mezclá los tres. Si te preguntan "¿por qué `Cuenta` es abstracta?" la respuesta corta es "porque hay tipos con comportamiento diferente y querés que sea el compilador el que te obligue a definirlo en cada subtipo", pero la respuesta completa toca los tres lentes.

### Mapa rápido de POO → código

| Concepto         | Dónde en el código                                                                                              |
| ---------------- | --------------------------------------------------------------------------------------------------------------- |
| Encapsulamiento  | `Cuenta.debitar/acreditar`, `Tarjeta.debitar/pagar`, `Usuario.autenticaCon`                                     |
| Herencia         | `Usuario→Empleado/Cliente`, `Cuenta→CajaAhorro/CuentaCorriente`, `MenuView→MenuEmpleadoView/MenuClienteView`   |
| Polimorfismo     | `usuario.crearMenu()`, `cuenta.saldoMinimo()`, `tipo.nuevaCuenta()`, `tipo.signo()`                             |
| Abstracción      | `abstract Cuenta`, `abstract Usuario`, `abstract MenuView`, `abstract FormularioUsuario<T>`                     |
| Interfaz         | `ICrud<T>` → implementado por DAOs, declarado en `EmpleadoServicio` (DIP)                                       |
| Genéricos        | `ICrud<T>`, `FormularioUsuario<T extends Usuario>`                                                              |
| Composición      | `Cuenta` tiene `Cliente` titular, `TarjetaDao` tiene `ClienteDao`                                               |
| Excepciones      | Tres niveles: `SQLException` → `GrabandoException/LeyendoException` → excepciones de dominio                  |
| Template Method  | `FormularioUsuario.montarVentana()`, `MenuView` (constructor + hooks abstractos)                                |
| Factory Method   | `TipoCuenta.nuevaCuenta()`                                                                                      |

---

## 1. Contexto del TP

**Consigna (Tema 2 — Mini Home Banking, UP, POO):**

- Aplicación de escritorio, Java + Swing.
- Entre 4 y 6 entidades con relaciones, al menos una relación objeto-a-objeto.
- Persistencia en H2 embebida con JDBC (no se permite ORM).
- Pantallas mínimas: login, ABM de al menos una entidad, operatoria de transferencias.
- Errores mostrados al usuario, no stacktraces.
- Un empleado del banco hace las altas; los débitos de tarjeta los registra él (no el cliente).
- Aplicar conceptos POO: encapsulamiento, herencia, polimorfismo, interfaces, excepciones.

**Material de la cátedra usado como base:**

- Template `Archivos y BD Swing-26-04-29` — de ahí saqué `BaseH2` (la infraestructura JDBC) y la idea de `ICrud<T>`. El resto (modelo de dominio, servicios, validaciones, vistas) es propio.

---

## 2. Vista de pájaro

La aplicación es un home banking en chiquito. Tiene dos roles:

- **Empleado** (rol `EMPLEADO` en la BD): es el operador del banco. Carga clientes, abre cuentas y tarjetas, registra débitos y pagos de tarjeta, audita todos los movimientos del sistema.
- **Cliente** (rol `CLIENTE`): el titular de las cuentas y tarjetas. Solo puede operar lo suyo: ver sus saldos, transferir entre cuentas, ver sus movimientos, ver sus tarjetas.

Las **5 entidades persistidas** son (las 4 originales + `Cheque`, agregada con la feature de cheques — ver 11.17):

| Entidad     | Subtipos (en código)                  | Tabla        |
| ----------- | ------------------------------------- | ------------ |
| `Usuario`   | `Empleado`, `Cliente`                 | `USUARIOS`   |
| `Cuenta`    | `CajaAhorro`, `CuentaCorriente`       | `CUENTAS`    |
| `Tarjeta`   | (clase única, sin subtipos)           | `TARJETAS`   |
| `Movimiento`| (clase única, distingue por `tipo`)   | `MOVIMIENTOS`|
| `Cheque`    | (clase única, estado vía `EstadoCheque`) | `CHEQUES` |

Las subclases viven en la misma tabla del padre con una **columna discriminadora**: `ROL` para Usuario, `TIPO` para Cuenta. Esto es **single-table inheritance**: simple, sin joins, suficiente para el caso. Si en el futuro Cliente tuviera una columna que Empleado no, agregarías la columna a `USUARIOS` y la dejarías nullable; el DAO concreto la mapea o no según el rol.

**Stack: Java 8 + Swing + JDBC + H2 file-mode.** Sin ORM, sin frameworks, sin Maven/Gradle. Compila con `javac` y corre con `java`. Una sola dependencia externa: `h2-2.2.224.jar`.

---

## 3. Arquitectura en capas

Cuatro paquetes, dependencias unidireccionales:

```
            ┌──────────────┐
            │    vista     │  Swing puro: layouts, listeners, popups.
            └──────┬───────┘  No conoce la BD ni el SQL.
                   │
                   ▼
            ┌──────────────┐
            │   servicio   │  Reglas de negocio + excepciones de dominio.
            └──────┬───────┘  Coordina operaciones; lo multi-statement va en transacción.
                   │
                   ▼
            ┌──────────────┐
            │ persistencia │  JDBC. Mapea filas a objetos. No tiene reglas
            └──────┬───────┘  de negocio: si una query falla, tira SQLException.
                   │
                   ▼
            ┌──────────────┐
            │   entidades  │  POJOs del dominio + lógica propia del objeto
            └──────────────┘  (ej. Cuenta.debitar valida saldoMinimo).
```

### Por qué separar así (defendible)

1. **Cambio aislado**. Si querés cambiar H2 por PostgreSQL, solo tocás `BaseH2` y los DAOs. Si querés cambiar Swing por una interfaz web, solo tocás `vista`. La lógica de negocio (servicios) y el dominio (entidades) quedan intactos.
2. **Testabilidad**. Los servicios reciben DAOs por constructor. Podés inyectar un fake que no toca la BD y testear las reglas de negocio aisladas.
3. **Conocimientos restringidos**. Una entidad no sabe SQL. Un DAO no sabe Swing. Es el principio de responsabilidad única aplicado a la capa.

### Por qué la entidad tiene lógica (y no es un POJO tonto)

`Cuenta.debitar(monto)` está **en la clase Cuenta**, no en `CuentaServicio`. Tres razones:

- Es una regla del dominio (no permitir bajar de `saldoMinimo`), no una regla de coordinación.
- Si la regla viviera en el servicio, sería fácil que dos servicios distintos duplicaran la lógica y se desincronizaran.
- Cumple el principio **"tell, don't ask"**: en lugar de preguntarle el saldo a la cuenta para decidir si podemos restar, le decimos a la cuenta que se debite y ella sabe si puede o no.

El servicio sigue siendo el coordinador: hace el snapshot del saldo previo, llama a `cuenta.debitar(monto)`, persiste en una transacción atómica, y si la persistencia falla la BD hace rollback sola y el servicio restaura el saldo en memoria con el snapshot. Pero el cálculo del nuevo saldo y la validación del mínimo son de la cuenta.

---

## 4. POO en este proyecto

Acá cada pilar del paradigma con un ejemplo concreto del código y por qué se usó.

### 4.1 Encapsulamiento

**Idea**: los datos de un objeto no se leen ni se mutan directamente; se accede a ellos a través de métodos que protegen invariantes.

**Ejemplos:**

- `Cuenta.saldo` es `protected Double`, pero la forma legítima de cambiarlo es `debitar()` o `acreditar()`, que validan. Hay un `setSaldo` que existe por necesidad del DAO (para hidratar desde la BD) y del servicio (para restaurar snapshot en caso de rollback), pero el código de aplicación no lo usa para operar directamente.

- `Tarjeta.disponible` y `Tarjeta.saldoAPagar`: las únicas vías "correctas" para modificarlos son `tarjeta.debitar(monto)` y `tarjeta.pagar(monto)`. `debitar` hace `disponible -= monto` y `saldoAPagar += monto` en conjunto, manteniendo la relación `disponible + saldoAPagar = limiteOriginal` (invariante). `pagar` usa `Math.max(0, saldoAPagar - monto)` para que nunca quede negativo aunque el pago supere la deuda. Si se expusieran los setters directamente, sería fácil romper esa invariante.

- `Usuario.password` se lee solo para autenticación, y el chequeo se hace **dentro** del propio Usuario: `usuario.autenticaCon(passwordIngresada)`. La capa que autentica no ve la contraseña ni la compara; le pregunta al objeto si la password coincide. Eso es encapsulamiento real.

**Por qué importa para la defensa**: si te preguntan "¿cómo evitás que alguien haga `cuenta.setSaldo(-1000000)`?" — la respuesta es que el código de aplicación NUNCA hace eso, siempre pasa por `debitar`/`acreditar`. El setter existe solo para infra (el DAO y la restauración de snapshot en el servicio).

### 4.2 Herencia

**Idea**: una clase deriva atributos y comportamientos de otra, y agrega o redefine lo propio.

**Dos jerarquías en el modelo:**

1. **Usuario → Empleado / Cliente**
   - Comparten: id, username, password, nombre, apellido, dni.
   - Difieren en: qué menú les corresponde. `Usuario` declara `abstract crearMenu()`. `Empleado` devuelve `new MenuEmpleadoView(this)`, `Cliente` devuelve `new MenuClienteView(this)`. `LoginView` llama `usuario.crearMenu()` sin saber el tipo concreto — polimorfismo puro.

2. **Cuenta → CajaAhorro / CuentaCorriente**
   - Comparten: id, alias, cbu, saldo, tipo, titular.
   - Difieren en: `saldoMinimo()` (0 vs -50.000), `permiteCheques()` (false vs true).

**Por qué herencia y no composición:**

Para Usuario, podríamos haber tenido una sola clase `Usuario` con un atributo `tipo: TipoUsuario`. La diferencia importante es: con herencia, **el tipo de Empleado y Cliente son tipos distintos en el sistema de tipos de Java**. Eso me permite hacer:

```java
FormularioEmpleado(Empleado sesion)  // Java garantiza que sesion es Empleado
```

en lugar de:

```java
FormularioEmpleado(Usuario sesion) { if (sesion.tipo != EMPLEADO) throw ... }
```

Es decir, la jerarquía mueve chequeos de runtime al compilador. Eso es valor real.

**Cuándo NO usar herencia (y usé composición):**

`Cuenta` no hereda de `Cliente` porque no hay relación "es un" — hay "tiene un" (un titular). `TarjetaDao` no hereda de `ClienteDao` porque no es un tipo de ClienteDao — lo contiene para resolver titulares. Herencia solo donde hay `is-a`; composición para `has-a`.

### 4.3 Polimorfismo

**Idea**: la misma llamada produce distinto comportamiento según el tipo dinámico del objeto.

**Cuatro usos clave en el proyecto:**

#### a) Polimorfismo en `Usuario.crearMenu()`

```java
// entidades/Usuario.java
public abstract MenuView crearMenu();

// entidades/Empleado.java
@Override
public MenuView crearMenu() { return new MenuEmpleadoView(this); }

// entidades/Cliente.java
@Override
public MenuView crearMenu() { return new MenuClienteView(this); }

// vista/LoginView.java
MenuView menu = usuario.crearMenu();  // ← no hay if, no hay instanceof
```

`LoginView` recibe un `Usuario` del servicio de autenticación. No sabe si es `Empleado` o `Cliente`. Llama `crearMenu()` y el JVM resuelve en runtime qué override ejecutar. Si mañana se agrega un rol `Auditor`, basta crear `Auditor extends Usuario` con su propio `crearMenu()` — `LoginView` no cambia.

Esto es **dispatch dinámico**: el tipo de objeto en memoria determina el método ejecutado. El llamador no necesita conocer el tipo concreto.

#### b) Polimorfismo en `Cuenta.debitar`

```java
public void debitar(Double monto) throws SaldoInsuficienteException {
    if (monto == null || monto <= 0) throw new IllegalArgumentException(...);
    if (saldo - monto < saldoMinimo())                    // <-- llamada polimórfica
        throw new SaldoInsuficienteException(...);
    this.saldo -= monto;
}
```

`saldoMinimo()` es abstract en `Cuenta`. Al ejecutarse, Java elige la implementación correcta según el tipo concreto (`CajaAhorro` devuelve 0, `CuentaCorriente` devuelve -50.000). El método `debitar` no sabe ni le importa qué tipo de cuenta es.

Esto es la **regla del "open/closed principle"**: si mañana agregamos `CuentaSueldo extends Cuenta` con `saldoMinimo() = 0` y otras reglas, no tocamos `debitar`. Está abierto a extensión, cerrado a modificación.

#### c) Polimorfismo en `TipoMovimiento.signo()`

```java
public enum TipoMovimiento {
    TRANSFERENCIA_ENVIADA  { @Override public int signo() { return -1; } },
    TRANSFERENCIA_RECIBIDA { @Override public int signo() { return  1; } },
    DEBITO_TARJETA         { @Override public int signo() { return -1; } },
    PAGO_TARJETA           { @Override public int signo() { return  1; } };

    public abstract int signo();
}
```

`ResumenView` calcula el balance simplemente con:

```java
balance += m.getMonto() * m.getTipo().signo();
```

No hay `switch` ni `if` en la vista: cada valor del enum sabe si aumenta o disminuye el balance. Si mañana agregamos un `TipoMovimiento.COMISION`, el compilador **obliga** a implementar `signo()` antes de poder compilar — no es posible olvidarlo.

Esto es el **principio Open/Closed**: `ResumenView` está cerrada a modificación aunque agreguemos nuevos tipos de movimiento, porque la lógica de interpretación vive dentro del enum, no afuera.

#### d) Polimorfismo en `TipoCuenta.nuevaCuenta()`

```java
public enum TipoCuenta {
    CAJA_AHORRO_PESOS(Moneda.PESOS)      { public Cuenta nuevaCuenta() { return new CajaAhorro(this); } },
    CAJA_AHORRO_DOLARES(Moneda.DOLARES)  { public Cuenta nuevaCuenta() { return new CajaAhorro(this); } },
    CUENTA_CORRIENTE(Moneda.PESOS)       { public Cuenta nuevaCuenta() { return new CuentaCorriente(this); } };
    ...
    public abstract Cuenta nuevaCuenta();
}
```

Aquí el polimorfismo es **a nivel del enum**: cada valor del enum es esencialmente un objeto distinto con su propia implementación de `nuevaCuenta()`. Lo uso para que `CuentaDao.mapearSinTitular` no tenga que hacer `switch(tipo)`:

```java
private Cuenta mapearSinTitular(ResultSet rs) throws SQLException {
    TipoCuenta tipo = TipoCuenta.valueOf(rs.getString("TIPO"));
    Cuenta c = tipo.nuevaCuenta();   // ← polimórfico, sin switch
    c.setId(rs.getInt("ID"));
    // ...
    return c;
}
```

Es una variante del **patrón Factory Method** donde el factory vive dentro del enum.

### 4.4 Abstracción

**Idea**: declarar un contrato sin proveer la implementación. Obliga a las subclases a implementarlo.

**Usos en el proyecto:**

- `abstract class Usuario` con `abstract crearMenu()` — cada subclase sabe qué menú construir. Métodos concretos `autenticaCon` y `toString` en el padre.
- `abstract class Cuenta` con `abstract saldoMinimo()` y `abstract permiteCheques()`.
- `abstract class MenuView` con `abstract tituloVentana()` y `abstract crearPanelOpciones()`.
- `abstract class FormularioUsuario<T>` con varios hooks abstractos.
- `abstract class BaseH2` para que `extends BaseH2` te dé acceso a `selectSql` / `updateDeleteInsertSql` sin que `BaseH2` por sí sola se pueda instanciar.

**Por qué no `interface` en estos casos**: las clases abstractas pueden tener estado y métodos concretos. Eso me permite que `MenuView` defina el `crearEncabezado()` común con código real, y deje solo dos hooks abstractos a las subclases.

### 4.5 Interfaces

**Idea**: contrato puro sin estado ni implementación.

**El único caso del proyecto: `ICrud<T>`**:

```java
public interface ICrud<T> {
    void grabar(T t) throws SQLException;
    T leer(Integer id) throws SQLException;
    List<T> leer() throws SQLException;
    void modificar(T t) throws SQLException;
    void borrar(Integer id) throws SQLException;
}
```

Lo implementan `EmpleadoDao`, `ClienteDao`, `CuentaDao`, `TarjetaDao`. **No** lo implementa `MovimientoDao` (un movimiento no se modifica) ni `UsuarioDao` (es un helper de búsqueda, no un DAO completo de la clase abstracta).

**Uso polimórfico en servicios**: `EmpleadoServicio` declara su dependencia como `ICrud<Empleado>` en lugar de `EmpleadoDao`. Esto aplica el Principio de Inversión de Dependencias en la capa de servicios: el servicio programa contra la abstracción, no contra la implementación concreta. El constructor que acepta `ICrud<Empleado>` permite inyectar cualquier implementación (un fake en tests, un DAO alternativo) sin tocar el servicio.

**Por qué interface y no clase abstracta**: el CRUD genérico no tiene estado compartido. Es un contrato puro. Cada DAO concreto ya extiende `BaseH2` (clase abstracta) para la conexión JDBC, así que la interfaz suma sin obligar a herencia múltiple.

**Diferencia entre clase abstracta e interfaz (resumen para defensa):**

| | Clase abstracta | Interfaz |
|---|---|---|
| Estado (campos) | Sí | No (solo constantes) |
| Métodos concretos | Sí | No (antes de Java 8) |
| Herencia múltiple | No | Sí (implementa varias) |
| Uso acá | `Usuario`, `Cuenta`, `MenuView`, `BaseH2` | `ICrud<T>` |
| Cuándo conviene | Compartís estado y comportamiento | Solo definís un contrato |

### 4.6 Composición

**Idea**: un objeto contiene a otros como atributos.

- `Cuenta` contiene un `Cliente` (su titular).
- `Tarjeta` contiene un `Cliente` (su titular).
- `Movimiento` contiene una `Cuenta` o una `Tarjeta` (mutuamente excluyentes).
- `CuentaDao` y `TarjetaDao` contienen un `ClienteDao` para resolver el titular.
- Los servicios contienen los DAOs que necesitan.

Composición sobre herencia: solo se hereda cuando hay una relación "es un". Para "tiene un" usamos composición. `Cuenta` no hereda de `Cliente` (sería absurdo), sino que tiene una referencia al `Cliente`.

### 4.7 Genéricos

**Dos lugares:**

- `ICrud<T>` para tipar los CRUD por entidad: `EmpleadoDao implements ICrud<Empleado>` te garantiza en compilación que `grabar(...)` solo recibe un `Empleado`.
- `FormularioUsuario<T extends Usuario>` para tipar el template method de los ABMs de usuarios. `FormularioEmpleado extends FormularioUsuario<Empleado>` hereda los métodos genéricos ya tipados a Empleado.

Sin genéricos, harías casts a mano y tendrías `Object` por todos lados, perdiendo seguridad en tiempo de compilación.

### 4.8 Excepciones

**Idea**: comunicar errores como objetos tipados, no como códigos de retorno o nulls.

**Tres niveles (los tres son checked — extienden `Exception`):**

1. **JDBC**: `SQLException` (checked). La cataloga la API de Java.
2. **Persistencia → Servicio**: cada servicio captura `SQLException` y la traduce a `GrabandoException` / `LeyendoException` (ambas también checked, extienden `Exception`). ¿Por qué? Porque a la capa de UI no le interesa que sea SQL: le interesa que "no se pudo grabar" o "no se pudo leer". Si mañana cambio de JDBC a un archivo plano, las vistas no cambian.
3. **Servicio → UI**: excepciones de dominio específicas (`SaldoInsuficienteException`, `TransferenciaInvalidaException`, `CuentaDuplicadaException`, `DatosInvalidosException`, etc.) — todas checked. La vista las captura y muestra un `JOptionPane` con el mensaje.

La única **unchecked** del proyecto es `IllegalArgumentException` en `Cuenta.debitar` cuando el monto es null o <= 0; eso es un bug del caller, no una condición de negocio recuperable.

### 4.9 Patrones de diseño usados

| Patrón                | Dónde                                                                                                                             |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **DAO**               | `BaseH2` + `ICrud<T>` + cada `*Dao`. Separa lógica de negocio del acceso a datos.                                                 |
| **Template Method**   | `FormularioUsuario.montarVentana()` define el flujo; los hooks abstractos los implementan las subclases. Idem `MenuView`.         |
| **Factory Method**    | `TipoCuenta.nuevaCuenta()` decide qué `Cuenta` concreta crear sin que el caller sepa el tipo.                                     |
| **Strategy implícito**| `Cuenta.saldoMinimo()` es la "estrategia" que `debitar` consulta. Cada subclase es una estrategia distinta.                       |

**Template Method en detalle (porque seguro te lo preguntan):**

El patrón define el ALGORITMO en la clase base con pasos concretos y hooks abstractos. En `FormularioUsuario.montarVentana()`:

```
montarVentana() {
    frame = new JFrame(tituloVentana())     ← hook abstracto
    crearPanelFormulario()                  ← usa tituloDatos(), crearEntidadVacia(), etc.
    crearPanelLista()                       ← usa tituloLista()
    crearPanelBotones()                     ← usa guardarNuevo(), eliminar(), etc.
    refrescarLista()                        ← usa listarTodos()
}
```

`FormularioEmpleado` y `FormularioCliente` implementan solo los hooks (qué servicio usan, qué mensajes, cómo dan de alta). La lógica del formulario (layout, validaciones, refresh) no se duplica.

Diferencia con Strategy: en Strategy el algoritmo completo es intercambiable; en Template Method el algoritmo vive en el padre y solo algunos pasos son personalizables.

---

## 5. Flujo end-to-end

Voy a trazar qué ocurre desde que abrís la app hasta que hacés una transferencia.

### 5.1 Boot

```
$ java -cp "bin:lib/*" app.Main
```

1. **`Main.main`** (`src/app/Main.java`):
   - `new InicializadorBD().crear()` → ejecuta los 4 `CREATE TABLE IF NOT EXISTS` y `seedEmpleadoInicial()` (inserta `admin/admin` si no hay ningún EMPLEADO). Si la BD ya existe, los `IF NOT EXISTS` no hacen nada.
   - Si falla, muestra popup de error y sale con código 1.
   - `SwingUtilities.invokeLater(LoginView::new)` → pasa a Swing's Event Dispatch Thread.

2. **`new LoginView()`**:
   - Crea un `AutenticacionServicio(new UsuarioDao())`.
   - Instancia un `LoginFrame` pasándole dos lambdas: `this::intentarLogin` (qué hacer al apretar "Ingresar") y `() -> System.exit(0)` (qué hacer al cancelar).
   - Muestra el frame.

### 5.2 Login

Usuario tipea `admin / admin` y aprieta "Ingresar".

1. **`LoginFrame`** ejecuta el callback `onLogin.accept(username, password)`.
2. **`LoginView.intentarLogin`**:
   ```java
   Usuario usuario = autenticacionServicio.autenticar(username, password);
   ```
3. **`AutenticacionServicio.autenticar`**:
   - `usuarioDao.buscarPorUsername(username)` → SQL: `SELECT ... FROM USUARIOS WHERE USERNAME = ?`.
   - `UsuarioDao.mapear` lee el `ROL` de la fila. Devuelve `new Empleado(...)` o `new Cliente(...)`. Si el ROL no es `EMPLEADO` ni `CLIENTE`, lanza `SQLException("Rol desconocido...")`.
   - Compara `u.autenticaCon(password)`. Si falla → `throw new AutenticacionException(...)`.
   - Si todo OK, devuelve el `Usuario` (concretamente un `Empleado` en este caso).
4. **De vuelta en `LoginView`**:
   ```java
   MenuView menu = usuario.crearMenu();    // ← polimorfismo: cada subclase sabe su menú
   frame.dispose();                         // cierra el login
   menu.alCerrar(LoginView::new);          // al cerrar el menú, reabrir login
   menu.mostrar();
   ```

No hay `instanceof`, no hay `if`. `usuario.crearMenu()` ejecuta el override de `Empleado` o `Cliente` según el tipo real del objeto. Este es el uso de polimorfismo más claro del proyecto.

### 5.3 Menú del empleado

`MenuEmpleadoView` extiende `MenuView`. El constructor del padre arma el layout y llama a `crearPanelOpciones()` (abstract, implementado por el hijo). Se muestran 6 botones: Empleados, Clientes, Cuentas, Tarjetas, Movimientos, Salir.

Cada botón tiene un `ActionListener` que, al click, instancia el formulario correspondiente. **Importante**: cada formulario abre su propio `JFrame`. No hay paneles dentro del mismo frame.

### 5.4 Crear un cliente

Click en "Clientes" → se abre `FormularioCliente extends FormularioUsuario<Cliente>`.

1. `FormularioCliente()` constructor:
   ```java
   this.clienteServicio = new ClienteServicio(new ClienteDao());
   montarVentana();
   ```
2. `montarVentana()` está en `FormularioUsuario` (template method): arma el layout, llama a `refrescarLista()`.
3. `refrescarLista()` llama al método abstracto `listarTodos()` que en `FormularioCliente` devuelve `clienteServicio.listar()`.
4. El usuario completa los campos y aprieta "Guardar":
   - `grabarOModificar()` valida campos (todos completos).
   - Crea un `Cliente` nuevo, lo llena con los datos del form.
   - Llama a `guardarNuevo(cliente)` que es abstract → en `FormularioCliente` hace `clienteServicio.agregar(cliente)`.
5. **`ClienteServicio.agregar`**:
   - Chequea username único contra `usuarioDao.buscarPorUsername(...)` (entre TODOS los roles, no solo clientes).
   - Si OK, `clienteDao.grabar(cliente)` → SQL `INSERT INTO USUARIOS ... VALUES (..., 'CLIENTE')`.
6. Vuelve a la vista, popup "Cliente guardado.", refresca la lista.

### 5.5 Crear una cuenta

Click en "Cuentas" → `FormularioCuenta`. Distinto patrón porque no extiende de un genérico (es una entidad propia).

1. Constructor: `new CuentaServicio(new CuentaDao())` y `new ClienteServicio(new ClienteDao())`.
2. `cargarTitulares()` → llena el combo de Cliente.
3. Usuario elige titular, tipo de cuenta (`CAJA_AHORRO_PESOS` por ejemplo), pone alias `juan.ahorro`, sin CBU, saldo `1000`. Aprieta "Guardar".
4. **`FormularioCuenta.grabarOModificar`**:
   ```java
   Cuenta cuenta = tipo.nuevaCuenta();   // ← POLIMORFISMO: devuelve CajaAhorro
   cuenta.setTitular(titular);
   cuenta.setAlias("juan.ahorro");
   cuenta.setCbu(null);
   cuenta.setSaldo(1000.0);
   cuentaServicio.agregar(cuenta);
   ```
5. **`CuentaServicio.agregar`**:
   - `validarReferencia(c)`: ¿hay alias o cbu? Sí (alias) → OK.
   - `validarSaldoInicial(c)`: `1000 >= 0` (saldoMinimo de CajaAhorro). OK.
   - `chequearUnicidad(c)`: busca cuentas con ese alias y/o CBU.
   - Si todo OK, `cuentaDao.grabar(c)` → INSERT.

Si el operador intenta crear con saldo `-100` en caja de ahorro: `validarSaldoInicial` tira `SaldoInicialInvalidoException("El saldo debe ser mayor o igual a 0.0...")`. La UI la captura y muestra el aviso.

### 5.6 Transferencia (caso más rico)

Cliente entra como `cli1`, abre "Transferencias" → `FormularioTransferencia(cli1)`.

1. Constructor llena el combo de origen con `cuentaServicio.listarPorCliente(cli1)`.
2. Cliente elige origen (`cli1.ahorro`, saldo $1000), modo de búsqueda `Alias`, valor `juan.ahorro`, monto $200.
3. Click en "Buscar destino" → `buscarDestino()`:
   ```java
   destinoActual = null;
   destinoResueltoLbl.setText("(sin resolver)");
   ...
   Cuenta destino = cuentaServicio.buscarPorAlias("juan.ahorro");
   if (destino == null) return ...;
   if (origen.esLaMismaQue(destino)) return ...;       // ← misma cuenta
   if (!origen.mismaMonedaQue(destino)) return ...;    // ← moneda distinta
   destinoActual = destino;
   destinoResueltoLbl.setText(destino.getTipo() + " " + destino.referencia());
   // NO mostramos el saldo del destino: solo "CAJA_AHORRO_PESOS juan.ahorro"
   ```
4. Click en "Transferir" → `ejecutar()`:
   - Valida origen, destinoActual, monto > 0.
   - Llama a `transferenciaServicio.transferir(origen, destinoActual, 200.0, descripcion)`.
5. **`TransferenciaServicio.transferir`**:
   ```java
   // Re-validamos (defensa en profundidad)
   if (origen.esLaMismaQue(destino)) throw new TransferenciaInvalidaException(...);
   if (!origen.mismaMonedaQue(destino)) throw new TransferenciaInvalidaException(...);

   // Snapshot (para restaurar la MEMORIA si la BD falla)
   Double saldoOrigenPrevio  = origen.getSaldo();
   Double saldoDestinoPrevio = destino.getSaldo();

   // Mutación en memoria (puede fallar por saldo)
   origen.debitar(monto);
   destino.acreditar(monto);

   try {
       // 2 updates de saldo + 2 inserts de movimiento, en UNA transacción
       transferenciaDao.transferir(
           origen.getId(), origen.getSaldo(),
           destino.getId(), destino.getSaldo(),
           movimientoEnviado, movimientoRecibido);
   } catch (SQLException e) {
       // La BD ya hizo rollback sola; acá solo restauramos los objetos en memoria
       origen.setSaldo(saldoOrigenPrevio);
       destino.setSaldo(saldoDestinoPrevio);
       throw new GrabandoException(...);
   }
   ```
6. La vista muestra: "Transferencia hecha. Saldo de tu cuenta: $800.00".

**Observación importante**: la transferencia es **atómica de verdad**. `BaseH2.transaccionSql(sqls, params)` abre UNA conexión, hace `setAutoCommit(false)`, ejecuta los N statements, y commitea; si cualquiera falla, hace `rollback` y relanza. O sea: no puede quedar un movimiento grabado sin su contraparte, ni un saldo debitado sin acreditar. El servicio solo tiene que restaurar los objetos en memoria (la BD se cuida sola). Antes esto se resolvía con "compensación manual" (revertir a mano los saldos si fallaba la persistencia), que dejaba dos agujeros: el primer movimiento podía quedar huérfano si fallaba el segundo, y si la propia compensación fallaba quedaba todo inconsistente y en silencio. La transacción real elimina ambos.

Si te preguntan "¿por qué el resto de los DAOs no usa transacciones?" → porque las demás operaciones son de un solo statement (un INSERT, un UPDATE): el auto-commit por operación ya es atómico ahí. `transaccionSql` se usa exactamente donde hay más de un statement que debe ser todo-o-nada: transferencias, débito/pago de tarjeta (saldo + movimiento) y borrado en cascada de cliente.

### 5.7 Resumen de movimientos

Cliente abre "Movimientos" → `ResumenView(cliente)`.

1. Constructor crea el layout (que inicializa `modelo` y `totalLbl` en `crearListado()`, y `cuentaCombo` en `crearFiltros()`), luego:
   ```java
   cargarCuentas();                                    // llena combo
   cuentaCombo.addActionListener(e -> refrescar());    // listener DESPUÉS de cargar
   refrescar();                                        // primer refresh
   ```
2. El orden importa: si el listener se registrara **antes** de `cargarCuentas`, cada `addItem()` en el combo dispararía `refrescar()` prematuramente — antes de que el combo esté completamente cargado. Se registra después para que la inicialización ocurra sin interferencia del listener.
3. `refrescar()` lee `cuentaServicio.movimientos(cuenta)` (la vista no opera SQL directo: si la BD falla, le llega una `LeyendoException` con mensaje humano), opcionalmente filtra por mes, calcula balance con `signo(tipo)` (suma o resta según el TipoMovimiento).

### 5.8 Cierre y reapertura del login

Cuando el cliente cierra su menú:

1. `MenuClienteView.frame` dispara `windowClosed`.
2. El listener registrado en `LoginView.intentarLogin`:
   ```java
   menu.alCerrar(LoginView::new);  // method reference: al cerrar, instanciar un LoginView nuevo
   ```
3. Se abre un login fresco. Si el usuario cancela, `System.exit(0)`.
4. Además, `MenuView.cerrarVentanasHijas()` cierra todos los formularios abiertos de esa sesión. No puede quedar un ABM operable con el login en pantalla.

---

## 6. Recorrido archivo por archivo

Si te preguntan "explicá tal clase", esto te sirve de chuleta.

### Entidades

**`Usuario.java`** (abstract). Atributos comunes (id, username, password, nombre, apellido, dni). Método abstracto: `crearMenu()` — cada subclase sabe qué vista de menú construir para sí misma. Métodos concretos: `autenticaCon(password)` encapsula la comparación de passwords, `toString()` para mostrar en listas.

> Por qué `crearMenu()` vive en `entidades` y no en `vista`: el polimorfismo requiere que la decisión de comportamiento esté en la jerarquía, no en el caller. Si el dispatch estuviera en `LoginView` con `instanceof`, habría que tocar `LoginView` cada vez que se agrega un rol. Con `crearMenu()`, agregar un rol es solo crear una subclase nueva con su override — `LoginView` no cambia.

> Por qué abstract: Usuario solo no se instancia nunca. Siempre es un Empleado o Cliente. Hacerlo abstract previene `new Usuario(...)` y garantiza que `crearMenu()` siempre está implementado.

> Por qué no hay `getRol()` abstracto: los DAOs hardcodean el rol en el SQL y `UsuarioDao.mapear` discrimina leyendo la columna de la BD. La subclase de Java **ya es** el discriminador; devolver `"EMPLEADO"` como string era duplicar esa información — código muerto que se eliminó.

**`Empleado.java`**, **`Cliente.java`**. Subclases concretas. Dos constructores + override de `crearMenu()`. `Empleado.crearMenu()` devuelve `new MenuEmpleadoView(this)`, `Cliente.crearMenu()` devuelve `new MenuClienteView(this)`. Toda la lógica compartida está en el padre.

**`Cuenta.java`** (abstract). Atributos comunes. `tipo` es **`final`**: se asigna en el constructor y no tiene setter — el tipo de una cuenta es su identidad y no cambia. Métodos abstractos: `saldoMinimo()` y `permiteCheques()`. Métodos concretos: `debitar`, `acreditar`, `getMoneda`, `mismaMonedaQue`, `esLaMismaQue`, `referencia`.

**`CajaAhorro.java`**, **`CuentaCorriente.java`**. Subclases concretas. `CajaAhorro` solo overrides los dos abstractos (`saldoMinimo` devuelve 0, `permiteCheques` devuelve false). `CuentaCorriente` además tiene `acuerdoDescubierto` con valor por defecto de 50.000 (no persistido, constante del sistema).

> `permiteCheques()` **sí se usa**: `ChequeServicio.emitir()` lo llama para permitir cheques solo en cuenta corriente (ver 11.17). `CajaAhorro` devuelve `false`, `CuentaCorriente` `true`.

**`Tarjeta.java`** (clase concreta única). Atributos: numero, titular, disponible, saldoAPagar. Métodos de dominio: `debitar(monto)` (reduce disponible, aumenta saldoAPagar), `pagar(monto)` (aumenta disponible, reduce saldoAPagar con `Math.max(0,...)` para no quedar negativo), `ultimosCuatro()`. Sin subclases porque la consigna no lo pide.

**`Movimiento.java`**. Atributos: id, fecha, monto, tipo, descripcion, cuenta (opcional), tarjeta (opcional). Las FKs a cuenta o tarjeta son mutuamente excluyentes en la práctica.

**`TipoCuenta.java`** (enum). Tres valores. Cada valor declara su `Moneda` asociada y override de `nuevaCuenta()` (factory polimórfico). El enum mismo declara `nuevaCuenta()` como abstract.

**`Moneda.java`** (enum). PESOS, DOLARES. Sin lógica.

**`TipoMovimiento.java`** (enum). TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECIBIDA, DEBITO_TARJETA, PAGO_TARJETA. Declara `signo()` como `abstract`: cada valor implementa `+1` o `-1`. El enum sabe interpretar su propio significado — `ResumenView` no necesita switch interno. (CREDITO y DEBITO existieron en una versión anterior pero nunca se asignaron a ningún movimiento real — los eliminé como código muerto.)

### Persistencia

**`BaseH2.java`** (abstract). Driver `org.h2.Driver`, URL `jdbc:h2:./data/banco`. Métodos `selectSql`, `updateDeleteInsertSql` y `transaccionSql` (N statements en una conexión con commit/rollback). Cada operación: carga driver → abre conexión → ejecuta → cierra. Sin pool. Tres endurecimientos propios sobre el template: `selectSql` cierra la conexión si la query falla (antes quedaba abierta y se filtraba), `cargarDriver` lanza `SQLException` si el driver no carga (antes hacía `System.exit(0)`, matando el proceso en silencio), y `transaccionSql` es nueva.

**`ICrud.java`**. Interfaz genérica. 5 métodos.

**`InicializadorBD.java`**. Crea las 5 tablas (`USUARIOS`, `CUENTAS`, `TARJETAS`, `MOVIMIENTOS`, `CHEQUES`) con `CREATE TABLE IF NOT EXISTS` y siembra admin/admin si no hay ningún empleado. Idempotente: se puede llamar N veces sin efecto. Sin migraciones.

**`UsuarioDao.java`**. Un solo método público: `buscarPorUsername`. Devuelve la subclase concreta de `Usuario`. Si el ROL en la BD no es `EMPLEADO` ni `CLIENTE`, lanza `SQLException` explícita. Lo usa `AutenticacionServicio` y los servicios para validar unicidad global de username.

**`EmpleadoDao.java`**, **`ClienteDao.java`**. CRUD completo, cada uno filtra por su rol en TODAS las queries. `ClienteDao` además tiene `borrarEnCascada(id)`: cinco DELETEs (movimientos de cuentas, movimientos de tarjetas, cuentas, tarjetas, usuario) en UNA transacción — o se borra todo o no se borra nada.

**`CuentaDao.java`**. CRUD + helpers de búsqueda (cbu, alias, titular) + `actualizarSaldo`. Detalle importante: `modificar` NO escribe `SALDO` ni `TIPO`. El saldo solo cambia por operatoria y el tipo es inmutable; así, editar el alias no puede pisar un saldo que cambió mientras tanto (lost update). `mapearSinTitular` usa `tipo.nuevaCuenta()` para construir la subclase correcta.

**`TarjetaDao.java`**. CRUD + búsqueda por número + por titular + `registrarOperacion(t, m)`: actualiza los saldos de la tarjeta Y graba el movimiento en una transacción (un débito jamás puede quedar sin asiento). `modificar` no escribe los saldos.

**`TransferenciaDao.java`**. Un solo método: `transferir`. Los 2 updates de saldo y los 2 inserts de movimiento en una transacción. Reusa el SQL de inserción de `MovimientoDao` (`SQL_INSERT` + `parametros(m)`), así el INSERT de movimientos vive en un solo lugar.

**`MovimientoDao.java`**. NO implementa `ICrud<Movimiento>` (decisión consciente). Métodos: `grabar`, `leerPorCuenta`, `leerPorTarjetaYMes`, `borrarPorCuenta`, `borrarPorTarjeta`. Expone (a nivel paquete) el SQL y los parámetros de inserción para que los DAOs transaccionales no dupliquen ese conocimiento.

### Servicios

**`AutenticacionServicio.java`**. Una sola función: `autenticar(username, password)`. Busca por username, verifica con `autenticaCon`, lanza `AutenticacionException` si falla o si la BD falla.

**`EmpleadoServicio.java`**, **`ClienteServicio.java`**. ABM. Unicidad de username global en `agregar` y `modificar`. `borrar` con cascada transaccional (en cliente, vía `ClienteDao.borrarEnCascada`) o con guard de autoborrado (en empleado).

**`CuentaServicio.java`**. ABM + búsquedas. En `agregar`: `validarReferencia` (alias o CBU requerido), `validarSaldoInicial` (>= saldoMinimo del tipo), `chequearUnicidad`. En `modificar`: existencia, **tipo inmutable**, referencia y unicidad. También expone `movimientos(c)` para que las vistas lean movimientos sin tocar el DAO.

**`TarjetaServicio.java`**. ABM + `debitar` y `pagar`: toman snapshot de los saldos, delegan la regla en la entidad, persisten saldos+movimiento en una transacción, y si la BD falla restauran el snapshot.

**`TransferenciaServicio.java`**. Una sola operación: `transferir`. Valida → snapshot → muta → persiste atómico → restaura la memoria si falló.

**Excepciones**. Una por cada caso de error. Convención de nombres: `*Inexistente` (id no encontrado), `*Existente` (ya existe), `*Duplicada` (campo único repetido), `*Invalida` (estado inválido), `*NoPermitida` (regla de negocio).

### Vistas

**`Main.java`** (en `app/`). Lo único: arrancar. Estaba en `dao/` por convención del template original, pero `dao` no tiene ningún significado semántico para un punto de entrada — se movió a `app/` donde sí lo tiene.

**`LoginView.java`**, **`LoginFrame.java`**. Separados a propósito. `LoginView` coordina (instancia el frame, gestiona el ciclo de vida, llama al servicio). `LoginFrame` es Swing puro (layout + listeners). El frame tiene dos callbacks como parámetros del constructor: `BiConsumer<String, String> onLogin` y `Runnable onCancel`. Inversión de dependencias: `LoginFrame` no conoce a `LoginView`, solo conoce contratos (funciones).

**`MenuView.java`** (abstract). Template para los menús: layout fijo (header + opciones), hooks `tituloVentana()` y `crearPanelOpciones()`. Hook opcional: `configurarTamano()` (default 440x320, override en `MenuEmpleadoView` a 440x420 porque tiene un botón más). Lleva el registro de las **ventanas hijas** (`registrarVentana`): cuando el menú se cierra (logout), cierra todas las ventanas de la sesión.

**`MenuEmpleadoView.java`**, **`MenuClienteView.java`**. Cada uno guarda su tipo concreto (empleado / cliente) como campo para evitar casts. Los lambdas de los botones capturan ese campo; por ser lambdas (no se ejecutan en construcción sino al click), el campo ya está seteado cuando se necesita.

**`FormularioUsuario.java`** (abstract `<T extends Usuario>`). Template method completo: layout, lista, botones, validaciones. Hooks abstractos para todo lo específico. Default overridable: `puedeEliminar(T)` y `mensajeEliminacionNoPermitida(T)` (usado por `FormularioEmpleado` para el guard de autoborrado).

**`FormularioEmpleado.java`**, **`FormularioCliente.java`**. Shells del template. Override del guard de autoborrado en Empleado.

**`FormularioCuenta.java`**. ABM con combo de titulares + combo de TipoCuenta. Usa `tipo.nuevaCuenta()` para crear la subclase. Al seleccionar una cuenta existente, el combo de tipo y el campo de saldo se bloquean (tipo es inmutable, saldo solo cambia por operatoria).

**`FormularioTarjeta.java`**. Flag `modoEmpleado = (clienteFijo == null)`. Si es empleado, muestra el form completo + botones de debitar/pagar. Si es cliente, solo lista + resumen mensual. Una clase que sirve dos vistas con leves diferencias, controlado con flag en lugar de duplicar.

**`FormularioTransferencia.java`**. Combo de cuentas propias (origen), búsqueda por Id/CBU/Alias para destino, validaciones en "Buscar destino" antes de habilitar transferir, no muestra saldo del destino. Si el usuario cambia el origen después de haber validado un destino, un listener invalida el destino resuelto (defensa en profundidad, porque el servicio igual re-valida todo).

**`ResumenView.java`**. Combo de cuentas + filtro por mes (`yyyy-MM`). Listener del combo conectado DESPUÉS de `cargarCuentas()` (orden importa: evita refrescos prematuros). Balance calculado con `m.getTipo().signo()` — el enum es polimórfico, la vista no tiene switch.

**`MisCuentasView.java`**. Solo listado read-only de cuentas del cliente.

**`CampoVacioException.java`**. Excepción de UI (no de dominio) porque solo aplica a formularios.

---

## 7. Decisiones defendibles — por qué no lo hice distinto

### 7.1 ¿Por qué estas entidades persistidas?

La consigna pide 4–6. El núcleo son 4: Usuario (con sus dos subtipos), Cuenta (con sus dos subtipos), Tarjeta, Movimiento. La feature de cheques sumó una 5ta (`Cheque`), quedando en 5 — dentro del rango. Las subclases viven en la misma tabla del padre vía columna discriminadora; no inflo la cantidad partiendo `Movimiento` en dos tablas porque sería complejidad sin aporte al dominio.

### 7.2 ¿Por qué no hay `Administrador`?

Lo tuve en una versión previa y lo quité. Razón: en este sistema, "empleado" y "administrador" hacen exactamente lo mismo. Mantener dos clases con el mismo comportamiento es duplicación. Si en el futuro hubiera diferencias (ej. solo el admin puede borrar empleados), valdría una jerarquía `Administrador extends Empleado`. Hoy no la hay.

### 7.3 ¿Por qué `Cuenta.acreditar` lanza `IllegalArgumentException` (unchecked) y no una checked?

Porque pasar un monto null o negativo es un **bug del caller**, no una condición esperable. Las excepciones checked obligan al caller a manejarlas; tiene sentido cuando el caller puede recuperarse razonablemente. Si tu monto es negativo, no hay nada que el caller pueda hacer salvo revisar su propio código.

Distinto es `SaldoInsuficienteException`: eso es un caso de uso real (el cliente quiso transferir más de lo que tiene). Checked, para que la UI lo capture y muestre.

### 7.4 ¿Cómo manejás la atomicidad? (transacciones)

`BaseH2` mantiene el patrón del template (conexión por operación, auto-commit) para los CRUD simples, porque un solo INSERT/UPDATE/DELETE ya es atómico por sí mismo. Para las operaciones **multi-statement** agregué `transaccionSql(sqls, params)`: una conexión, `setAutoCommit(false)`, los N statements, `commit`; si algo falla, `rollback` y relanza.

Lo usan exactamente las operaciones que lo necesitan:

1. **Transferencia** (`TransferenciaDao.transferir`): 2 updates de saldo + 2 inserts de movimiento.
2. **Débito/pago de tarjeta** (`TarjetaDao.registrarOperacion`): update de saldos + insert del movimiento.
3. **Borrado de cliente** (`ClienteDao.borrarEnCascada`): movimientos de cuentas + movimientos de tarjetas + cuentas + tarjetas + usuario (5 DELETEs).
4. **Borrado de cuenta** (`CuentaDao.borrarEnCascada`): movimientos + cuenta (2 DELETEs).
5. **Borrado de tarjeta** (`TarjetaDao.borrarEnCascada`): movimientos + tarjeta (2 DELETEs).

### 7.5 ¿Por qué el cliente no puede registrar débitos de su propia tarjeta?

Es lo que pide la consigna explícitamente: "para simplificar la operatoria solo el usuario administrador, oficiando de entidad bancaria, podrá generarle débitos en las tarjetas a los usuarios". En la práctica real, los débitos los genera el comercio (un POS) y los recibe el banco, así que tiene sentido que sea operación de empleado.

### 7.6 ¿Por qué no muestro el saldo del destino en transferencias?

Por privacidad. El cliente no debería ver el saldo de cuentas que no son suyas. Lo único que muestro es tipo de cuenta + referencia (alias o CBU) para confirmar que está transfiriendo al lugar correcto.

### 7.7 ¿Por qué `acuerdoDescubierto` no se persiste?

Porque el sistema no pide configurarlo por cuenta. La constante de $50.000 es una decisión del banco a nivel sistema. Si en el futuro fuera por cliente o por cuenta, agregaría columna a `CUENTAS` y la mapearía.

### 7.8 ¿Por qué `MovimientoDao` no implementa `ICrud`?

Un movimiento es un evento contable: se crea cuando ocurre algo, se consulta para auditoría, se borra solo cuando se borra la cuenta o tarjeta asociada (cascada). Nunca se modifica ni se borra individualmente desde la UI. La API del DAO refleja esa realidad.

### 7.9 ¿Por qué el `Main` está en el paquete `app` y no en `dao`?

Al principio quedó en `dao` por inercia del template original. Pero el paquete `dao` debería contener solo objetos de acceso a datos — un punto de entrada ahí no tiene sentido semántico. Lo moví a `app`, que es el nombre de paquete estándar para el entry point de una aplicación Java. No hubo ningún cambio de comportamiento: el único propósito de `Main` es llamar a `LoginView`.

### 7.10 ¿Por qué `BaseH2` abre conexión por operación y no la mantiene?

Es el patrón del template original. Tiene la ventaja de que no hay que preocuparse por leaks de conexión. El costo (reabrir la conexión por query) es irrelevante para una app de escritorio con H2 local. La única excepción es `transaccionSql`, que mantiene la conexión abierta durante los N statements de una operación atómica — exactamente lo que una transacción exige.

### 7.11 ¿Por qué no hashing de passwords?

No es objetivo del TP. Las contraseñas se almacenan en texto plano. En producción usaría BCrypt: `Usuario.autenticaCon` haría `BCrypt.checkpw(passwordPlana, this.passwordHash)`.

### 7.12 ¿Por qué Swing y no JavaFX?

Porque el template de la cátedra usa Swing.

### 7.13 ¿Por qué no usé Maven/Gradle?

Para mantener compatibilidad con el método de compilación que enseñó la cátedra (`javac` directo + classpath manual).

### 7.14 ¿Por qué `LoginFrame` está separado de `LoginView`?

`LoginFrame` es Swing puro: widgets, layout, listeners. `LoginView` coordina: instancia el servicio, interpreta el resultado, gestiona el ciclo de vida. Al separarlos, `LoginFrame` no conoce a `LoginView`; depende solo de dos callbacks (`BiConsumer<String,String>` y `Runnable`). Eso es **inversión de dependencias**: el frame no sabe qué pasa con las credenciales, solo las reenvía. En un test podría pasarle lambdas que no hacen nada real.

### 7.15 ¿Por qué `FormularioTarjeta` usa un flag `modoEmpleado` en vez de dos clases?

Porque el 80% del código es compartido (lista, resumen mensual) y la diferencia es exclusivamente cosmética (qué botones y campos se muestran). El flag es la opción más simple. Si en el futuro las dos vistas divergieran más, valdría dividirlas en `FormularioTarjetaEmpleado` y `VistaTarjetaCliente`.

---

## 8. Posibles extensiones

Para anticipar preguntas del tipo "¿y si hubiera que agregar X?". Pensá cómo encajaría con la arquitectura actual.

### 8.1 Agregar tipo de cuenta nuevo (ej. cuenta sueldo)

1. Crear `CuentaSueldo extends Cuenta` con sus implementaciones de `saldoMinimo()` y `permiteCheques()`.
2. Agregar un valor `CUENTA_SUELDO(Moneda.PESOS) { ... nuevaCuenta() ... }` al enum `TipoCuenta`.
3. Listo. `CuentaDao.mapearSinTitular` ya usa polimorfismo, así que la nueva subclase se mapea sola. `FormularioCuenta` muestra el nuevo valor en el combo automáticamente.

**Cero líneas a cambiar fuera de las dos clases nuevas.** Esto es exactamente lo que prometía el polimorfismo.

### 8.2 Agregar rol nuevo (ej. auditor que solo lee)

1. Crear `Auditor extends Usuario` con su `crearMenu()`: `return new MenuAuditorView(this)`.
2. Crear `MenuAuditorView extends MenuView`.
3. En `UsuarioDao.mapear`, agregar un caso `else if ("AUDITOR".equals(rol)) return new Auditor(...)`.
4. En `AuditorDao` (nuevo) hardcodear `ROL = 'AUDITOR'` en todas las queries, igual que hacen `EmpleadoDao` y `ClienteDao`.
5. Si necesita ABM, crear `AuditorServicio` análogo a Empleado.

**`LoginView` no cambia.** `usuario.crearMenu()` ya resuelve el menú correcto por polimorfismo. El único cambio en código existente es `UsuarioDao.mapear` (3 líneas). Esto es exactamente lo que prometía el Open/Closed principle.

### 8.3 Agregar un DAO base para eliminar duplicación Empleado/Cliente

`EmpleadoDao` y `ClienteDao` comparten ~85% del SQL sobre `USUARIOS` con el rol como única diferencia. La mejora: crear `UsuarioDaoBase<T extends Usuario>` con el SQL parametrizado, y que cada DAO concreto extienda de ahí. El servicio y la vista no cambiarían nada — ven la misma interfaz.

### 8.4 Soportar transferencias entre monedas con conversión

Extender `TransferenciaServicio.transferir`:
- Si las monedas difieren, en lugar de tirar `TransferenciaInvalidaException`, aplicar tasa: `montoDestino = monto * tasa`.
- Debitar `monto` del origen y acreditar `montoDestino` al destino.

Cambios: solo `TransferenciaServicio`. La UI puede agregar un cartel "Se aplicará tasa de cambio X".

### 8.5 Emisión de cheques — ✅ YA IMPLEMENTADO

Esto dejó de ser una extensión: se implementó. Ver **11.17** para el detalle. Resumen: entidad `Cheque` + enum `EstadoCheque`, `ChequeDao`/`ChequeServicio`, `FormularioCheque`, tabla `CHEQUES`, y `ChequeServicio.emitir()` valida `cuenta.permiteCheques()` (solo cuenta corriente).

### 8.6 Multi-titularidad de cuentas

Cambio: pasar de `Cuenta.titular: Cliente` a `Cuenta.titulares: List<Cliente>`. Nueva tabla `CUENTAS_TITULARES (ID_CUENTA, ID_CLIENTE)` para n:m. Cambios en `CuentaDao` (query adicional para resolver titulares) y en los servicios para chequear titularidad antes de operar.

### 8.7 Login con hash de password (BCrypt)

1. Agregar dependencia jBCrypt al `lib/`.
2. `Usuario.autenticaCon(passwordPlana)`: `return BCrypt.checkpw(passwordPlana, this.password)`.
3. Al crear/modificar usuario en los servicios: hash antes de persistir.
4. Migración: al primer login de un usuario con password plana, re-hashear.

### 8.8 Auditoría de quién hizo qué

Agregar a `Movimiento` un campo `operadorId: Integer` (FK opcional a Usuario). En transferencias de cliente, queda con el id del cliente. En débitos de tarjeta por empleado, queda con el id del empleado.

### 8.9 Sesiones con timeout

En `MenuView`, un `javax.swing.Timer` que tras N minutos sin actividad cierre el frame y devuelva al login. Necesita registrar listeners de teclado/mouse en todas las vistas para "patear" el timer.

### 8.10 Exportar resumen a PDF

Bajo Apache PDFBox o iText. En `ResumenView`, botón "Exportar PDF" que arme el documento con los movimientos filtrados.

### 8.11 Tests unitarios

`JUnit 4/5`. Los servicios ya son testeables porque reciben DAOs por constructor. Testás:
- `CuentaServicio.agregar` con un `FakeCuentaDao` para validar las reglas.
- `Cuenta.debitar` directamente: instanciar una CajaAhorro, llamar `debitar(monto)`, asertar.
- `TransferenciaServicio.transferir` con DAOs en memoria para validar la coreografía.

### 8.12 Resolución del N+1 en listados

Hoy listar cuentas hace una query por fila para resolver el titular (N+1). La solución: un JOIN en el `SELECT`:
```sql
SELECT C.*, U.USERNAME, U.NOMBRE, U.APELLIDO, U.DNI
FROM CUENTAS C JOIN USUARIOS U ON C.ID_TITULAR = U.ID
```
`CuentaDao.mapear` resolvería el titular desde el mismo ResultSet. Con el volumen de una app de escritorio esto es invisible, pero en un sistema real es importante.

---

## 9. Q&A típico de defensa

### Q: ¿Por qué Cuenta es abstracta?

R: Porque modela una abstracción del dominio que nunca existe sola: en un banco real no hay "una cuenta a secas", siempre es "una caja de ahorro" o "una cuenta corriente". Hacerla abstracta refleja eso. Además, declara dos métodos abstractos (`saldoMinimo`, `permiteCheques`) que cada subtipo concreto debe implementar, así el compilador obliga a definir las reglas específicas.

### Q: ¿Cómo aplicaste polimorfismo?

R: En tres lugares principales:

1. `Cuenta.debitar` consulta `saldoMinimo()` que es abstracto: cada subtipo (`CajaAhorro`, `CuentaCorriente`) define su mínimo y `debitar` funciona sin saber qué subtipo es.
2. `TipoMovimiento.signo()` es abstracto en el enum: cada valor devuelve `+1` o `-1`. `ResumenView` llama `m.getTipo().signo()` sin saber qué tipo de movimiento es. Si se agrega un nuevo tipo de movimiento, el compilador obliga a implementar `signo()` — `ResumenView` no se toca.
3. `CuentaDao.mapearSinTitular` instancia la subclase correcta con `tipo.nuevaCuenta()`: el enum `TipoCuenta` mismo es polimórfico, cada valor sabe qué subclase de `Cuenta` crear.

### Q: ¿Por qué usás herencia y no composición?

R: Uso ambas en lugares distintos. Herencia donde hay relación "es un" (un Empleado **es un** Usuario, una CajaAhorro **es una** Cuenta). Composición donde hay relación "tiene un" (una Cuenta **tiene un** Cliente como titular, un Movimiento **tiene una** Cuenta asociada). La regla es: si dos cosas comparten estado y comportamiento y son del mismo concepto, herencia; si una contiene a otra, composición.

### Q: ¿Por qué la navegación es hijo→padre y `Cliente` no tiene una colección de sus cuentas/tarjetas (ni `Cuenta` sus movimientos)?

R: Porque es una arquitectura DAO **sin ORM**, y ahí la navegación top-down idiomática es **por query**, no por grafo de objetos en memoria: `cuentaDao.leerPorTitular(idCliente)`, `movimientoDao.leerPorCuenta(cuenta)`. Es el equivalente manual a un repository method de Spring Data o un `JOIN FETCH` de JPA.

La relación la modelo unidireccional hijo→padre (`Cuenta` conoce su `Cliente`) porque esa dirección la necesito **siempre** —para mapear la fila desde la BD y para mostrar el titular en las vistas—, mientras que la inversa la necesito **solo a veces** y la resuelvo con una query puntual.

Si en cambio `Cliente` cacheara una `List<Cuenta>`, sin un Unit of Work (sesión de persistencia) esa lista quedaría **rancia** apenas se crea o borra una cuenta, y crearía una **referencia circular** con `Cuenta.titular` —riesgo de recursión infinita en `toString`/`equals`/serialización—. Por eso lo evito a propósito. Además ya neutralicé el N+1: `leerPorTitular` hace 2 queries fijas (las cuentas + una sola para el titular compartido), no una por cuenta. Las colecciones las pongo donde el dato es fresco y acotado: el **servicio** devuelve `List<Cuenta>` en el momento que la vista lo pide (patrón de `MisCuentasView` con su botón "Refrescar"), no cacheadas en la entidad.

### Q: ¿Dónde aplicaste el Principio de Inversión de Dependencias?

R: En tres puntos del proyecto, cada uno en una capa distinta:

1. **`LoginFrame` depende de callbacks, no de `LoginView`**: el frame recibe `BiConsumer<String,String> onLogin` y `Runnable onCancel` en su constructor. No conoce `LoginView`. Podría conectarse a cualquier lógica sin cambiar el frame.

2. **`EmpleadoServicio` depende de `ICrud<Empleado>`, no de `EmpleadoDao`**: el campo y el constructor de inyección usan la interfaz. El servicio no sabe si el DAO habla con H2, con un archivo o con un mock de testing.

3. **`LoginFrame` no conoce a `LoginView`**: separo el frame Swing (que solo tiene widgets) de la coordinación (que instancia servicios y gestiona el ciclo de vida). `LoginFrame` recibe dos callbacks en el constructor — no sabe qué hay del otro lado.

En los tres casos el módulo de alto nivel depende de una abstracción (interfaz o callback) en lugar de depender de la implementación concreta.

### Q: ¿Por qué `Usuario` es abstracta?

R: Dos razones que se refuerzan. Primero, **expresar intención de diseño**: en el dominio bancario no existe un "usuario sin rol"; siempre es un Empleado o un Cliente. Hacer la clase abstracta documenta eso en el código. Segundo, tiene el método abstracto `crearMenu()` — cada subclase sabe qué menú construir. El `abstract` en la clase es consecuencia natural de que hay al menos un método abstracto que las subclases deben implementar. Bonus: `new Usuario(...)` no compila, así que es imposible crear una instancia sin rol por error.

### Q: ¿Por qué `Cuenta.mismaMonedaQue()` compara con `==` y no con `.equals()`?

R: Porque `Moneda` es un enum. En Java, cada constante de un enum existe como una única instancia en la JVM (son singletons por diseño del lenguaje). Por lo tanto `==` y `.equals()` son equivalentes para enums, pero `==` es la comparación idiomática y recomendada: es más eficiente (no hay llamada a método) y expresa más claramente que se están comparando referencias a la misma constante.

### Q: En `ICrud<T>` hay dos métodos llamados `leer`. ¿Eso es valid en Java y qué concepto es?

R: Sí, es **sobrecarga de métodos** (method overloading): mismo nombre, distinta firma — uno recibe `Integer id`, el otro no recibe nada. El compilador los distingue por los tipos de los parámetros en tiempo de compilación. No confundir con sobrescritura (override), que requiere misma firma en una subclase. La sobrecarga es polimorfismo estático (resuelto en compilación); el override es polimorfismo dinámico (resuelto en runtime).

### Q: ¿Por qué `montarVentana()` en `FormularioUsuario` es `final`?

R: Para que ninguna subclase pueda alterar el flujo de construcción del formulario. El patrón Template Method exige que el "esqueleto del algoritmo" sea inviolable — la variación permitida está en los métodos abstractos (`tituloVentana()`, `listarTodos()`, etc.), no en el flujo que los une. Si `montarVentana()` no fuera `final`, una subclase podría redefinirlo y saltearse pasos críticos como `refrescarLista()` o el `setVisible(true)`.

### Q: ¿`Cuenta` tiene composición o agregación con `Cliente`?

R: Técnicamente es **agregación**: el `Cliente` existe independientemente de la `Cuenta` — si se borra la cuenta, el cliente sigue existiendo en el sistema. La composición estricta implicaría que el objeto contenido no tiene sentido sin el contenedor. Aquí `titular` puede ser `null` durante la construcción del objeto y el `Cliente` vive en su propia tabla de BD, lo que confirma que es agregación. En la práctica ambos términos se usan informalmente como "composición" (relación "tiene-un"), pero para ser preciso frente al docente la respuesta correcta es agregación.

### Q: ¿Por qué `LoginView` no usa `instanceof` para detectar el rol?

R: Porque `instanceof` es exactamente el antipatrón que el polimorfismo reemplaza. Si `LoginView` tuviera `if (usuario instanceof Empleado) ... else if (usuario instanceof Cliente) ...`, habría que modificar `LoginView` cada vez que se agrega un rol nuevo. En cambio, `usuario.crearMenu()` delega la decisión a la jerarquía: `Empleado.crearMenu()` devuelve su menú, `Cliente.crearMenu()` devuelve el suyo. Al agregar `Auditor`, basta crear `Auditor extends Usuario` con su propio override — `LoginView` no cambia. Eso es el **Open/Closed principle** aplicado al dispatch de vistas.

### Q: ¿Cuál es la diferencia entre clase abstracta e interfaz? ¿Cuándo usaste cada una?

R: Una clase abstracta puede tener estado (campos de instancia), constructores y métodos concretos. Una interfaz solo declara firmas (antes de Java 8, no puede tener código).

Usé clases abstractas donde hay estado y comportamiento compartido: `Usuario` (campos id, username, password...), `Cuenta` (campos saldo, alias, cbu, titular; métodos `debitar`, `acreditar`), `MenuView` (el frame, la lista de ventanas hijas, `crearEncabezado()` concreto). Si hubiera hecho esas interfaces, los subtipos no tendrían dónde poner los campos comunes y habría duplicación.

Usé `ICrud<T>` como interfaz porque es un contrato puro de cinco métodos sin estado compartido. Además, los DAOs ya extienden `BaseH2` (herencia simple), así que la interfaz suma sin restricciones.

### Q: ¿Qué pasa si dos personas distintas intentan transferir al mismo destino al mismo tiempo?

R: En esta aplicación, nada raro porque es de escritorio mono-usuario por proceso. Para multi-usuario real, habría que pasar a una BD servidora (Postgres) y usar transacciones con `SELECT ... FOR UPDATE` o nivel de aislamiento serializable para detectar lecturas sucias y escrituras concurrentes.

### Q: ¿Qué pasa si me llamás `cuenta.setSaldo(-1000000)`?

R: Funciona técnicamente, pero el código de aplicación tiene dos usos legítimos de `setSaldo`: el DAO lo usa para hidratar el objeto al leer de la BD, y `TransferenciaServicio` lo usa para restaurar el snapshot en memoria cuando la BD falla y ya hizo rollback. Ninguno de los dos es una "operación financiera". La forma idiomática de mutar el saldo es siempre `cuenta.debitar(monto)` o `cuenta.acreditar(monto)`, que validan. Si quisiera ser estricto, haría el setter `package-private` y movería los DAOs/servicios al mismo paquete que las entidades.

### Q: ¿Por qué `ICrud` es genérica?

R: Para tener seguridad de tipos en compilación. Sin genéricos, `void grabar(Object t)` y dentro habría que castear. Con `ICrud<T>`, `EmpleadoDao implements ICrud<Empleado>` garantiza que `grabar(...)` solo acepta `Empleado` y `leer(id)` devuelve `Empleado`. El compilador lo verifica.

### Q: ¿Qué pasa si borro el cliente seleccionado?

R: `ClienteServicio.borrar` valida que exista y delega en `ClienteDao.borrarEnCascada`: cinco DELETEs ejecutados en **una transacción**. O se borra todo (movimientos de cuentas, movimientos de tarjetas, cuentas, tarjetas, el usuario) o no se borra nada — no puede quedar un cliente a medio borrar.

### Q: ¿Por qué no usaste `ON DELETE CASCADE` en las FKs?

R: Porque prefiero que la cascada sea **visible y explícita** en el código: los cinco DELETEs están escritos en `borrarEnCascada`, en el orden correcto, dentro de una transacción. Con `ON DELETE CASCADE` el comportamiento queda escondido en el schema y cualquier DELETE accidental arrastra datos sin que el código lo muestre.

### Q: ¿Qué excepciones implementaste y por qué?

R: Tengo tres niveles. `SQLException` de JDBC. Genéricas de infra: `GrabandoException` y `LeyendoException`. Específicas de dominio: `SaldoInsuficienteException`, `TransferenciaInvalidaException`, `CuentaDuplicadaException`, etc. Todas checked (extienden `Exception`) porque son condiciones esperables que el caller debe manejar. La única unchecked es `IllegalArgumentException` en `debitar`/`acreditar` cuando el monto es inválido — eso es bug del caller.

### Q: ¿Cómo evitás duplicar lógica entre `FormularioEmpleado` y `FormularioCliente`?

R: Con `FormularioUsuario<T extends Usuario>`, un template method abstracto. Define el flujo común (layout, validaciones, refrescar lista, grabar, eliminar) y deja hooks abstractos para lo específico (qué servicio usa, qué mensajes muestra, cómo elimina). Cada formulario concreto es de ~80 líneas porque la lógica está en la base.

### Q: ¿Por qué `MovimientoDao` no implementa `ICrud`?

R: Porque un movimiento no se modifica ni se borra individualmente. Es un evento contable: se crea cuando ocurre algo, se consulta filtrado por cuenta/tarjeta/mes, se borra solo en cascada. Forzarlo a implementar `ICrud` me obligaba a tener métodos `modificar` y `leer(id)` que la app nunca llama.

### Q: ¿Qué pasa con la conexión a la BD si falla la transferencia a mitad?

R: La transferencia corre dentro de una transacción real (`BaseH2.transaccionSql`): una sola conexión, auto-commit apagado, los 4 statements, y commit al final. Si cualquier statement falla, se hace `rollback` y la conexión se cierra en el `finally` — la BD queda exactamente como estaba. El servicio restaura los saldos de los objetos en memoria (tomó snapshot antes de mutar). Datos atómicos, conexión sin leaks.

### Q: ¿Cómo se diferencian conceptualmente caja de ahorro y cuenta corriente?

R: Tres diferencias en el dominio bancario clásico, dos modeladas:

1. **Caja de ahorro no permite girar al descubierto.** Modelado en `saldoMinimo()`: 0 vs -50.000.
2. **Cuenta corriente permite emitir cheques.** Modelado en `permiteCheques()` y **usado** por la feature de cheques (ver 11.17): solo la cuenta corriente puede emitir.
3. (No modelada) Cuenta corriente típicamente cobra mantenimiento mensual.

### Q: ¿Qué cambiarías si lo hicieras de nuevo?

R: Probablemente:

1. Un DAO base común para `EmpleadoDao`/`ClienteDao` (hoy comparten ~85% del SQL).
2. Resolver el titular con un JOIN en los listados (hoy es N+1; con pocos registros no se nota).
3. Hashing de passwords (BCrypt) y no precargar la contraseña en el formulario de edición.
4. Tests unitarios permanentes para los servicios.

(Lo de "darle uso a `permiteCheques()`" ya lo hice: es la feature de cheques, ver 11.17.)

Lo que mantendría: la separación en capas, las jerarquías de herencia, el factory polimórfico en `TipoCuenta`, la lógica de dominio dentro de las entidades, y las transacciones para las operaciones multi-statement.

### Q: Explicá el patrón Template Method con `FormularioUsuario`.

R: Template Method define el ALGORITMO en la clase base (una secuencia de pasos con algunos pasos abstractos), y deja que las subclases implementen solo esos pasos. En `FormularioUsuario.montarVentana()`:

- Pasos concretos (en el padre): construir el frame, los paneles, registrar listeners, llamar a refrescarLista.
- Hooks abstractos (a implementar en cada subclase): `tituloVentana()`, `listarTodos()`, `guardarNuevo(T)`, `guardarModificacion(T)`, `eliminar(Integer)`, etc.

`FormularioEmpleado` y `FormularioCliente` solo implementan los hooks. El resultado: cero duplicación del código de layout, validación y refresh.

### Q: ¿Cómo garantizás que un cliente no vea ni opere cuentas de otro cliente?

R: La vista filtra: `FormularioTransferencia` carga el combo de origen con `cuentaServicio.listarPorCliente(cliente)`, donde `cliente` es el objeto de sesión autenticado. Solo las cuentas del cliente aparecen como opciones de origen. El destino puede ser de cualquier cliente (es el propósito). El servicio re-valida moneda, cuenta distinta y saldo — pero no chequea titularidad porque la vista ya es la frontera de entrada. Con una sola UI de escritorio eso alcanza.

### Q: ¿Qué es single-table inheritance y por qué lo usaste?

R: Estrategia donde todos los subtipos de una jerarquía viven en una sola tabla con una columna discriminadora (aquí `ROL` para usuarios, `TIPO` para cuentas). Conviene cuando los subtipos comparten la mayoría de los campos y las diferencias son pocas. La alternativa (table-per-class) requiere JOINs o UNIONs para leer el tipo base junto con el concreto. Para este dominio, donde Empleado y Cliente comparten todos los campos, single-table es lo más simple.

### Q: ¿Cómo funciona la inicialización de la BD?

R: `InicializadorBD.crear()` ejecuta `CREATE TABLE IF NOT EXISTS` para las 5 tablas. Si ya existen, la sentencia no hace nada (idempotente). Luego `seedEmpleadoInicial()` chequea si hay algún `EMPLEADO` en la tabla; si no hay ninguno, inserta `admin/admin`. Así, la primera corrida crea todo; las siguientes arrancan con el estado previo. Para resetear: borrar la carpeta `data/`.

### Q: ¿Por qué `Cuenta.tipo` es `final`?

R: Porque el tipo de cuenta es su identidad y no debería cambiar después de creada. El `final` hace que se asigne en el constructor y no tenga setter. Esto es encapsulamiento a nivel de compilador: es imposible escribir `cuenta.setTipo(otraCosa)` porque el método no existe, y aunque el campo fuera accesible por reflexión, la intención es clara. El servicio también valida que no cambie al modificar, pero el `final` es la primera y más fuerte barrera.

### Q: ¿Por qué eliminaste `getRol()`?

R: Porque nadie lo llamaba. La lógica que discrimina por rol en los DAOs hardcodea el string en el SQL (`WHERE ROL = 'EMPLEADO'`) y `UsuarioDao.mapear` lee la columna de la BD para decidir qué subclase instanciar. La subclase de Java ya **es** el discriminador en el sistema de tipos. Tener `getRol()` que devuelve `"EMPLEADO"` en `Empleado.java` era duplicar esa información en forma de string literal — código muerto. El único método abstracto que quedó en `Usuario` es `crearMenu()`, que sí tiene un caller real: `LoginView`.

### Q: ¿Por qué borrar una cuenta hacía falta `borrarEnCascada`?

R: Porque antes `CuentaServicio.borrar` llamaba primero a `movimientoDao.borrarPorCuenta(id)` y después a `cuentaDao.borrar(id)` como dos operaciones independientes. Si fallaba la segunda, la cuenta seguía en la BD pero sin movimientos — estado inconsistente, igual al bug que tenía la transferencia. El arreglo: mover los dos DELETEs dentro de `CuentaDao.borrarEnCascada` usando `transaccionSql`, donde o se completan ambos o ninguno. Mismo patrón que ya tenía `ClienteDao`.

### Q: ¿Qué pasa si cerrás una ventana hija antes de hacer logout?

R: La ventana se cierra normalmente y el listener `windowClosed` que `registrarVentana` le puso la quita de la lista `ventanasHijas`. Cuando después el usuario hace logout, la lista solo contiene las ventanas que todavía están abiertas, y esas se cierran. Sin el listener, las ventanas cerradas se acumulaban en la lista indefinidamente — memory leak suave, y al logout se intentaba cerrar ventanas ya destruidas (inocuo, pero incorrecto).

### Q: ¿Por qué `Tarjeta.pagar` usa `Math.max(0, saldoAPagar - monto)` en vez de `saldoAPagar -= monto`?

R: Para mantener una invariante: `saldoAPagar` nunca puede ser negativo. Si el pago supera la deuda (pagás $200 pero debés $100), sin `Math.max` quedaría `saldoAPagar = -100`, lo cual no tiene sentido — no hay "crédito" por pagar de más. Con `Math.max(0, ...)`, `saldoAPagar` llega a cero como mínimo. Esto es encapsulamiento: la regla del dominio vive dentro del método, no en el caller.

### Q: ¿Por qué separaste `LoginView` de `LoginFrame`?

R: `LoginFrame` es Swing puro: widgets, layout, listeners, no sabe qué hacer con las credenciales. `LoginView` coordina: instancia el servicio, interpreta el resultado, gestiona el ciclo de vida. Al separarlos, `LoginFrame` depende solo de dos contratos funcionales (`BiConsumer<String,String> onLogin` y `Runnable onCancel`) — no de `LoginView`. Eso es **inversión de dependencias**: los componentes de alto nivel (LoginView) inyectan su comportamiento en los de bajo nivel (LoginFrame), no al revés.

### Q: ¿Qué pasaría si alguien intenta dar de alta una cuenta sin alias ni CBU?

R: `CuentaServicio.agregar` llama a `validarReferencia(c)` primero. Si alias y CBU son ambos nulos o vacíos, lanza `CuentaDuplicadaException("Cargá al menos un alias o un CBU...")`. La vista captura esa excepción y muestra el mensaje. No llega al DAO.

### Q: ¿Cómo se compila y ejecuta el proyecto? ¿Qué significan esos comandos?

R: Dos pasos: compilar y ejecutar.

**Compilar:**
```
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" <archivos .java>
```

- `javac`: el compilador de Java (parte del JDK). Convierte `.java` a `.class`.
- `-source 8 -target 8`: el código fuente usa sintaxis de Java 8 y los `.class` generados son compatibles con JVM 8+. Garantiza que no se use sintaxis de versiones más nuevas.
- `-encoding UTF-8`: los fuentes están en UTF-8 (tildes, ñ). Sin esto, en Windows podría fallar al leer los literales de texto.
- `-d bin`: los `.class` compilados se depositan en la carpeta `bin/`, respetando la estructura de paquetes (`bin/entidades/Cuenta.class`, etc.).
- `-cp "lib/*"`: classpath de compilación — le dice al compilador dónde encontrar las clases externas que el código importa. Acá solo hay una: `h2-2.2.224.jar`. Sin esto, `import java.sql.*` compila bien (viene del JDK), pero las clases de H2 que usa `BaseH2` no se encontrarían.
- `$(find src -name "*.java")`: expande a la lista de todos los archivos `.java` del proyecto.

**Ejecutar:**
```
java -cp "bin:lib/*" app.Main
```

- `java`: la JVM, corre `.class`.
- `-cp "bin:lib/*"`: classpath de ejecución — dos entradas separadas por `:` (`;` en Windows). `bin` es donde viven los `.class` del proyecto; `lib/*` es el JAR de H2. Ambos son necesarios en runtime: el código del proyecto está en `bin`, y cuando la JVM carga `org.h2.Driver` lo busca en `lib/`.
- `app.Main`: la clase de entrada. `app` es el paquete, `Main` es la clase que tiene el método `public static void main(String[] args)`.

**Por qué no Maven/Gradle:** para que el método de compilación sea exactamente el que enseña la materia — sin herramientas de build externas que el evaluador no esperaría.

---

## 10. Glosario rápido

| Término               | Lo que significa acá                                                                          |
| --------------------- | --------------------------------------------------------------------------------------------- |
| **ABM**               | Alta, Baja, Modificación (CRUD - Read).                                                       |
| **DAO**               | Data Access Object. Una clase que encapsula el acceso a una entidad en la BD.                 |
| **POJO**              | Plain Old Java Object. Una clase con campos, getters y setters, sin lógica.                   |
| **Discriminador**     | Columna en una tabla que distingue subtipos (ej. `ROL`, `TIPO`).                              |
| **Single-table inheritance** | Estrategia: todos los subtipos viven en la misma tabla.                                |
| **Template method**   | Patrón: la clase base define el algoritmo con hooks abstractos que las subclases implementan. |
| **Factory method**    | Patrón: un método encargado de crear instancias, polimórfico por subclase.                    |
| **Compensación**      | Cuando no podés usar una transacción real, revertir manualmente los cambios si algo falla.    |
| **Checked exception** | Excepción que el compilador exige declarar o capturar (extiende `Exception`).                 |
| **Unchecked**         | Excepción que el compilador NO exige manejar (extiende `RuntimeException`).                   |
| **Open/closed**       | Principio: abierto a extensión, cerrado a modificación.                                       |
| **Tell, don't ask**   | Principio: decile al objeto qué hacer, no le preguntes datos para decidir afuera.             |
| **Inversión de dependencias** | Los módulos de alto nivel no dependen de los de bajo nivel: ambos dependen de abstracciones. |
| **N+1**               | Problema de performance: una query por fila al listar (hoy en cuentas/tarjetas al resolver titular). |

---

## 11. Endurecimiento post-revisión: qué se encontró y qué se cambió

En junio de 2026 le pasé al proyecto una revisión de código exhaustiva (varios revisores independientes mirando corrección, persistencia, duplicación y diseño, con verificación de cada hallazgo contra el código real). Esta sección documenta qué se encontró y cómo se arregló, porque **contar un bug que encontraste y cerraste vale más en una defensa que pretender que nunca existió**.

### 11.1 La transferencia podía dejar la BD a medias → transacción real

**Qué pasaba.** La persistencia de una transferencia eran 4 statements independientes (2 updates de saldo, 2 inserts de movimiento), cada uno con su propia conexión auto-commit. Si fallaba el tercero o el cuarto, la "compensación manual" revertía los saldos… pero el movimiento ya grabado quedaba huérfano. Y si la compensación misma fallaba, su `catch` vacío se tragaba el error: saldos en memoria distintos de la BD, en silencio.

**El arreglo.** `BaseH2.transaccionSql(sqls, params)`: una conexión, `setAutoCommit(false)`, N statements, `commit`/`rollback`. Sobre eso, `TransferenciaDao.transferir` hace los 4 statements atómicos. El servicio quedó más simple: el método `compensar` desapareció — la BD se revierte sola, el servicio solo restaura los dos saldos en memoria.

**Lente arquitectura.** El mismo helper resuelve los otros dos puntos con más de un statement: `TarjetaDao.registrarOperacion` y `ClienteDao.borrarEnCascada`. Regla resultante: *un statement → auto-commit; más de uno → transacción*.

### 11.2 El ABM podía pisar plata → saldo y tipo fuera del `modificar`

**Qué pasaba.** Dos lost-updates demostrables:

1. Empleado selecciona una tarjeta (el form carga disponible=1000), hace **Debitar 200** (la BD queda en 800, el form no se entera), corrige un dígito del número y toca **Guardar** → `modificar` escribía el disponible viejo (1000) y el débito desaparecía sin dejar rastro.
2. Lo mismo con cuentas: editar el alias re-escribía un `SALDO` que una transferencia podía haber cambiado en el medio.

Y un tercero conceptual: al modificar se podía cambiar el **tipo** de la cuenta — re-denominación silenciosa.

**El arreglo, en tres capas:**

- **Persistencia**: `CuentaDao.modificar` ya no escribe `SALDO` ni `TIPO`; `TarjetaDao.modificar` ya no escribe los saldos.
- **Servicio**: `CuentaServicio.modificar` compara el tipo persistido contra el del form y rechaza con `OperacionNoPermitidaException`.
- **Vista**: al seleccionar una cuenta/tarjeta existente, los campos de saldo y el combo de tipo se bloquean.

### 11.3 El logout dejaba ventanas vivas → `MenuView` cierra a sus hijas

**Qué pasaba.** Al tocar "Salir", el menú se cerraba y reaparecía el login… pero cualquier formulario abierto seguía vivo y operable. Una "sesión cerrada" que podía seguir dando de alta empleados. Además, la lista `ventanasHijas` crecía sin límite: las ventanas que el usuario cerraba antes del logout no se quitaban de la lista — memory leak suave en sesiones largas.

**El arreglo.** `MenuView.registrarVentana` añade la ventana a la lista Y le pone un listener `windowClosed` que la quita de la lista cuando el usuario la cierra por su cuenta. Al logout, el menú recorre lo que quede en la lista y cierra todo. Nada queda vivo, nada se acumula.

### 11.4 Robustez de infraestructura (BaseH2 y mapeos)

- `selectSql` no tenía `finally`: si la query fallaba, la conexión H2 quedaba abierta. Ahora cierra antes de relanzar.
- `cargarDriver` hacía `System.exit(0)` si el driver no cargaba: mataba el proceso sin mensaje y con código de éxito. Ahora lanza `SQLException`.
- `UsuarioDao.mapear` trataba cualquier `ROL` desconocido como Cliente (una fila corrupta se logueaba en silencio). Ahora exige `EMPLEADO` o `CLIENTE` y falla explícito.
- `ResumenView` era la única vista que mostraba el `getMessage()` crudo de `SQLException`. Ahora pasa por `CuentaServicio.movimientos` como todas las demás.

### 11.5 Validación de la vista salteable → invalidar el destino al cambiar el origen

**Qué pasaba.** En transferencias se podía buscar y validar un destino con el origen A, y después cambiar el combo a B sin re-buscar: la validación de la vista quedaba hecha contra el origen equivocado. El servicio igual re-validaba todo — por eso nunca fue un bug de plata — pero el usuario recibía el mensaje del servicio con texto distinto al de la vista.

**El arreglo.** Un listener en el combo de origen invalida `destinoActual` y obliga a re-buscar.

### 11.6 Limpieza

- Código muerto eliminado: constructores sin uso de `Cuenta`/`CajaAhorro`/`CuentaCorriente`/`Tarjeta`/`BaseH2`, `Cuenta.setTipo`, accessors de `acuerdoDescubierto`, `MovimientoDao.leerPorTarjeta`.
- Montos con 2 decimales en todos los `toString` y mensajes.
- El catch genérico de `FormularioUsuario` ahora muestra "Ocurrió un error inesperado." si la excepción no trae mensaje (antes podía mostrar `null`).

### 11.7 Borrar cuenta o tarjeta también era no-atómico → `borrarEnCascada` en Cuenta y Tarjeta

**Qué pasaba.** `CuentaServicio.borrar` hacía dos llamadas separadas: primero `movimientoDao.borrarPorCuenta(id)` y después `cuentaDao.borrar(id)`. Si el segundo statement fallaba, la cuenta seguía en la BD pero sin movimientos. El mismo problema en `TarjetaServicio.borrar`. Idéntico al bug de la transferencia pero en las bajas.

**El arreglo.** Se agregó `borrarEnCascada(Integer id)` a `CuentaDao` y a `TarjetaDao`, usando el mismo `transaccionSql`. Cada método encapsula los DELETEs en una transacción: o se borra todo o no se borra nada. Los servicios quedaron con una sola llamada al DAO, sin tocar `MovimientoDao` directamente.

**Por qué es diseño correcto.** La responsabilidad de saber qué tablas dependen de una cuenta está en el DAO de cuentas, no en el servicio. El servicio no debería conocer la existencia de `MovimientoDao` para una operación de borrado de cuenta — eso es un detalle de infraestructura.

### 11.8 Código muerto eliminado: `getRol()` y valores de `TipoMovimiento`

**`getRol()`.** `Usuario` tenía un método abstracto `getRol()` que devolvía el string del rol (`"EMPLEADO"`, `"CLIENTE"`). Lo sobreescribían `Empleado` y `Cliente`. El problema: nadie lo llamaba. Los DAOs hardcodean el rol en el SQL; `UsuarioDao.mapear` lo lee de la BD directamente. La subclase de Java **ya es** el discriminador — `getRol()` duplicaba esa información de forma redundante y fue eliminado. `crearMenu()` es el único método abstracto de `Usuario`, y sí tiene un caller real: `LoginView.intentarLogin`.

**`TipoMovimiento.CREDITO` y `.DEBITO`.** Existían en el enum pero nunca se asignaban a ningún `Movimiento` en todo el código. Los cuatro valores usados son `TRANSFERENCIA_ENVIADA`, `TRANSFERENCIA_RECIBIDA`, `DEBITO_TARJETA` y `PAGO_TARJETA`. Se eliminaron los dos sin uso, y se limpió el `case CREDITO` que `ResumenView.signo()` tenía en un switch.

### 11.9 Segunda ronda de revisión: hallazgos de diseño OO

Los siete hallazgos anteriores (11.1–11.8) eran bugs de corrección y robustez. Esta segunda ronda buscó violaciones de principios OOP: dependencias en la dirección incorrecta, excepciones con semántica equivocada, lógica que vivía fuera de la entidad que la debería poseer.

### 11.10 Dependencia entidades→servicio eliminada: `SaldoInsuficienteException` movida a `entidades`

**Qué pasaba.** `Cuenta.debitar` y `Tarjeta.debitar` lanzaban `SaldoInsuficienteException`, pero esa clase estaba en el paquete `servicio`. Resultado: `entidades` importaba `servicio` — la capa más interna del sistema dependía de una capa más externa.

**El arreglo.** Se creó `entidades.SaldoInsuficienteException` y se eliminó la clase original de `servicio`. Todos los que la lanzaban (`Cuenta.debitar`, `Tarjeta.debitar`, `TransferenciaServicio`, `TarjetaServicio`) y los que la capturaban (`FormularioTarjeta`, `FormularioTransferencia`) ahora usan la de `entidades`. `Cuenta` y `Tarjeta` ya no importan `servicio`.

> Nota de revisión final: en un paso intermedio la clase vieja quedó como un shim `@Deprecated` que extendía la nueva, para no romper compilación. Pero como ningún código la referenciaba, mantener dos clases con el mismo nombre simple solo generaba una pregunta incómoda en la defensa ("¿por qué hay dos `SaldoInsuficienteException`?"). Se borró el shim: en un proyecto sin consumidores externos, la compatibilidad hacia atrás no le sirve a nadie.

**Lente arquitectura.** Las excepciones de dominio deben vivir en `entidades`: son parte del contrato del objeto, no detalles de coordinación del servicio.

### 11.11 Decisión de diseño: `crearMenu()` en `Usuario` como polimorfismo de dispatch

**La decisión.** `Usuario` declara `abstract crearMenu()`. `Empleado` y `Cliente` la implementan devolviendo su vista de menú. `LoginView` llama simplemente:

```java
MenuView menu = usuario.crearMenu();
frame.dispose();
menu.alCerrar(LoginView::new);
menu.mostrar();
```

**Por qué es la decisión correcta para este proyecto.** El objetivo de la materia es demostrar polimorfismo. `usuario.crearMenu()` es el ejemplo más directo: el caller no sabe el tipo concreto, el JVM elige el override en runtime. Si se pusiera `instanceof` en `LoginView`, habría que tocarlo cada vez que se agrega un rol — exactamente lo que Open/Closed prohíbe.

**El tradeoff aceptado.** `entidades` importa `vista` (porque `crearMenu()` retorna `MenuView`). Esto rompe la regla de capas unidireccional. La alternativa arquitectónicamente pura sería una interfaz `MenuFactory` en una capa intermedia, pero agrega complejidad que no fue pedida. Para un TP de demostración de POO, el polimorfismo puro pesa más que la pureza de capas — y si el docente pregunta, la respuesta es exactamente esa.

### 11.12 Open/Closed en `TipoMovimiento`: `signo()` abstracto

**Qué pasaba.** `ResumenView` tenía un método privado `signo(TipoMovimiento tipo)` con un `switch`: si se agregaba un nuevo tipo de movimiento, había que acordarse de actualizar `ResumenView`. Lógica sobre el dominio dispersa en la vista.

**El arreglo.** `TipoMovimiento.signo()` es ahora `abstract` en el enum, con implementación por valor. `ResumenView` simplemente llama `m.getTipo().signo()`.

**Por qué importa.** El compilador ahora rechaza cualquier nuevo valor de `TipoMovimiento` sin `signo()`. Imposible olvidarlo — el Open/Closed principle se cumple por construcción.

### 11.13 Semántica correcta de excepciones: `DatosInvalidosException`

**Qué pasaba.** `CuentaServicio.validarReferencia` lanzaba `CuentaDuplicadaException` cuando alias y CBU eran ambos vacíos. Semántica incorrecta: una cuenta sin alias ni CBU no es una cuenta *duplicada*, es un dato requerido faltante.

**El arreglo.** Se creó `servicio.DatosInvalidosException`. `validarReferencia` la lanza en su lugar. `FormularioCuenta` la captura en su multi-catch.

**Lente defensa.** Los nombres de las excepciones son parte de la documentación del sistema. Una excepción mal nombrada es una mentira en el código.

### 11.14 `CuentaCorriente` configurable: constructor parametrizado

**Qué pasaba.** `acuerdoDescubierto` era una constante privada de 50.000 no configurable. Imposible crear una `CuentaCorriente` con un límite distinto (en tests o para nuevas reglas de negocio).

**El arreglo.** Se agregó `CuentaCorriente(TipoCuenta tipo, double acuerdoDescubierto)` y `getAcuerdoDescubierto()`. El constructor original `(TipoCuenta)` delega al nuevo con el valor por defecto — sin cambios en el código existente.

### 11.15 `EmpleadoServicio` programa contra la interfaz: `ICrud<Empleado>`

**Qué pasaba.** El campo `empleadoDao` en `EmpleadoServicio` era `EmpleadoDao` (clase concreta). El servicio dependía de la implementación, no de la abstracción.

**El arreglo.** El campo y el constructor secundario se declaran como `ICrud<Empleado>`. El constructor principal sigue aceptando `EmpleadoDao` (que implementa `ICrud<Empleado>`) para el uso normal. Se puede inyectar cualquier implementación del contrato sin tocar el servicio.

**Lente defensa.** Programar contra interfaces es el Principio de Inversión de Dependencias aplicado a nivel de campo. Hace testable el servicio sin BD real.

### 11.16 Lo que se decidió NO arreglar (y por qué)

- **`EmpleadoDao`/`ClienteDao` duplicados (~85%)**: refactor estructural a días de la entrega; riesgo mayor que beneficio. Asumido en el Q&A de "¿qué cambiarías?".
- **N+1 al listar cuentas/tarjetas**: invisible con volúmenes de demo. La solución (JOIN) está identificada.
- **Passwords en texto plano**: fuera del alcance, documentado en 7.11 con el plan BCrypt.
- **Autorización a nivel servicio**: con una sola UI de escritorio la vista ya es la frontera de entrada.
- **`Double` para plata**: el dominio real usa `BigDecimal`; para los montos del sistema la precisión de `double` alcanza y es lo que enseña la cátedra con H2 `DOUBLE`.

### 11.17 Feature nueva: cheques (uso real de `permiteCheques()`)

**Por qué se agregó.** `Cuenta.permiteCheques()` (abstracto; `true` en `CuentaCorriente`, `false` en `CajaAhorro`) estaba declarado pero sin ningún caller. La feature de cheques le da uso real y demuestra varios conceptos juntos.

**Qué se agregó (slice vertical completo, mismo patrón que Tarjeta):**
- `entidades/Cheque.java` + enum `entidades/EstadoCheque.java` (`PENDIENTE`/`COBRADO`/`ANULADO`).
- `persistencia/ChequeDao.java` (`extends BaseH2 implements ICrud<Cheque>`), con la operación transaccional `cobrar`.
- `servicio/ChequeServicio.java` (`emitir`/`cobrar`/`anular`/`listarPorCuenta`) + `servicio/ChequeDuplicadoException.java`.
- `vista/FormularioCheque.java`, con botón "Cheques" en `MenuClienteView` y `MenuEmpleadoView`.
- Tabla `CHEQUES` (FK a `CUENTAS`) en `InicializadorBD`, y valor `CHEQUE_COBRADO` en el enum `TipoMovimiento`.

**Flujo en dos pasos:**
- **Emitir:** valida `cuenta.permiteCheques()` (despacho polimórfico; caja de ahorro → `OperacionNoPermitidaException`) y que el monto sea > 0 (`DatosInvalidosException`). Crea el cheque `PENDIENTE`. **No toca el saldo** (un cheque mueve plata recién al cobrarse).
- **Cobrar:** `cuenta.debitar(monto)` (reusa `saldoMinimo()` polimórfico, incluido el acuerdo de descubierto), inserta el `Movimiento(CHEQUE_COBRADO)` y marca `COBRADO`, todo en una `transaccionSql` (atómico). `setEstado(COBRADO)` ocurre solo si la persistencia tuvo éxito.
- **Anular:** `PENDIENTE → ANULADO`.

**Qué demuestra:** polimorfismo (`permiteCheques`, `saldoMinimo` vía `debitar`), Open/Closed (nuevo `TipoMovimiento` forzado por el compilador a implementar `signo()`), atomicidad transaccional reutilizando `transaccionSql`, y consistencia de capas.

**Detalle de integridad referencial:** como `CHEQUES` referencia a `CUENTAS`, se actualizó `borrarEnCascada` (en `ClienteDao` y `CuentaDao`) para borrar primero los cheques. Sin eso, borrar una cuenta con cheques violaría la foreign key.

### 11.18 Decisión: por qué NO se invirtió la navegación con colecciones

Se evaluó que `Cliente` tuviera `List<Cuenta>`/`List<Tarjeta>` y `Cuenta` tuviera `List<Movimiento>` (navegación top-down por objetos). **Se decidió mantener el diseño actual** (navegación por query con `leerPorTitular`/`leerPorCuenta`). El razonamiento completo está en el Q&A "¿Por qué la navegación es hijo→padre…?" de la sección 9: en DAO sin ORM la navegación idiomática es por query; cachear colecciones sin Unit of Work trae datos rancios, referencia circular con `titular` y doble vía de acceso; y el N+1 ya está resuelto en `leerPorTitular`. Lo que demuestra madurez de diseño no es agregar las colecciones, sino saber por qué acá no conviene.

---

## 12. Fragmentos de código clave para estudiar

Cada fragmento viene con anotaciones línea a línea y las preguntas más probables que puede hacerte el docente sobre ese código.

---

### 12.1 `Cuenta.debitar()` — encapsulamiento + polimorfismo + excepciones

```java
// src/entidades/Cuenta.java
public void debitar(Double monto) throws SaldoInsuficienteException {
    if (monto == null || monto <= 0) {
        throw new IllegalArgumentException("El monto tiene que ser mayor a cero");
    }
    if (saldo - monto < saldoMinimo()) {    // ← polimorfismo: llama al override de la subclase
        throw new SaldoInsuficienteException("Saldo insuficiente en la cuenta " + referencia());
    }
    this.saldo -= monto;
}
```

**Anotaciones:**
- `monto == null || monto <= 0` → guarda de argumento inválido. Lanza `IllegalArgumentException` (**unchecked**): es un bug del programador, no un caso de negocio.
- `saldoMinimo()` → llama al método abstracto de `Cuenta`. En runtime Java ejecuta el override de la subclase concreta (`CajaAhorro` devuelve `0.0`, `CuentaCorriente` devuelve `-50000.0`). `debitar` no sabe ni le importa de qué subclase es — eso es **polimorfismo**.
- `SaldoInsuficienteException` → **checked**, extiende `Exception`. El caller está obligado a manejarla porque "el cliente no tiene plata" es un caso de negocio esperable.
- `this.saldo -= monto` → el saldo solo cambia acá, nunca directo desde afuera. Eso es **encapsulamiento**: el invariante "el saldo no baja de `saldoMinimo()`" está protegido.

**Preguntas del docente:**
- "¿Por qué `IllegalArgumentException` es unchecked y `SaldoInsuficienteException` es checked?" → unchecked = bug del caller, checked = condición de negocio que el caller puede recuperar.
- "¿Qué pasa si mañana agregamos `CuentaSueldo` con saldoMinimo distinto?" → solo creamos `CuentaSueldo extends Cuenta` con su override. `debitar` no se toca. Open/Closed.

---

### 12.2 `CajaAhorro` y `CuentaCorriente` — herencia + polimorfismo

```java
// src/entidades/CajaAhorro.java
public class CajaAhorro extends Cuenta {
    public CajaAhorro(TipoCuenta tipo) { super(tipo); }

    @Override public double saldoMinimo()   { return 0.0;   }
    @Override public boolean permiteCheques() { return false; }
}

// src/entidades/CuentaCorriente.java
public class CuentaCorriente extends Cuenta {
    private static final double ACUERDO_POR_DEFECTO = 50000.0;
    private final double acuerdoDescubierto;

    public CuentaCorriente(TipoCuenta tipo) {
        this(tipo, ACUERDO_POR_DEFECTO);       // ← constructor delegante
    }
    public CuentaCorriente(TipoCuenta tipo, double acuerdoDescubierto) {
        super(tipo);
        this.acuerdoDescubierto = acuerdoDescubierto;
    }

    @Override public double saldoMinimo()     { return -acuerdoDescubierto; }
    @Override public boolean permiteCheques() { return true; }
}
```

**Anotaciones:**
- Ambas sobreescriben `saldoMinimo()` y `permiteCheques()`: el compilador **obliga** a hacerlo porque son abstractos en `Cuenta`.
- `ACUERDO_POR_DEFECTO` es `static final` (constante de clase, no de instancia). `acuerdoDescubierto` es `final` de instancia: se setea en el constructor y no cambia.
- El constructor de un parámetro delega en el de dos (`this(tipo, ACUERDO_POR_DEFECTO)`): un único punto de inicialización, sin duplicar la asignación.
- `saldoMinimo()` devuelve `-acuerdoDescubierto` (negativo): si el acuerdo es 50.000, el saldo puede bajar hasta -50.000.

**Preguntas del docente:**
- "¿Por qué `CajaAhorro` está casi vacía?" → porque toda la lógica compartida vive en `Cuenta`. Solo provee las respuestas concretas a los dos contratos abstractos.
- "¿Por qué `acuerdoDescubierto` es `final`?" → un descubierto acordado no cambia durante la vida de la cuenta (es parte de su identidad). `final` hace imposible reasignarlo por error.

---

### 12.3 `TipoMovimiento.signo()` — polimorfismo en enum + Open/Closed

```java
// src/entidades/TipoMovimiento.java
public enum TipoMovimiento {
    TRANSFERENCIA_ENVIADA  { @Override public int signo() { return -1; } },
    TRANSFERENCIA_RECIBIDA { @Override public int signo() { return  1; } },
    DEBITO_TARJETA         { @Override public int signo() { return -1; } },
    PAGO_TARJETA           { @Override public int signo() { return  1; } };

    public abstract int signo();
}

// Uso en ResumenView:
total += m.getMonto() * m.getTipo().signo();
```

**Anotaciones:**
- Cada constante del enum es esencialmente una subclase anónima que sobrescribe `signo()`. El enum puede tener métodos abstractos — el compilador exige que **todas** las constantes los implementen.
- `ResumenView` no tiene ningún `switch` ni `if`: pregunta al movimiento cuánto vale su signo, sin saber qué tipo es. **Tell, don't ask.**
- Si se agrega `COMISION` sin implementar `signo()`, el código **no compila**. El Open/Closed se cumple por construcción.

**Preguntas del docente:**
- "¿Puede un enum tener métodos abstractos en Java?" → sí, siempre que todas sus constantes los implementen. El compilador lo enforcea.
- "¿Por qué moviste esta lógica al enum y no la dejaste en la vista?" → la vista no debería saber si una transferencia enviada suma o resta; eso es semántica del dominio.

---

### 12.4 `TipoCuenta.nuevaCuenta()` — Factory Method en enum

```java
// src/entidades/TipoCuenta.java
public enum TipoCuenta {
    CAJA_AHORRO_PESOS(Moneda.PESOS) {
        @Override public Cuenta nuevaCuenta() { return new CajaAhorro(this); }
    },
    CAJA_AHORRO_DOLARES(Moneda.DOLARES) {
        @Override public Cuenta nuevaCuenta() { return new CajaAhorro(this); }
    },
    CUENTA_CORRIENTE(Moneda.PESOS) {
        @Override public Cuenta nuevaCuenta() { return new CuentaCorriente(this); }
    };

    private final Moneda moneda;
    TipoCuenta(Moneda moneda) { this.moneda = moneda; }
    public Moneda getMoneda() { return moneda; }
    public abstract Cuenta nuevaCuenta();
}

// Uso en FormularioCuenta y CuentaDao:
Cuenta cuenta = tipo.nuevaCuenta();   // ← ni FormularioCuenta ni CuentaDao saben qué subclase crean
```

**Anotaciones:**
- El enum mismo es el factory: cada valor sabe qué subclase de `Cuenta` le corresponde.
- Cada constante lleva su `Moneda` como dato y su `nuevaCuenta()` como comportamiento. Datos + comportamiento juntos: eso es encapsulamiento.
- Agregar `CUENTA_SUELDO` solo requiere una nueva constante con su `nuevaCuenta()`. El resto del sistema no cambia.

**Preguntas del docente:**
- "¿Por qué no hacés `new CajaAhorro()` directamente en `FormularioCuenta`?" → porque `FormularioCuenta` trabaja con `TipoCuenta` seleccionado por el usuario. Si hiciera `new` directamente, necesitaría un `switch(tipo)` que hay que actualizar cada vez que se agrega un tipo.

---

### 12.5 `ICrud<T>` — interfaz genérica + overloading + DIP

```java
// src/persistencia/ICrud.java
public interface ICrud<T> {
    void grabar(T t)           throws SQLException;
    T    leer(Integer id)      throws SQLException;   // overload 1
    List<T> leer()             throws SQLException;   // overload 2
    void modificar(T t)        throws SQLException;
    void borrar(Integer id)    throws SQLException;
}

// src/servicio/EmpleadoServicio.java
private final ICrud<Empleado> empleadoDao;            // ← tipo INTERFAZ, no clase concreta

public EmpleadoServicio(EmpleadoDao empleadoDao) {    // ← constructor de conveniencia
    this(empleadoDao, new UsuarioDao());
}
public EmpleadoServicio(ICrud<Empleado> empleadoDao, UsuarioDao usuarioDao) {
    this.empleadoDao = empleadoDao;                   // ← inyección real
    this.usuarioDao  = usuarioDao;
}
```

**Anotaciones:**
- `ICrud<T>` es una interfaz genérica: sin el parámetro `T`, `grabar(T)` sería `grabar(Object)` y habría que castear en todos lados.
- `leer(Integer id)` y `leer()` son **sobrecarga** (overloading): mismo nombre, distinta firma. Polimorfismo estático, resuelto en compilación.
- `private final ICrud<Empleado> empleadoDao` → el campo es del tipo de la interfaz. El servicio nunca llama métodos específicos de `EmpleadoDao`, solo los 5 del contrato. **Principio de Inversión de Dependencias.**
- El constructor de conveniencia acepta `EmpleadoDao` concreto (lo que usa producción), pero lo convierte internamente al tipo de interfaz al delegar al constructor más general (que aceptaría cualquier `ICrud<Empleado>`, incluyendo un mock de test).

**Preguntas del docente:**
- "¿Por qué `ICrud` es interfaz y no clase abstracta?" → porque es un contrato puro sin estado compartido. Los DAOs ya extienden `BaseH2` (herencia simple), así que la interfaz suma sin conflicto.
- "¿Para qué el segundo constructor que acepta `ICrud<Empleado>`?" → para testing: en un test podés pasar un DAO falso que no toca la BD real.

---

### 12.6 `FormularioUsuario<T>` — Template Method + genéricos

```java
// src/vista/FormularioUsuario.java (esquema)
public abstract class FormularioUsuario<T extends Usuario> {

    protected final void montarVentana() {         // ← final: el esqueleto es inviolable
        this.frame = new JFrame(tituloVentana());  // ← hook abstracto
        // ... arma paneles ...
        refrescarLista();                          // ← llama a listarTodos() abstracto
        frame.setVisible(true);
    }

    // Hooks que cada subclase implementa:
    protected abstract String  tituloVentana();
    protected abstract T       crearEntidadVacia();
    protected abstract List<T> listarTodos()                throws LeyendoException;
    protected abstract void    guardarNuevo(T entidad)      throws Exception;
    protected abstract void    guardarModificacion(T entidad) throws Exception;
    protected abstract void    eliminar(Integer id)         throws Exception;
    // ... más hooks ...
}

// FormularioEmpleado solo implementa los hooks:
public class FormularioEmpleado extends FormularioUsuario<Empleado> {
    @Override protected String  tituloVentana()          { return "Empleados"; }
    @Override protected Empleado crearEntidadVacia()     { return new Empleado(); }
    @Override protected List<Empleado> listarTodos()     { return empleadoServicio.listar(); }
    @Override protected void guardarNuevo(Empleado e)    { empleadoServicio.agregar(e); }
    // ...
}
```

**Anotaciones:**
- `<T extends Usuario>` → bounded wildcard: `T` puede ser cualquier subclase de `Usuario`. La lista `JList<T>` y los métodos ya están tipados a `Empleado` o `Cliente` sin casts.
- `final void montarVentana()` → el flujo del formulario (crear frame, armar paneles, refrescar lista, hacer visible) es fijo. Las subclases no pueden romperlo.
- Los métodos abstractos son los **hooks**: los únicos puntos donde cada formulario inyecta su comportamiento específico.
- `FormularioEmpleado` y `FormularioCliente` son cada uno ~80 líneas. Sin el Template Method serían ~280 líneas de código duplicado.

**Preguntas del docente:**
- "¿Cuál es la diferencia entre Template Method y Strategy?" → en Template Method el algoritmo vive en el padre y solo algunos pasos son personalizables (hooks). En Strategy el algoritmo completo es intercambiable como objeto externo.
- "¿Por qué `montarVentana()` es `protected` y no `public`?" → para que solo las subclases o clases del mismo paquete puedan llamarla. La forma correcta de usar un `FormularioEmpleado` es `new FormularioEmpleado()` — el constructor llama a `montarVentana()` internamente; el caller no necesita saberlo.

---

### 12.7 `LoginView.intentarLogin()` — polimorfismo de dispatch de menú

```java
// src/vista/LoginView.java
private void intentarLogin(String username, String password) {
    try {
        Usuario usuario = autenticacionServicio.autenticar(username, password);
        MenuView menu = usuario.crearMenu();   // ← polimorfismo: sin if, sin instanceof
        frame.dispose();
        menu.alCerrar(LoginView::new);
        menu.mostrar();
    } catch (AutenticacionException ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error de autenticación", JOptionPane.ERROR_MESSAGE);
    }
}
```

```java
// src/entidades/Empleado.java
@Override
public MenuView crearMenu() { return new MenuEmpleadoView(this); }

// src/entidades/Cliente.java
@Override
public MenuView crearMenu() { return new MenuClienteView(this); }
```

**Anotaciones:**
- `usuario.crearMenu()` — el JVM resuelve en runtime qué override ejecutar. `LoginView` no sabe si el usuario es `Empleado` o `Cliente`, y no necesita saberlo.
- `frame.dispose()` va **después** de resolver el menú: si `crearMenu()` lanzara una excepción, el frame seguiría vivo y el error podría mostrarse.
- `menu.alCerrar(LoginView::new)` → method reference: al cerrar el menú, se instancia un login nuevo. Ciclo de vida correcto.
- Si mañana se agrega un rol `Auditor`, solo se crea `Auditor.crearMenu()` — este método no cambia.

**Preguntas del docente:**
- "¿Por qué no usás `instanceof` acá?" → porque `instanceof` es exactamente el antipatrón que el polimorfismo reemplaza. Cada vez que agregás un rol, tendrías que tocar `LoginView`. Con polimorfismo, cada clase sabe lo que tiene que hacer.
- "¿`entidades` no está importando `vista`?" → sí, `Empleado` y `Cliente` importan `MenuView`. Es un tradeoff aceptado: el polimorfismo de dispatch pesa más que la pureza de capas en un proyecto cuyo objetivo es demostrar POO. La alternativa sería una interfaz `MenuFactory` en una capa intermedia, pero agrega complejidad innecesaria para la escala del TP.

---

### 12.8 `Cuenta.mismaMonedaQue()` — == en enums + null-safety

```java
// src/entidades/Cuenta.java
public boolean mismaMonedaQue(Cuenta otra) {
    return otra != null && getMoneda() == otra.getMoneda();
}

public Moneda getMoneda() {
    return tipo != null ? tipo.getMoneda() : null;
}
```

**Anotaciones:**
- `otra != null` → null-safety: si `otra` es `null`, devuelve `false` sin NPE.
- `getMoneda() == otra.getMoneda()` → `==` para enums es correcto y recomendado. Los enums son singletons: `Moneda.PESOS` es exactamente una instancia en la JVM. Comparar con `==` es equivalente a `.equals()` pero más eficiente.
- El método encapsula la comparación: quien llama no sabe cómo se determina la moneda (que viene del tipo de cuenta). **Tell, don't ask**.

**Preguntas del docente:**
- "¿Por qué `==` y no `.equals()`?" → porque `Moneda` es un enum; sus instancias son únicas, `==` y `.equals()` son equivalentes, pero `==` es el idioma Java para enums.

---

### 12.9 `Usuario.autenticaCon()` — encapsulamiento de comparación sensible

```java
// src/entidades/Usuario.java
public boolean autenticaCon(String password) {
    return this.password != null && this.password.equals(password);
}
```

**Anotaciones:**
- La comparación de contraseña ocurre **dentro del objeto**. El caller (servicio de autenticación) nunca ve la contraseña del usuario — le pregunta si la ingresada coincide y recibe un booleano.
- `this.password != null &&` → null-safety: si el usuario de BD tiene password nula (datos corruptos), devuelve `false` en lugar de NPE.
- Si en el futuro se agrega hashing (`BCrypt.checkpw(password, this.passwordHash)`), el cambio es solo en esta línea. El servicio de autenticación no se toca.

**Preguntas del docente:**
- "¿Cómo evitás que alguien lea la password del usuario?" → el campo `password` es `private` en `Usuario`. No hay `getPassword()` usado en autenticación; la comparación ocurre dentro del objeto. La capa que autentica solo llama `autenticaCon(ingresada)`.
- *(El docente puede notar que sí existe `getPassword()` en el código — se usa solo para precargar el formulario de edición. Si te lo señalan, reconocé el trade-off y decí cómo lo eliminarías: precargando con `****` en lugar del valor real.)*

---

### 12.10 Jerarquía de excepciones — los tres niveles en acción

```java
// NIVEL 1 — JDBC (lo lanza el driver, lo captura el DAO)
// EmpleadoDao.java
public void grabar(Empleado e) throws SQLException {
    updateDeleteInsertSql("INSERT INTO USUARIOS ...", ...);   // puede lanzar SQLException
}

// NIVEL 2 — Persistencia → Servicio (el servicio captura y traduce)
// EmpleadoServicio.java
public void agregar(Empleado e) throws GrabandoException, EmpleadoExistenteException {
    try {
        ...
        empleadoDao.grabar(e);
    } catch (SQLException ex) {
        throw new GrabandoException("Error al grabar el empleado: " + ex.getMessage());
        // ↑ La vista recibe GrabandoException, nunca SQLException cruda
    }
}

// NIVEL 3 — Servicio → Vista (excepción de dominio, la vista la muestra)
// FormularioEmpleado.java (heredado de FormularioUsuario)
catch (GrabandoException ex) {
    JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
}
```

**Anotaciones:**
- Cada capa habla el idioma de la capa que la consume. La vista no sabe SQL; recibe mensajes de negocio.
- `GrabandoException` y `LeyendoException` son **checked** (extienden `Exception`): el compilador obliga a la vista a manejarlas o declararlas.
- El mensaje de `GrabandoException` incluye `ex.getMessage()` para tener contexto en logs, pero el usuario solo ve el mensaje de nivel 2 ("Error al grabar...").

---

### 12.11 Composición vs. herencia — los dos patrones juntos

```java
// Herencia (es-un): Empleado ES un Usuario
public class Empleado extends Usuario { ... }
public class CajaAhorro extends Cuenta { ... }

// Composición (tiene-un): Cuenta TIENE un Cliente como titular
public abstract class Cuenta {
    protected Cliente titular;   // ← composición
}

// También composición: TarjetaDao TIENE un ClienteDao para resolver el titular
public class TarjetaDao extends BaseH2 implements ICrud<Tarjeta> {
    private final ClienteDao clienteDao;
    public TarjetaDao() { this.clienteDao = new ClienteDao(); }
}
```

**Regla:** usá herencia solo cuando la relación es "es-un" verificable en el dominio. Para "tiene-un", usá composición.

**Pregunta trampa:** "¿`TarjetaDao` hereda de `ClienteDao`?" → No, tiene uno. `TarjetaDao` no es un tipo de `ClienteDao`. Hereda de `BaseH2` (es un DAO que necesita acceso a H2) y contiene un `ClienteDao` para resolver el titular de cada tarjeta.

---

### 12.12 `protected` vs `private` — acceso controlado en la jerarquía

```java
// src/entidades/Usuario.java — campos PRIVATE
private Integer id;
private String username;
private String password;    // ← especialmente sensible

// src/entidades/Cuenta.java — campos PROTECTED
protected Double saldo;
protected Cliente titular;
protected final TipoCuenta tipo;
```

**¿Por qué la diferencia?**

- En `Usuario`: `Empleado` y `Cliente` no necesitan acceso directo a los campos; usan los getters/setters heredados. `private` es el nivel más restrictivo y correcto.
- En `Cuenta`: las subclases `CajaAhorro` y `CuentaCorriente` podrían necesitar leer `saldo` en implementaciones futuras de métodos propios. `protected` les da acceso sin exponerlos al mundo exterior.

**Nota:** `protected` más `setSaldo(Double)` público crea una tensión: código externo puede fijar el saldo sin pasar por `debitar()`. En la práctica está justificado porque el DAO necesita hidratar el objeto desde la BD, y el servicio necesita restaurar el snapshot en memoria si falla la transacción. Pero es un trade-off documentado — si te preguntan, reconocelo.

---

### 12.13 `BaseH2.transaccionSql()` + `TransferenciaDao` — atomicidad transaccional

Este es el fragmento más complejo del proyecto y el que más probablemente te van a preguntar. La infraestructura transaccional vive en la clase abstracta `BaseH2`:

```java
// src/persistencia/BaseH2.java
protected final void transaccionSql(String[] sqls, Object[][] params) throws SQLException {
    cargarDriver();
    obtenerConexion();
    try {
        connection.setAutoCommit(false);              // ← abre transacción
        for (int i = 0; i < sqls.length; i++) {
            PreparedStatement s = preparedStatement_v20(sqls[i], params[i]);
            s.executeUpdate();
            s.close();
        }
        connection.commit();                          // ← todo o nada: confirma
    } catch (SQLException e) {
        try {
            connection.rollback();                    // ← si algo falló, revierte TODO
        } catch (SQLException alRevertir) {
            e.addSuppressed(alRevertir);              // ← no perder la excepción original
        }
        throw e;
    } finally {
        cerrarConexion();                             // ← la conexión siempre se cierra
    }
}
```

Y el caller que la usa para una transferencia:

```java
// src/persistencia/TransferenciaDao.java
public void transferir(Integer idOrigen, Double saldoOrigen, Integer idDestino, Double saldoDestino,
        Movimiento enviado, Movimiento recibido) throws SQLException {
    String[] sqls = {
        SQL_ACTUALIZAR_SALDO,           // UPDATE saldo origen
        SQL_ACTUALIZAR_SALDO,           // UPDATE saldo destino
        MovimientoDao.SQL_INSERT,       // INSERT movimiento enviado
        MovimientoDao.SQL_INSERT        // INSERT movimiento recibido
    };
    Object[][] params = {
        { saldoOrigen, idOrigen },
        { saldoDestino, idDestino },
        movimientoDao.parametros(enviado),
        movimientoDao.parametros(recibido)
    };
    transaccionSql(sqls, params);       // ← los 4 statements en UNA transacción
}
```

**Anotaciones:**
- `setAutoCommit(false)` abre la transacción; `commit()` la confirma. Si cualquiera de los 4 statements lanza `SQLException`, el `catch` hace `rollback()` y la BD queda como estaba — **nunca** se debita el origen sin acreditar el destino.
- `e.addSuppressed(alRevertir)`: si el propio rollback falla, no perdemos la excepción original. Java permite adjuntar la secundaria.
- `finally { cerrarConexion(); }`: la conexión se cierra pase lo que pase — éxito, fallo de SQL, o fallo de rollback. Sin esto, una conexión se filtraría en cada error.
- `transaccionSql` es `protected final`: `protected` porque solo los DAOs (subclases) la usan; `final` porque ningún DAO debería redefinir cómo se maneja una transacción.

**Preguntas del docente:**
- "¿Qué pasa si se corta la luz justo después del primer UPDATE?" → como `autoCommit` está en `false` y todavía no hubo `commit()`, al reiniciar la BD descarta los cambios no confirmados. El saldo del origen no se tocó.
- "¿Por qué pasás los saldos ya calculados en vez de calcularlos acá?" → porque el cálculo (`cuenta.debitar()`, `cuenta.acreditar()`) es lógica de dominio y vive en la entidad. El DAO solo persiste. Separación de responsabilidades.

---

### 12.14 `UsuarioDao.mapear()` — herencia de tabla única (single-table inheritance)

Acá es donde el polimorfismo se encuentra con la persistencia. Las dos subclases de `Usuario` viven en **una sola tabla** `USUARIOS`, discriminadas por la columna `ROL`:

```java
// src/persistencia/UsuarioDao.java
private Usuario mapear(ResultSet rs) throws SQLException {
    Integer id = rs.getInt("ID");
    String username = rs.getString("USERNAME");
    String password = rs.getString("PASSWORD");
    String nombre = rs.getString("NOMBRE");
    String apellido = rs.getString("APELLIDO");
    String dni = rs.getString("DNI");
    String rol = rs.getString("ROL");                       // ← el discriminador
    if ("EMPLEADO".equals(rol)) {
        return new Empleado(id, username, password, nombre, apellido, dni);
    }
    if ("CLIENTE".equals(rol)) {
        return new Cliente(id, username, password, nombre, apellido, dni);
    }
    throw new SQLException("Rol desconocido en USUARIOS: " + rol);   // ← dato corrupto, no recuperable
}
```

**Anotaciones:**
- El método devuelve `Usuario` (el tipo padre) pero la instancia real es `Empleado` o `Cliente`. De ahí en adelante, `usuario.crearMenu()` (ver 12.7) usa polimorfismo para abrir el menú correcto. **El `mapear` es el único punto del sistema donde se decide el tipo concreto.**
- `"EMPLEADO".equals(rol)` y no `rol.equals("EMPLEADO")`: si `rol` fuera `null`, la primera forma devuelve `false` sin romper; la segunda tiraría `NullPointerException`. Es el idioma defensivo de Java.
- El `throw` final: si la BD tiene un ROL que no es ni EMPLEADO ni CLIENTE, es corrupción de datos. No es un error recuperable por el usuario → `SQLException`, no una excepción de dominio.

**Preguntas del docente:**
- "¿Por qué una sola tabla y no una por subclase?" → es la estrategia *single-table inheritance*. Con 6 campos compartidos y 0 campos propios por subclase, una tabla con discriminador es más simple que dos tablas idénticas o un JOIN. Si Empleado y Cliente tuvieran muchos campos distintos, reconsideraría.
- "¿Dónde está el `getRol()`?" → no existe. El ROL vive en la columna de la BD y lo lee el DAO. La subclase de Java **ya es** el discriminador en memoria (ver Q&A sobre getRol()).

---

### 12.15 `EmpleadoDao` — herencia + interfaz genérica + mapeo de `ResultSet`

Un DAO concreto combina dos mecanismos OOP a la vez: **hereda** de `BaseH2` (para reusar la infraestructura JDBC) e **implementa** `ICrud<Empleado>` (para cumplir el contrato genérico):

```java
// src/persistencia/EmpleadoDao.java
public class EmpleadoDao extends BaseH2 implements ICrud<Empleado> {

    private static final String COLS = "ID, USERNAME, PASSWORD, NOMBRE, APELLIDO, DNI";
    private static final String FILTRO_ROL = "ROL = 'EMPLEADO'";   // ← filtra SIEMPRE por rol

    @Override
    public List<Empleado> leer() throws SQLException {
        String sql = "SELECT " + COLS + " FROM USUARIOS WHERE " + FILTRO_ROL + " ORDER BY ID";
        ResultSet rs = selectSql(sql);                  // ← método heredado de BaseH2
        List<Empleado> lista = new ArrayList<>();
        try {
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        } finally {
            if (rs != null) rs.close();
            cerrarConexion();                           // ← también heredado
        }
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        return new Empleado(
            rs.getInt("ID"), rs.getString("USERNAME"), rs.getString("PASSWORD"),
            rs.getString("NOMBRE"), rs.getString("APELLIDO"), rs.getString("DNI")
        );
    }
}
```

**Anotaciones:**
- `extends BaseH2 implements ICrud<Empleado>`: herencia simple (una clase padre) + implementación de interfaz. Java permite una sola superclase pero N interfaces. `EmpleadoDao` reusa `selectSql`/`updateDeleteInsertSql`/`cerrarConexion` del padre, y promete los 5 métodos de `ICrud`.
- `FILTRO_ROL = "ROL = 'EMPLEADO'"` aplicado en **todas** las queries: aunque `USUARIOS` tiene empleados y clientes mezclados, `EmpleadoDao` solo ve empleados. `ClienteDao` hace lo mismo con `'CLIENTE'`. Es el reverso del discriminador de 12.14.
- `mapear` privado: cada DAO sabe armar su propia entidad desde el `ResultSet`. No se comparte porque cada uno construye un tipo distinto.

**Pregunta del docente:**
- "¿No duplicás SQL entre EmpleadoDao y ClienteDao?" → sí, ~85% del SQL es idéntico y solo cambia el rol. Lo reconozco. La mejora sería un `UsuarioDaoBase<T extends Usuario>` con el SQL parametrizado (ver sección 8.3). No lo hice por tiempo, no por no verlo.

---

### 12.16 `MenuView` — Template Method + ciclo de vida de ventanas

`MenuView` es el segundo uso de Template Method del proyecto (el primero es `FormularioUsuario`, ver 12.6). El constructor del padre arma el esqueleto y delega los huecos a las subclases:

```java
// src/vista/MenuView.java
protected MenuView(Usuario usuario) {
    this.usuario = usuario;
    this.frame = new JFrame(tituloVentana());           // ← hook abstracto
    frame.setLayout(new BorderLayout());
    JPanel root = new JPanel(new BorderLayout(MARGIN, MARGIN));
    root.add(crearEncabezado(), BorderLayout.NORTH);     // ← hook concreto (overridable)
    root.add(crearPanelOpciones(), BorderLayout.CENTER); // ← hook abstracto
    frame.add(root, BorderLayout.CENTER);
    // Al cerrar el menú (logout) no puede quedar ninguna ventana de la sesión abierta.
    frame.addWindowListener(new WindowAdapter() {
        @Override public void windowClosed(WindowEvent e) { cerrarVentanasHijas(); }
    });
}

protected void registrarVentana(JFrame ventana) {
    ventanasHijas.add(ventana);
    ventana.addWindowListener(new WindowAdapter() {
        @Override public void windowClosed(WindowEvent e) { ventanasHijas.remove(ventana); }
    });
}

protected abstract String tituloVentana();          // cada menú define su título
protected abstract JPanel crearPanelOpciones();      // cada menú define sus botones
```

**Anotaciones:**
- **Template Method**: el constructor (el "algoritmo") es fijo; `tituloVentana()` y `crearPanelOpciones()` son los pasos variables que `MenuEmpleadoView` y `MenuClienteView` rellenan. El padre controla el orden; el hijo solo aporta las piezas.
- `crearEncabezado()` es un hook **concreto** con default (muestra el nombre del usuario), redefinible. `crearPanelOpciones()` es **abstracto** (obligatorio). Esa es la diferencia entre un hook opcional y uno requerido.
- **Ciclo de vida**: cada ventana hija se registra en `ventanasHijas`. Cuando se cierra una hija, su listener `windowClosed` la saca de la lista (evita el memory leak de acumular ventanas muertas). Cuando se cierra el menú (logout), `cerrarVentanasHijas()` cierra todas las que quedaron abiertas. Sin esto, hacer logout dejaría formularios huérfanos abiertos.

**Pregunta del docente:**
- "¿Por qué llamás métodos abstractos desde el constructor?" → es el corazón del Template Method. Es seguro acá porque las subclases no leen estado propio aún no inicializado dentro de esos métodos — solo devuelven un título o construyen un panel con sus botones.

---

### 12.17 `EmpleadoServicio` — programar contra la interfaz (DIP a nivel de campo)

El servicio depende de la **abstracción** `ICrud<Empleado>`, no de la clase concreta `EmpleadoDao`:

```java
// src/servicio/EmpleadoServicio.java
public class EmpleadoServicio {

    private final ICrud<Empleado> empleadoDao;       // ← tipo de la INTERFAZ, no del DAO concreto
    private final UsuarioDao usuarioDao;

    public EmpleadoServicio(EmpleadoDao empleadoDao) {        // uso normal de la app
        this(empleadoDao, new UsuarioDao());
    }

    public EmpleadoServicio(ICrud<Empleado> empleadoDao, UsuarioDao usuarioDao) {  // inyectable
        this.empleadoDao = empleadoDao;
        this.usuarioDao = usuarioDao;
    }

    public List<Empleado> listar() throws LeyendoException {
        try {
            return empleadoDao.leer();               // ← solo usa métodos del contrato ICrud
        } catch (SQLException ex) {
            throw new LeyendoException("Error al listar empleados: " + ex.getMessage());
        }
    }
}
```

**Anotaciones:**
- El campo es `ICrud<Empleado>`. El servicio **no sabe ni le importa** si por debajo hay un DAO de H2, un mock de testing o una implementación en memoria. Eso es el **Principio de Inversión de Dependencias**: el módulo de alto nivel (servicio) depende de una abstracción, no de un detalle (el DAO concreto).
- El constructor principal acepta `EmpleadoDao` por comodidad (es lo que usa la app real); el secundario acepta cualquier `ICrud<Empleado>` para poder inyectar un doble de prueba.
- `catch (SQLException) → throw LeyendoException`: el servicio traduce la excepción técnica de bajo nivel (SQL) a una de dominio que la vista entiende. La vista nunca ve un `SQLException` crudo.

**Pregunta del docente:**
- "¿Para qué sirve declarar el campo como interfaz si siempre le pasás un EmpleadoDao?" → para testabilidad y para no acoplarme a la implementación. Si mañana quiero testear el servicio sin BD, le inyecto un `ICrud<Empleado>` falso que devuelve datos en memoria, sin tocar una línea del servicio.

---

### 12.18 `ClienteDao.borrarEnCascada()` — atomicidad en las bajas

Borrar un cliente implica borrar sus movimientos, cuentas y tarjetas. Si se hiciera en statements sueltos y fallara a mitad, quedaría un cliente fantasma o cuentas sin dueño. La solución: los 5 DELETEs en **una** transacción:

```java
// src/persistencia/ClienteDao.java
public void borrarEnCascada(Integer id) throws SQLException {
    String[] sqls = {
        "DELETE FROM MOVIMIENTOS WHERE ID_CUENTA IN (SELECT ID FROM CUENTAS WHERE ID_TITULAR = ?)",
        "DELETE FROM MOVIMIENTOS WHERE ID_TARJETA IN (SELECT ID FROM TARJETAS WHERE ID_TITULAR = ?)",
        "DELETE FROM CUENTAS WHERE ID_TITULAR = ?",
        "DELETE FROM TARJETAS WHERE ID_TITULAR = ?",
        "DELETE FROM USUARIOS WHERE ID = ? AND ROL = 'CLIENTE'"
    };
    Object[][] params = { { id }, { id }, { id }, { id }, { id } };
    transaccionSql(sqls, params);       // ← reusa la MISMA infraestructura de 12.13
}
```

**Anotaciones:**
- El **orden importa**: primero los movimientos (hijos de cuentas/tarjetas), después cuentas y tarjetas, al final el usuario. Si se borrara el usuario primero, las FK podrían quedar colgando.
- Reusa `transaccionSql` de `BaseH2` — la misma que la transferencia. Una sola implementación de "todo o nada" sirve para transferencias, débitos de tarjeta y borrados en cascada.
- `AND ROL = 'CLIENTE'` en el último DELETE: defensa extra para no borrar accidentalmente un empleado si llegara un id equivocado.

**Pregunta del docente:**
- "¿Por qué la responsabilidad de saber qué tablas dependen del cliente está en el DAO y no en el servicio?" → porque es conocimiento de la estructura de datos (qué FK apuntan a qué). El servicio coordina reglas de negocio; el DAO sabe el esquema. Si el servicio tuviera que llamar a `movimientoDao`, `cuentaDao` y `tarjetaDao` en orden, ese conocimiento del esquema se filtraría a la capa equivocada.

---

## Ejemplo de "speech" inicial de defensa

> El proyecto es un mini home banking en Java + Swing con persistencia local en H2. Modela 5 entidades persistidas: Usuario (con subclases Empleado y Cliente), Cuenta (con subclases CajaAhorro y CuentaCorriente), Tarjeta, Movimiento y Cheque.
>
> Está organizado en cuatro capas: entidades, persistencia, servicios y vistas, con dependencias unidireccionales de vista hacia entidades.
>
> Los conceptos de POO que aplico son: herencia para las dos jerarquías de subtipos, polimorfismo en cuatro puntos clave (el dispatch del menú según el tipo de usuario con crearMenu(), el cálculo del signo de cada movimiento desde el enum TipoMovimiento, el cálculo de saldo mínimo según el tipo de cuenta, y la instanciación de subclases desde el enum TipoCuenta), encapsulamiento de las reglas de dominio dentro de las propias entidades (por ejemplo, Cuenta.debitar valida el saldo mínimo con saldoMinimo() polimórfico), interfaces genéricas para el patrón DAO, y excepciones tipadas en tres niveles (SQL, infraestructura, dominio).
>
> El flujo típico es: el operador del banco se loguea, da de alta clientes, les abre cuentas y tarjetas, registra movimientos de tarjeta. El cliente accede con su propio usuario, ve sus cuentas, transfiere entre ellas, y consulta sus movimientos y tarjetas.
>
> La operación más compleja es la transferencia, que valida que las cuentas sean distintas y de la misma moneda, delega el débito y el crédito a las entidades, y persiste los dos saldos y los dos movimientos en una única transacción JDBC: si algo falla a mitad, la base hace rollback y el servicio restaura los saldos en memoria. La misma infraestructura transaccional se reutiliza en las otras operaciones multi-statement: débitos y pagos de tarjeta (saldos + movimiento), y todos los borrados en cascada (cliente elimina 5 tablas, cuenta y tarjeta eliminan sus movimientos junto con el registro principal) — o se borra todo, o no se borra nada.

(unos 250 palabras, te alcanza para 2 minutos hablados)
