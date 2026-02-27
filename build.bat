@echo off
REM Compila todos los archivos java en src
REM Ajusta PSQL_JAR a la ruta del driver de PostgreSQL en tu sistema
set PSQL_JAR=lib\postgresql.jar  REM por ejemplo postgresql-42.7.8.jar renombrado a postgresql.jar

if not exist "%PSQL_JAR%" (
    echo WARNING: no se encontro %PSQL_JAR%. La compilación se hará sin él, pero la ejecución fallará si falta el driver.
    javac src\*.java
) else (
    javac -cp ".;%PSQL_JAR%" src\*.java
)
if errorlevel 1 (
    echo Compilación fallida.
    exit /b 1
)

echo Compilación completada.
