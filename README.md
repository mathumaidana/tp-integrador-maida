# TP Integrador - Tema 2: Mini Home Banking

Sistema de home banking de escritorio implementado en **Java + Swing + H2** para la materia POO de la Universidad de Palermo.

## Estructura del proyecto

```
  tp-integrador-maida/
  .vscode/settings.json    # igual que ajedrez-maida: src, bin, lib/**/*.jar
  lib/                     # JAR H2 (no versionado; ver descarga abajo)
  data/                    # BD H2 local (no versionada; se genera al correr)
  src/
    dao/Main.java          # punto de entrada
    entidades/             # Usuario, Cliente, Administrador, Cuenta, Movimiento, Tarjeta + enums
    persistencia/          # BaseH2, ICrud<T>, InicializadorBD, *Dao
    servicio/              # *Servicio + excepciones de dominio
    vista/                 # LoginView, Menu*, Formulario*, ResumenView
  bin/                     # compilados (ignorado)
```

## Modelo de dominio

6 entidades de dominio, todas relacionadas por atributos del tipo objeto (no por IDs sueltos):

- `Usuario` (abstracta) → `Cliente`, `Administrador` (herencia)
- `Cliente` posee `List<Cuenta>` y `List<Tarjeta>` (asociación)
- `Cuenta` referencia a su `Cliente titular` y posee `List<Movimiento>`
- `Tarjeta` referencia a su `Cliente titular`

Los tipos de cuenta (caja de ahorro pesos, caja de ahorro dólares, cuenta corriente) se modelan vía el enum `TipoCuenta`. Los enums no cuentan como entidades.

## Requisitos

- **JDK 8** (misma línea que `ajedrez-maida`: compilá y ejecutá con la misma versión para evitar `UnsupportedClassVersionError`).
- VSCode o Cursor con **Extension Pack for Java** (opcional; la config en `.vscode/settings.json` es la misma estructura que en `ajedrez-maida`).
- Driver H2 en `lib/h2-2.2.224.jar` (no se sube al repo; está en `.gitignore`).

### Descargar el driver H2

Desde la raíz del proyecto:

```bash
curl -sSL -o lib/h2-2.2.224.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

```bash
wget -q -O lib/h2-2.2.224.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

## Ejecución

Ejecutá siempre desde la **raíz** del proyecto (`tp-integrador-maida/`), para que la URL `./data/banco` apunte a la carpeta correcta.

### Desde VSCode / Cursor

Abrir la carpeta del proyecto y ejecutar `src/dao/Main.java` con `Run`. El classpath de `lib/**/*.jar` lo toma `.vscode/settings.json` (mismas claves que `ajedrez-maida`: `java.project.sourcePaths`, `outputPath`, `referencedLibraries`).

### Desde línea de comandos (Linux / macOS)

```bash
cd tp-integrador-maida
mkdir -p bin
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $(find src -name "*.java")
java -cp "bin:lib/*" dao.Main
```

En **Windows** (PowerShell), el separador del classpath es `;`:

```powershell
cd tp-integrador-maida
mkdir bin -Force
$files = @(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $files
java -cp "bin;lib/*" dao.Main
```

## Credenciales iniciales

Al primer arranque se crea automáticamente el usuario administrador:

- **Usuario:** `admin`
- **Contraseña:** `admin`

Desde el menú del administrador se da de alta a los clientes (CRUD completo). Cada cliente luego puede iniciar sesión con su propio usuario y contraseña.

## Alcance por entrega

### Entrega 1 (segundo parcial)

- Las 6 entidades creadas con sus atributos y relaciones.
- **CRUD completo de `Cliente`** desde Swing (alta, lectura por id, listado, modificación, baja) con manejo de excepciones (`ClienteExistenteException`, `ClienteInexistenteException`, `CampoVacioException`, `GrabandoException`, `LeyendoException`).
- Login funcional con discriminación de rol (admin vs cliente).
- Esqueleto navegable de los demás formularios (cuentas, transferencias, tarjetas, resumen).

### Entrega final (defensa)

- Transferencias entre cuentas con débito/crédito atómico (selección por id/cbu/alias).
- Movimientos automáticos en operaciones bancarias.
- Emisión de tarjetas y registro de débitos por el administrador, resumen mensual.
- Reportes y bonus (intereses, archivos, auditoría).

## Excepciones del dominio

- `GrabandoException` / `LeyendoException` — errores de persistencia.
- `ClienteExistenteException` / `ClienteInexistenteException` — validaciones de negocio del CRUD.
- `SaldoInsuficienteException` — operaciones que requieren saldo.
- `AutenticacionException` — login.
- `CampoVacioException` (en `vista`) — validación de formulario.

## Persistencia

H2 en modo file: `jdbc:h2:./data/banco`. La carpeta `data/` se crea al arrancar y la BD persiste entre ejecuciones. `InicializadorBD.crear()` ejecuta `CREATE TABLE IF NOT EXISTS` y siembra el usuario admin si todavía no existe.
