# Estado del port HyperIsland para HyperOS 2

## Dispositivo objetivo

- Redmi Note 13 4G (`sapphire`)
- HyperOS 2 Global, Android 15
- KernelSU para root
- Vector (runtime compatible con libxposed) para cargar el módulo Xposed

## Arquitectura actual

- Se conserva el punto de entrada original `HyperIslandModule` y el API `io.github.libxposed.api`.
- `HyperOS2CompatibilityProbe` inspecciona, sin modificar el sistema, si el classloader expone las clases dinámicas nativas conocidas de HyperOS 3.
- El servicio `Hos2IslandListenerService` implementa un fallback visual independiente usando `NotificationListenerService` y `TYPE_APPLICATION_OVERLAY`.
- El fallback necesita que el usuario conceda Acceso a notificaciones y Mostrar sobre otras aplicaciones.
- KernelSU aporta root, pero no sustituye el runtime libxposed ni ejecuta por sí mismo los hooks.

## Límites conocidos

- La presencia de las clases nativas de HyperOS 3 no está confirmada en HyperOS 2. La sonda solo informa su disponibilidad; no prueba compatibilidad de métodos, firmas ni comportamiento.
- Los hooks originales se diseñaron para la estructura de SystemUI de HyperOS 3/4. Su compatibilidad con HyperOS 2 debe comprobarse hook por hook; una compilación correcta no basta.
- El fallback es una ventana flotante de notificaciones, no una integración nativa con SystemUI. No reproduce todas las animaciones, controles multimedia ni eventos especiales de la isla original.
- No se reemplazan APK del sistema ni se aplican parches de kernel.
- El APK de CI usa firma debug si no se configura un keystore de prueba.

## Validación pendiente en el dispositivo

1. Instalar el APK y activar el módulo en Vector.
2. Confirmar que Vector carga `HyperIslandModule` en `com.android.systemui` y `miui.systemui.plugin`.
3. Revisar Logcat por `HyperIsland[HyperOS2Compat]` y registrar si las clases nativas aparecen.
4. Conceder los dos permisos del fallback y probar notificaciones normales, agrupadas y eliminadas.
5. Confirmar que no hay reinicios de SystemUI, ventanas duplicadas ni consumo anómalo de batería.

Hasta completar esas pruebas, el proyecto debe considerarse un port experimental, no una versión estable certificada.
