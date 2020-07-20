IntelliJavier DECAF IDE
==============

Plantilla basica para una aplicacion basica en Vaadin que unicamente necesita un contenedor Servlet 3.0 para su ejecucion.

Workflow
========

Para compilar el proyecto, ejecutar en la consola "mvn install".

Para ejecutar la aplicacion, escribir en consola"mvn jetty:run" y abrir en el navegador http://localhost:8080/ .

Para hacer un deployable en modo WAR:
- Cambiar a productionMode a true en la configuracion de clase servlet (ubicado en la clase de UI)
- Ejecutar "mvn clean package"
- Probar el archivo WAR con el comando "mvn jetty:run-war"

Compilacion del lado del cliente
-------------------------

El proyecto maven generado está utilizando un conjunto de widgets generado automáticamente de forma predeterminada.
Cuando agrega una dependencia que necesita compilación del lado del cliente, el complemento Maven
generarlo automáticamente. Sus propias personalizaciones del lado del cliente se pueden agregar a
paquete "cliente".

Debugging codigo del lado de cliente
  - Ejecutar "mvn vaadin:run-codeserver" en una consola separa mientras la aplicacion esta corriendo
  - Activar Super Dev Mode en la ventana de debug de la aplicacion

Desarrollando un tema utilizando el compilador en runtime
-------------------------

Al desarrollar el tema, Vaadin se puede configurar para compilar el tema SASS en tiempo de ejecución en el servidor. De esta manera, solo puede modificar los archivos scss en
su IDE y vuelva a cargar el navegador para ver los cambios.

Para usar la compilación de tiempo de ejecución, abra pom.xml y comente el tema de compilación objetivo de la configuración vaadin-maven-plugin. Para eliminar un posible existente tema precompilado, ejecute "mvn clean package" una vez.

Al usar el compilador en runtime, ejecutar la aplicación en el modo "run"
(en lugar de en modo "debug") puede acelerar las compilaciones de temas consecutivos
significativamente.

Se recomienda deshabilitar la compilación en tiempo de ejecución para los archivos WAR de producción.

Utilizando los pre-realease de Vaadin
-------------------------

Si  Vaadin pre-releases no esta activada por default, utilizar los parametros de Maven
"-P vaadin-prerelease" o cambiar el valor de activacion por default en pom.xml .
