# PowerShell script to compile and run the simulation
$PSQL_JAR = "postgresql.jar"
if (-not (Test-Path $PSQL_JAR)) {
    Write-Error "ERROR: no se encontró $PSQL_JAR. Copia el driver JDBC de PostgreSQL en la carpeta raíz y renómbralo o ajusta esta variable."
    exit 1
}

Write-Host "Compilando fuentes..."
javac -cp $PSQL_JAR -d . src\*.java
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilación fallida."
    exit 1
}

Write-Host "Limpiando log anterior..."
if (Test-Path "simulation.log") { Remove-Item -Force "simulation.log" }

# aceptar parámetro de modo: RAW, POOL o BOTH
$mode = $args[0]
Write-Host "Ejecutando con classpath: .;$PSQL_JAR"
Write-Host "Modo: $mode"
java -cp ".;$PSQL_JAR" Main $mode

# para abrir la interfaz gráfica en su lugar:
# java -cp ".;$PSQL_JAR" SimulatorUI

