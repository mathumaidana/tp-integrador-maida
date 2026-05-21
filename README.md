# Mini Home Banking (TP Integrador) - Universidad de Palermo - Matheo Maidana

Aplicación de escritorio en Java con Swing. Base de datos H2 en carpeta `data/`.

## Requisitos

- JDK 8
- JAR de H2 en `lib/h2-2.2.224.jar`

Descarga del driver (desde la raíz del proyecto):

```bash
curl -sSL -o lib/h2-2.2.224.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

```bash
wget -q -O lib/h2-2.2.224.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

## Cómo ejecutar

Abrí el proyecto desde su carpeta raíz y ejecutá `dao.Main` (en el IDE: `src/dao/Main.java`).

Linux / macOS:

```bash
cd tp-integrador-maida
mkdir -p bin
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $(find src -name "*.java")
java -cp "bin:lib/*" dao.Main
```

Windows (PowerShell):

```powershell
cd tp-integrador-maida
mkdir bin -Force
$files = @(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -source 8 -target 8 -encoding UTF-8 -d bin -cp "lib/*" $files
java -cp "bin;lib/*" dao.Main
```

## Acceso

Usuario administrador por defecto: **admin** / **admin**.

Roles:

- **Administrador**: alta y baja de empleados. Ve los movimientos del banco a modo de auditoría.
- **Empleado**: alta y modificación de clientes, cuentas y tarjetas.
- **Cliente**: opera sus propias cuentas, transferencias y tarjetas.

Para empezar a operar entrá como `admin`, creá un empleado, después logueate como ese empleado y desde ahí cargá los clientes y sus productos.
