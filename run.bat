@echo off
REM Ejecuta la simulación compilada
set PSQL_JAR=lib\postgresql.jar

if not exist "%PSQL_JAR%" (
    echo ERROR: no se encontro %PSQL_JAR%. Copia el driver JDBC de PostgreSQL (por ejemplo postgresql-42.7.8.jar) en la carpeta lib y renómbralo o ajusta esta variable.
    exit /b 1
)

java -cp ".;%PSQL_JAR%" Main
