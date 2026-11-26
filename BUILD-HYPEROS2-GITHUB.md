# Compilar e instalar HyperIsland para HyperOS 2

Este repositorio contiene el port en desarrollo para HyperOS 2 / Android 15. No necesitas crear otro repositorio ni copiar el proyecto a una carpeta nueva.

## Compilar el APK

1. Abre la pestaña **Actions** de este repositorio.
2. Selecciona **HyperIsland HyperOS 2 APK**.
3. Si GitHub solicita habilitar Actions, habilítalo.
4. Pulsa **Run workflow** y selecciona la rama `hos2/compatibility-audit`.
5. Espera a que todos los pasos terminen en verde.
6. En esa ejecución, abre **Artifacts** y descarga `HyperIsland-HyperOS2-APK`.

También se inicia una compilación automáticamente al subir cambios a una rama `hos2/**`.

El workflow genera la variante `releaseFast`, valida el APK y lo publica como artefacto. Si no se configura una clave propia, Android Gradle Plugin usa una firma debug: sirve para pruebas, no para distribuir como versión final. No subas keystores ni contraseñas al repositorio.

## Instalación y activación

1. Instala el APK descargado en el Redmi Note 13 4G.
2. Abre Vector y activa HyperIsland como módulo.
3. Comprueba que el ámbito del módulo incluya los procesos que declara el proyecto; después reinicia SystemUI o reinicia el teléfono si Vector lo solicita.
4. Abre HyperIsland una vez para inicializar sus preferencias.
5. Para probar el fallback de HyperOS 2, concede **Acceso a notificaciones** y **Mostrar sobre otras aplicaciones** a HyperIsland en Ajustes. Son permisos distintos y ambos son necesarios para la isla flotante de respaldo.

No desinstales ni reemplaces SystemUI, MIUISystemUIPlugin ni otros APK del sistema.

## Qué significa que compile

Una compilación correcta solo confirma que Gradle pudo generar el APK. No demuestra que los hooks nativos de HyperOS 3 existan en HyperOS 2 ni que funcionen en el dispositivo. El port incluye una sonda de clases nativas y un fallback basado en APIs públicas de Android; el comportamiento real debe verificarse en el teléfono.

Si falla la compilación, abre el run de Actions y revisa el primer error de Gradle. Comparte el enlace del run o el texto del error completo, no solo el mensaje final de fallo.
