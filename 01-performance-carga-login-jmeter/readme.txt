EJERCICIO 1 - PRUEBA DE CARGA DEL SERVICIO DE LOGIN
====================================================

Prueba de carga con Apache JMeter del servicio de login del enunciado:

  POST https://fakestoreapi.com/auth/login
  Body: {"username": "...", "password": "..."}

Los usuarios y contraseñas se leen del archivo data/users.csv (cabecera user,passwd). La prueba
sostiene más de 20 TPS y se valida contra los criterios del enunciado:

  - Al menos 20 TPS.
  - Tiempo de respuesta máximo de 1,5 segundos.
  - Tasa de error menor al 3 % del total de peticiones.


1. TECNOLOGÍAS Y VERSIONES
--------------------------
  - Apache JMeter 5.6.3 (zip oficial de https://jmeter.apache.org), sin plugins adicionales.
  - Java JDK 17 (probado con Eclipse Temurin 17.0.19). JMeter necesita Java 8 o superior.
  - Groovy 3.0.20, el que trae JMeter en su carpeta lib/; lo usa el script de evaluación de
    criterios. No hay que instalarlo aparte.
  - Probado en Windows 11; los comandos para Linux y macOS son equivalentes.


2. INSTALACIÓN (UNA SOLA VEZ)
-----------------------------
  1) Instalar el JDK 17 y verificar que "java -version" responde.

  2) Descargar apache-jmeter-5.6.3.zip de https://jmeter.apache.org/download_jmeter.cgi y
     descomprimirlo, por ejemplo en C:\tools\apache-jmeter-5.6.3.

  3) Definir la variable JMETER_HOME y agregar su carpeta bin al PATH:

       Windows (PowerShell):
         [Environment]::SetEnvironmentVariable("JMETER_HOME", "C:\tools\apache-jmeter-5.6.3", "User")
         [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\apache-jmeter-5.6.3\bin", "User")

       Linux o macOS (en ~/.bashrc o ~/.zshrc):
         export JMETER_HOME=/opt/apache-jmeter-5.6.3
         export PATH="$PATH:$JMETER_HOME/bin"

  4) Abrir una terminal nueva y verificar con "jmeter -v" que la versión es 5.6.3.


3. EJECUCIÓN PASO A PASO
------------------------
  Todos los comandos se ejecutan desde esta carpeta (01-performance-carga-login-jmeter).

  1) Prueba smoke: 1 usuario virtual y 5 peticiones, una por cada fila del CSV. Sirve para
     comprobar que el script y el servicio responden antes de generar carga.

       jmeter -n -t scripts/login-load-test.jmx -q config/test.properties -Jthreads=1 -Jloops=5 -l resultados/smoke.jtl -j resultados/smoke.log -f

     Resultado esperado en la consola: "summary = 5 ... Err: 0 (0.00%)".

  2) Prueba de carga oficial: 22 TPS objetivo, 30 s de arranque gradual y 5 minutos sostenidos
     (330 s en total), con el dashboard HTML de JMeter al terminar. Tarda unos 6 minutos.

       jmeter -n -t scripts/login-load-test.jmx -q config/test.properties -Jtps=22 -Jrampup=30 -Jduration=330 -Jthreads=50 -l resultados/carga.jtl -j resultados/carga.log -e -o reportes/dashboard -f

     La consola muestra cada 30 segundos las peticiones, el promedio de TPS, los tiempos y los
     errores acumulados.

  3) Evaluar los criterios del enunciado sobre el resultado:

       Windows (PowerShell):
         java -cp "$env:JMETER_HOME\lib\*" groovy.ui.GroovyMain scripts/evaluate-criteria.groovy reportes/dashboard/statistics.json reportes/criterios.txt

       Linux o macOS:
         java -cp "$JMETER_HOME/lib/*" groovy.ui.GroovyMain scripts/evaluate-criteria.groovy reportes/dashboard/statistics.json reportes/criterios.txt

     Muestra el valor y el resultado (CUMPLE / NO CUMPLE) de cada criterio, lo guarda en
     reportes/criterios.txt y termina con código 0 si se cumplen todos o 1 si alguno falla.
     La consola de Windows puede mostrar mal las tildes; el archivo criterios.txt queda en UTF-8.

  4) Revisar los resultados:
       - reportes/criterios.txt           Criterios del enunciado con su valor y resultado.
       - reportes/dashboard/index.html    Dashboard de JMeter (se abre en el navegador).

  Notas:
    - La opción -f borra los resultados y el dashboard de una ejecución anterior.
    - Para ver o editar el plan en la interfaz gráfica: jmeter -t scripts/login-load-test.jmx
      La interfaz gráfica es solo para construir el plan; la carga se ejecuta en modo consola
      (-n), como recomienda Apache JMeter.
    - Para regenerar el dashboard a partir de un .jtl existente:
        jmeter -g resultados/carga.jtl -o reportes/dashboard-nuevo -q config/test.properties


