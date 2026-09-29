# Pruebas del menú hamburguesa — V3.7.1

## Compilación e instalación

1. Conservar `app/google-services.json` y `remote-worker/wrangler.toml` de la instalación configurada.
2. En Android Studio ejecutar `Build > Clean Project` y `Build > Rebuild Project`.
3. Instalar sobre la aplicación existente sin desinstalarla, para conservar sus datos.

## Comprobación visual

1. Abrir la aplicación y verificar que el icono `☰` aparece arriba a la derecha.
2. Desplazarse hasta el final y confirmar que ya no existe el bloque `Centro de control`.
3. Confirmar que siguen visibles los controles contextuales de vigilancia, análisis manual, ordenación y oportunidades.

## Comprobación de accesos

Abrir el menú `☰` y probar, uno por uno: Inicio; Mis criptos y configuración individual; Oportunidades; Análisis por criptomoneda; Alarmas de precio; Posibles explosiones; Historial; Rendimiento/Estadísticas; Backtest; Exportación; Diagnóstico; Configuración global; Mapa de los 59 puntos; y Ayuda/Acerca de.

## Regresión mínima

Comprobar que se conservan el estado de vigilancia, umbral guardado, historial, alarmas de precio, detector de explosiones y datos acumulados. En Ajustes, el selector de posición del menú debe seguir permitiendo cambiar posteriormente entre izquierda y derecha.
