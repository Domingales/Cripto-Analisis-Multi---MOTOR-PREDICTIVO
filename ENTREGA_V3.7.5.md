# Entrega V3.7.5 vigilancia real en segundo plano

## Estado de la entrega

- Base utilizada: proyecto completo 3.7.4 `versionCode 18` del 27/09/2026 a las 12:00.
- Nueva versión: `versionCode 19`, `versionName 3.7.5-vigilancia-real-background`.
- Motor técnico, indicadores, señales, aprendizaje, SQLite, alarmas de precio y datos existentes: conservados.
- Comprobación estática de manifiesto, referencias, Worker y diferencias: superada.
- Compilación Android: pendiente en el PC porque el entorno de preparación no dispone de Gradle 8.9.
- Prueba física: pendiente en el DOOGEE S100.

## Archivos Android modificados

- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `service/BootReceiver.kt`
- `service/MarketScanJobService.kt`
- `service/MarketScanRunner.kt`
- `service/MarketWatchService.kt`
- `service/MonitoringScheduler.kt`
- `service/RemoteMessagingService.kt`
- `ui/MainActivity.kt`
- `ui/DiagnosticsActivity.kt`
- `ui/InfoActivity.kt`
- `ui/CoverageActivity.kt`
- `util/Prefs.kt`

## Worker y documentación

- `remote-worker/src/index.js`
- `README.md`
- `ARCHITECTURE.md`
- `CHANGELOG_V3.md`
- `COVERAGE_59_POINTS.md`
- `GUIA_HIBRIDA_GRATUITA.md`
- `PRUEBAS_VIGILANCIA_REAL_V3.7.5.md`
- `PROJECT_FILES.txt`

No se ha eliminado ningún archivo funcional.

## Instalación Android

1. Descomprimir el ZIP en una carpeta nueva.
2. Abrir la carpeta `PROYECTO CriptoAnalisisMulti` en Android Studio.
3. Esperar a que finalice la sincronización de Gradle.
4. Ejecutar `Build > Build APK(s)`.
5. Instalar la APK sobre la aplicación existente. No desinstalar antes.
6. Abrir la app y confirmar que aparece `CriptoAnálisis Multi 3.7.5`.
7. Pulsar `AJUSTES DE BATERÍA` y dejar la aplicación sin restricciones. En el DOOGEE, permitir también inicio automático si el fabricante muestra esa opción.
8. Activar la vigilancia y comprobar la notificación fija `CriptoAnálisis Multi activo`.

## Actualización del Worker

La app puede compilarse antes, pero para aplicar la mejora remota hay que desplegar el Worker incluido:

```powershell
cd "C:\Users\Usuario\PROYECTO CriptoAnalisisMulti\remote-worker"
npx.cmd wrangler deploy
npx.cmd wrangler tail
```

El despliegue conserva KV y secretos ya configurados. Deben aparecer envíos `FCM_ACCEPTED`.

## Prueba recomendada

Seguir `PRUEBAS_VIGILANCIA_REAL_V3.7.5.md`. La prueba decisiva consiste en bloquear el DOOGEE al menos 35 minutos sin abrir la app y comprobar después que el Diagnóstico contiene ciclos `REMOTO_FCM` o `LOCAL_FGS` durante ese periodo, sin ciclos duplicados separados por pocos segundos.

Silencio y No molestar deben dejar la notificación registrada y permitir que Android suprima sonido o vibración. Forzar detención desde Ajustes bloquea legalmente toda ejecución hasta volver a abrir la app.
