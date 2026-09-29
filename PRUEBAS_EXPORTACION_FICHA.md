# Pruebas de la exportación de ficha completa

## Preparación en Windows 11

1. Descomprime el ZIP del proyecto en una carpeta nueva.
2. Abre Android Studio y selecciona `Open`.
3. Elige la carpeta descomprimida que contiene `settings.gradle`.
4. Espera a que finalice `Gradle Sync`.
5. Ejecuta `Build > Make Project`.
6. Instala la aplicación en el teléfono desde Android Studio.

La actualización usa el mismo `applicationId` de CriptoAnálisis Multi y no modifica la base de datos, por lo que debe instalarse como actualización conservando los datos existentes.

## Prueba funcional

1. Abre CriptoAnálisis Multi y entra en una criptomoneda que ya tenga análisis.
2. Comprueba que, debajo del título de la ficha, aparece `EXPORTAR FICHA COMPLETA`.
3. Pulsa el botón y verifica estas cuatro opciones:
   - `Copiar al portapapeles`: pega el contenido en una aplicación de notas y confirma que aparecen todos los apartados.
   - `Compartir informe`: confirma que Android muestra las aplicaciones disponibles para compartir texto.
   - `Guardar como TXT`: elige una carpeta, guarda el archivo y ábrelo.
   - `Guardar CSV para Excel`: guarda el archivo, ábrelo con Excel y comprueba las columnas `seccion`, `campo` y `valor`.
4. Repite la prueba con una criptomoneda distinta para confirmar que no mezcla datos entre activos.
5. Comprueba que siguen funcionando la vigilancia, el historial, la configuración y las notificaciones.

## Resultado esperado

- El nombre del archivo contiene criptomoneda, intervalo, fecha y hora.
- Las tildes y la letra ñ se muestran correctamente.
- El CSV se abre con tres columnas y no concentra toda la información en una sola.
- Se exportan la decisión, predicción, indicadores, MTF, Indicador Maestro, aprendizaje histórico, auditoría, evaluaciones recientes y seguimiento de señales.
- Cancelar el selector de archivos no crea ningún documento ni muestra un error.

## Si Excel coloca todo en una columna

La exportación utiliza punto y coma, que es el separador habitual de Excel en España. Si una configuración concreta de Excel no lo reconoce, utiliza `Datos > Desde texto/CSV` y selecciona `punto y coma` como delimitador.
