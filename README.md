# Proyecto 2 - Pool de Conexiones (Java + JDBC)

Este proyecto es un simulador de conexiones a PostgreSQL para comparar dos enfoques:

- **Raw**: cada petición abre y cierra una conexión.
- **Pooled**: las conexiones se reutilizan desde un pool limitado.

Las métricas se guardan en `simulation.log` y también se muestran en consola.

## Configuración

- El archivo `.ENV` en la raíz define los parámetros de conexión:
  ```properties
  DB_HOST=localhost
  DB_PORT=5433
  DB_NAME=javaprueba
  DB_USER=postgres
  DB_PASSWORD=nicole10
  POOL_SIZE=50
  POOL_TIMEOUT=3000
  ```

- El archivo `sim.properties` controla la simulación:
  ```properties
  query=SELECT * FROM usuario LIMIT 1
  samples=20
  retries=3
  ```

Ajusta `samples` y `retries` según tus necesidades.

## Compilar y ejecutar en Windows

1. Coloca el driver JDBC de PostgreSQL (por ejemplo `postgresql-42.5.0.jar`) en `lib\\` y renómbralo `postgresql.jar` o actualiza las rutas en los `*.bat`.
2. En PowerShell o cmd:
   ```powershell
   .\\build.bat
   .\\run.bat
   ```

Esto generará el archivo `simulation.log` con cada muestra (fecha, id, éxito/fallo, mensaje) y mostrará un resumen de métricas al terminar.

## Métricas calculadas

- Tiempo total por simulación (raw y pooled).
- Cantidad y porcentaje de muestras exitosas/fallidas.
- Promedio de reintentos por muestra.

## Funcionalidades adicionales

- Hilos concurrentes que inician al mismo tiempo (uso de `CountDownLatch`).
- Freno manual: presiona **Enter** en la consola para detener la simulación rápidamente.
- Configuración externa (`.ENV` y `sim.properties`).

## Ejemplo de iteraciones (opcional)

El código se puede extender para variar el número de muestras iterativamente del 100 al designado; simplemente llama al método de `Cliente` en un bucle.

---

_Disponible el 27/02/2026, desarrollado en Java 17._