4. PARÁMETROS DE LA PRUEBA
--------------------------
  Se cambian con -J en la línea de comandos, sin editar el plan:

    Parámetro   Por defecto   Uso
    tps         22            Peticiones por segundo objetivo (10 % por encima de los 20 TPS pedidos).
    rampup      30            Segundos en los que arrancan los usuarios virtuales.
    duration    330           Duración total en segundos (30 de arranque y 300 sostenidos).
    threads     50            Usuarios virtuales disponibles para sostener la tasa.
    loops       -1            Iteraciones por usuario (-1 = sin límite; manda la duración).


5. CÓMO ESTÁ CONSTRUIDO EL PLAN (scripts/login-load-test.jmx)
--------------------------------------------------------------
  - Thread Group "Login users": usuarios virtuales, arranque gradual y duración.
  - Precise Throughput Timer "Target throughput": fija la tasa en "tps" peticiones por segundo,
    con llegadas aleatorias.
  - CSV Data Set Config "Users from CSV": lee data/users.csv; las variables ${user} y ${passwd}
    toman el nombre de la cabecera. Recicla el archivo al llegar al final.
  - HTTP Request "POST /auth/login": el mismo request del curl del enunciado, con el header
    Content-Type: application/json, timeouts de 60 s (--max-time 60) y seguimiento de
    redirecciones (--location).
  - Response Assertion "Status is 201": el servicio responde 201 cuando el login es correcto.
  - JSON Assertion "Token is not empty": la respuesta trae el token en el campo token.
  - Sin listeners de visualización: consumen memoria durante la carga. Los resultados van al
    archivo .jtl y al dashboard.

  config/test.properties fija el umbral APDEX del dashboard en 1.500 ms (el máximo del
  enunciado), muestra los gráficos por segundo y guarda en el .jtl el mensaje de cada error.


6. RESULTADOS ENTREGADOS
------------------------
  Corrida oficial del 24 de octubre de 2026, de 08:04 a 08:10 (hora de Colombia, UTC-5):

    - resultados/carga.jtl             Datos crudos de las 7.262 peticiones.
    - resultados/smoke.jtl             Prueba smoke previa (5 peticiones).
    - reportes/dashboard/index.html    Dashboard HTML de JMeter.
    - reportes/criterios.txt           Evaluación de los criterios: los tres se cumplen.
    - evidencias/consola-carga.txt     Salida de la consola durante la corrida.
    - evidencias/*.png                 Capturas del dashboard: resumen, peticiones por segundo y
                                       tiempos de respuesta.

  El análisis de los resultados está en conclusiones.txt.


7. ESTRUCTURA
-------------
  scripts/login-load-test.jmx        Plan de prueba de JMeter.
  scripts/evaluate-criteria.groovy   Evaluación automática de los criterios del enunciado.
  data/users.csv                     Usuarios y contraseñas del enunciado.
  config/test.properties             Propiedades del dashboard y de los resultados.
  resultados/                        Archivos .jtl de las ejecuciones.
  reportes/                          Dashboard HTML y criterios.txt.
  evidencias/                        Salida de la consola y capturas del dashboard.
