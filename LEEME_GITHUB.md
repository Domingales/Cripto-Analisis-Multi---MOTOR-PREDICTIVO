# CriptoAnálisis Multi 3.8.0 — preparación para GitHub

Este repositorio contiene el proyecto **Android nativo en Kotlin**. GitHub almacena el código; la app no se abre en un navegador a partir del enlace del repositorio.

## Contenido

- `app/src/main/java/`: interfaz, motor predictivo, datos y vigilancia Android.
- `app/src/main/res/` y `app/src/main/AndroidManifest.xml`: recursos y permisos.
- `app/src/test/`: pruebas unitarias existentes.
- `gradle/`, `gradlew`, `gradlew.bat`, `build.gradle`, `settings.gradle`, `gradle.properties` y `app/build.gradle`: proyecto Android Studio.
- `remote-worker/`: código del apoyo remoto; su configuración de ejemplo se conserva.
- Los archivos `.md` originales: documentación y pruebas declaradas por el proyecto.

## Configuración que no se publica

Esta copia omite `local.properties`, `app/google-services.json`, `remote-worker/wrangler.toml` y `.idea/`. Conserva los correspondientes archivos `.example` donde existen. Los secretos de Cloudflare y la clave privada de Firebase deben mantenerse fuera del repositorio. El código incluido no se ha modificado.

Para compilar en tu ordenador, abre esta carpeta completa en Android Studio. Android Studio puede generar `local.properties` con la ruta de tu SDK. Si vas a usar Firebase en tu instalación local, pon **tu** `google-services.json` en `app/`; no lo subas a GitHub. Para configurar el Worker, crea localmente `remote-worker/wrangler.toml` a partir del ejemplo e introduce sus valores en tu equipo. La compilación de esta copia aún no se ha ejecutado aquí.

Antes de publicar, comprueba en GitHub que los archivos excluidos no figuren en el repositorio. Si otro repositorio ya contenía credenciales, eliminarlas en un nuevo commit no elimina las copias anteriores del historial.
