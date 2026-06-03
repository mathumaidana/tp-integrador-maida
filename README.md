# Mini Home Banking — TP Integrador POO

Trabajo Práctico Integrador de Programación Orientada a Objetos (Universidad de Palermo). Implementa el **Tema 2: Mini home banking** del enunciado: aplicación de escritorio en Java + Swing con persistencia local en una base H2 embebida.

Autor: Matheo Maidana.

## Funcionalidad

- Administración de usuarios con dos perfiles:
  - **Empleado** (el empleado del banco mencionado en la consigna): ABM de otros empleados (sin poder borrarse a sí mismo), ABM de clientes, cuentas y tarjetas, registración de débitos y pagos de tarjeta, y vista de auditoría sobre todos los movimientos.
  - **Cliente**: ve sus cuentas con saldo, opera transferencias, consulta sus movimientos filtrables por mes y ve sus tarjetas con resumen mensual.
- Cuentas en tres modalidades: caja de ahorro en pesos, caja de ahorro en dólares y cuenta corriente (con acuerdo de giro al descubierto fijo de $50.000). El saldo inicial se valida contra el mínimo del tipo de cuenta.
- Transferencias entre cuentas: la cuenta origen se elige de una lista propia; la destino se busca por id, CBU o alias. Se validan misma moneda, distintas cuentas y saldo suficiente. El saldo del destino nunca se muestra.
- Tarjetas con disponible y saldo a pagar. Solo el empleado registra débitos y pagos. Cada tarjeta tiene resumen mensual seleccionando un mes (débitos y pagos separados).
- Movimientos: créditos, débitos, transferencias enviadas y recibidas, débito y pago de tarjeta. Resumen filtrable por mes con balance del período.

## Estructura del proyecto

```
src/
├── dao/Main.java        Punto de entrada (inicializa H2 y abre el login).
├── entidades/           Modelo de dominio.
│   ├── Usuario (abstract) → Empleado, Cliente
│   ├── Cuenta  (abstract) → CajaAhorro, CuentaCorriente
│   ├── Tarjeta, Movimiento
│   └── TipoCuenta, Moneda, TipoMovimiento (enums)
├── persistencia/        Capa JDBC sobre H2 (BaseH2, DAOs, InicializadorBD).
├── servicio/            Reglas de negocio y excepciones de dominio.
└── vista/               Pantallas Swing (login, menús por rol, ABMs, transferencias, resumen).
```

Las **cuatro entidades persistidas** del modelo son `Usuario`, `Cuenta`, `Tarjeta` y `Movimiento` (la consigna pide entre 4 y 6). El resto del paquete `entidades` son subclases o enums que refinan esas entidades vía herencia/polimorfismo.

## Requisitos

- JDK 8 o superior.
- Driver de H2 en `lib/h2-2.2.224.jar` (es un Multi-Release JAR compilado contra Java 8, así que funciona con cualquier JDK 8+).

Descargar el driver desde la raíz del proyecto si no está:

```bash
curl -sSL -o lib/h2-2.2.224.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

## Compilar y ejecutar

Linux / macOS:

```bash
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $(find src -name "*.java")
java -cp "bin:lib/*" dao.Main
```

Windows (PowerShell):

```powershell
$files = (Get-ChildItem -Path src -Recurse -Filter *.java).FullName
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $files
java -cp "bin;lib/*" dao.Main
```

La base se persiste en `./data/banco.mv.db`. Para arrancar de cero, borrá la carpeta `data/`.

## Acceso

Empleado inicial: **admin** / **admin**. Lo carga `InicializadorBD` la primera vez que se ejecuta.

Flujo sugerido para una demo:

1. Entrá como `admin` (el empleado inicial). Opcionalmente creá otros empleados desde "Empleados".
2. Desde el mismo menú, cargá clientes y abriles cuentas (caja de ahorro o cuenta corriente) y tarjetas.
3. Cerrá sesión y entrá como cliente para operar: transferencias, movimientos y tarjetas.

## Persistencia

La infraestructura JDBC (`BaseH2`, patrón DAO con `ICrud<T>`, excepciones `GrabandoException` y `LeyendoException`) está basada en el template "Archivos y BD Swing" entregado por la cátedra, adaptado al dominio del TP. El esquema de tablas, las relaciones y el mapeo polimórfico (`Usuario` por rol, `Cuenta` por tipo) son propios del proyecto. No hay migraciones: las tablas se crean con `CREATE TABLE IF NOT EXISTS` y, para reiniciar, se borra la carpeta `data/`.
