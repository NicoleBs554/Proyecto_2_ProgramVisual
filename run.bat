@echo off
REM Compila y ejecuta la simulación

echo ON > nul
set PSQL_JAR=lib\postgresql.jar

if not exist "%PSQL_JAR%" (
    echo ERROR: no se encontro %PSQL_JAR%.
    echo Copia el driver JDBC de PostgreSQL en la carpeta lib y renoḿbralo o ajusta esta variable.
    echo (por ejemplo: postgresql-42.7.8.jar)
    exit /b 1
)

echo Compilando fuentes...
javac -cp "%PSQL_JAR%" -d . src\*.java
if errorlevel 1 (
    echo Compilacion fallida.
    exit /b 1
)

echo Limpiando log anterior...
if exist simulation.log (
    del /f /q simulation.log
)

echo Ejecutando con classpath: ".;%PSQL_JAR%"
java -cp ".;%PSQL_JAR%" Main
