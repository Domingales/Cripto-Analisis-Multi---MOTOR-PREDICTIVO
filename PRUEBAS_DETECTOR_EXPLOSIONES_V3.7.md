# Pruebas del detector independiente de posibles explosiones — V3.7.0

## Estado de verificación

- Implementación y conexiones revisadas estáticamente.
- Se añadieron pruebas unitarias de 5/5, bloqueo intravela, volumen insuficiente y resolución por primer toque.
- En el entorno de generación no pudo ejecutarse Gradle porque la distribución 8.9 no estaba instalada y el acceso a `services.gradle.org` estaba bloqueado. La compilación real queda pendiente de Android Studio.

## Compilación en Windows 11

1. Sustituir la carpeta del proyecto por esta V3.7.0 conservando fuera del ZIP las credenciales privadas.
2. Confirmar que `app/google-services.json` continúa en su sitio si ya estaba configurado.
3. Abrir Android Studio y esperar a que finalice Gradle Sync.
4. Ejecutar `Build > Clean Project`.
5. Ejecutar `Build > Rebuild Project`.
6. Instalar con `Mayús + F10` sobre la aplicación existente. La migración V8 → V9 no debe borrar datos.

## Regresión obligatoria

1. Verificar que siguen visibles los contadores anteriores de evaluaciones, preseñales, señales, alertas, alarmas y seguimientos.
2. Abrir Historial, Rendimiento, Alarmas de precio y Ficha completa; comprobar que sus registros anteriores siguen presentes.
3. Ejecutar `ANALIZAR AHORA` y confirmar un nuevo ciclo completo sin errores.
4. Confirmar que el Worker/Firebase continúa mostrando la conexión remota configurada.
5. Confirmar que el umbral conserva el valor guardado por el usuario; el detector no lo cambia.

## Detector independiente

1. En `Configuración global`, comprobar los tres controles: activar detector, sonido y vibración.
2. Abrir `POSIBLES EXPLOSIONES`. Inicialmente puede mostrar cero: es normal porque exige 5/5.
3. Cuando aparezca una detección, comprobar que indica dirección, volumen, ATR, ADX, MTF y `sin calibrar`.
4. Verificar que la misma vela no genera una segunda detección ni una segunda notificación.
5. Confirmar que la recomendación normal y su porcentaje permanecen sin cambios en la misma tarjeta.
6. Probar `COPIAR / PEGAR JSON`, TXT y CSV.
7. Tras alcanzar +1 ATR, −1 ATR o finalizar el horizonte, comprobar ACIERTO, FALLO o NEUTRA y sus MFE/MAE.

## Criterio exacto inicial

- Volumen actual ≥2,00 veces la media.
- Cierre confirmado fuera del soporte o resistencia calculado, sin falso breakout.
- ATR14 ≥1,10 veces su media reciente y superior al ATR anterior.
- ADX14 ≥25, superior al anterior y DMI coherente con la dirección.
- Tres o más intervalos alineados, incluidos el principal y al menos 4h o 1d, sin un intervalo contrario.

No debe interpretarse como una probabilidad del 80–90 %. Es una configuración técnica extrema pendiente de calibración histórica propia.
