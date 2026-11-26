# Compilar HyperIsland HyperOS 2 con GitHub Actions

1. Crea un repositorio nuevo en GitHub (privado recomendado).
2. Sube **todo el contenido** de esta carpeta del proyecto a la raíz del repositorio. Deben quedar `android/`, `.github/`, `LICENSE` y los demás archivos en la raíz; no subas la carpeta contenedora `HyperIsland-main` como un nivel adicional.
3. En GitHub abre **Actions** y habilita los workflows si GitHub lo solicita.
4. Ejecuta **HyperIsland HyperOS 2 APK** → **Run workflow**.
5. Al terminar, abre la ejecución y descarga el artefacto `HyperIsland-HyperOS2-APK-N`.

El workflow compila `releaseFast` para poder probar sin una clave privada propia. El APK resultante se firma con la clave debug generada por Android Gradle Plugin. No compartas claves privadas ni contraseñas en el repositorio.

Si la compilación falla, descarga el artefacto de diagnósticos si existe y comparte aquí el texto del primer error real de Gradle (no solo el resumen `Process completed with exit code 1`).

La compilación no demuestra por sí sola que todas las funciones se ejecuten correctamente en el dispositivo. Después habrá que instalarlo, conceder los permisos necesarios y probarlo en el Redmi Note 13 4G con HyperOS 2 y Vector.
