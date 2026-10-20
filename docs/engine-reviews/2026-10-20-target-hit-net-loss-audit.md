# Auditoría prospectiva — 20 de octubre de 2026

Consulta y publicación: 20/10/2026, aproximadamente 08:55 Europe/Madrid. Investigación solamente; sin modificaciones del motor Android, modelos, umbrales ni protocolos congelados.

## Estado recuperado de fuentes actuales
PR #9 abierta; head previo a este informe: 663e14150de731ffd241342741a31960b156dcf4. Las diez últimas ejecuciones consultadas estaban completadas con success; ninguna captura en curso. No se repitió la matriz150.

| Registro/motor | Decisiones | Señales | HIT | FAIL | Neutral | Pendiente |
|---|---:|---:|---:|---:|---:|---:|
| 15 combinaciones/original | 540 | 74 | 24 | 34 | 2 | 14 |
| 15 combinaciones/alternativo | 540 | 8 | 4 | 4 | 0 | 0 |
| XRP30m/original | 54 | 8 | 3 | 3 | 0 | 2 |
| XRP30m/alternativo | 54 | 0 | 0 | 0 | 0 | 0 |
| ADA main | 76 | 14 | 8 | 6 | 0 | 0 |

Cortes: selected 2026-10-20T06:36:52.262Z; XRP 2026-10-20T01:56:05.677Z. ADA recuperado de main después de run39366151115. Frente al informe del19: selected +51 decisiones,+6 señales originales,+2HIT; XRP +4 decisiones,+2 señales originales,+1FAIL; ADA +4 decisiones, sin señales nuevas, resueltos los tres pendientes en2HIT/1FAIL. Son diferencias entre cortes, no una estimación de ventaja.

Las ocho señales alternativas de las15 combinaciones permanecen concentradas en un grupo transitivo solapado de TRX; primera señal HIT. Cero señales XRP alternativas corresponde a modelo evaluable, no a falta de evaluación. El modelo prospectivo XRP no hereda el83,02% histórico.

## Hallazgo nuevo: alcanzar objetivo no garantiza retorno neto positivo

| Caso original | Salida | Entrada real simulada | Salida simulada | Neto principal |
|---|---|---:|---:|---:|
| RUNE-1h-1792349999999 SELL | TARGET / HIT direccional | 0,632 | 0,6305727771428572 | −0,1738956155% |
| XRP-30m-1792358999999 SELL | STOP / FAIL | 1,2706 | 1,27400735 | −0,6693745617% |

Costes congelados: comisión0,10% por lado y deslizamiento principal0,10% por lado; sensibilidades0%/0,20% de deslizamiento. RUNE devuelve +0,0260522% con comisión pero sin deslizamiento y −0,3742442% con0,20% por lado. No reajustar parámetros tras inspeccionar estos resultados.

Auditoría independiente local, sin importar el simulador productor: seis ventanas completas,1.728 velas5m y15 escenarios de costes reproducidos con Decimal, tolerancia absoluta1e-12 en retornos. Se verificaron hashes canónicos de ventanas y eventos presentes en las filas auditables, unicidad de decisiones, continuidad temporal y consistencia OHLC, entrada posterior al registro, límites originales no recentrados, primer toque, salida y fórmula de comisiones/deslizamiento. Cinco operaciones simuladas resueltas y un caso no ejecutable: RUNE15m abre fuera de niveles registrados y no se convierte artificialmente en beneficio. Esta reproducción prueba consistencia con evidencia persistida; no constituye una segunda fuente de mercado independiente ni una nueva auditoría completa de todos los recibos.

Selected: 264 decisiones elegibles desde inicio de costes2026-10-16T00Z;29 señales originales:3SIMULATED,1NOT_EXECUTABLE,22OMITTED_ACTIVE_WINDOW,3PENDING_DATA. XRP:24 decisiones elegibles;4 señales:2SIMULATED,1OMITTED_ACTIVE_WINDOW,1PENDING_DATA. Ningún par admitido estrictamente comparable original/alternativo. No sumar retornos como cartera. Muestra pequeña, dependiente y seleccionada; SELL es corto hipotético sin financiación/préstamo ni disponibilidad real verificada. No demuestra rentabilidad ejecutable.

## Continuidad y comprobaciones
- Selected run39380686979: recuperadas531 decisiones,+9=540;90tests Python y testDebugUnitTest/Kotlin BUILD SUCCESSFUL.
- XRP run39355948142: recuperadas53,+1=54;90tests y Kotlin BUILD SUCCESSFUL.
- ADA main run39366151115: success.
- Artefacto selected12245473252: sha2563159c3859ae6fdc9de44b23792fe67b7e74bd07359208c4163f3e04a19f51ffd.
- Artefacto XRP12235736148: sha256821833be3573ab375d59cc53a48012ee6c57128098604b5710506b6fa8052a20.
Metadatos de artefactos consultados; ZIP no descargados en esta auditoría. Journals y evidencia recuperados directamente de los ficheros duraderos de la rama:
selected blob a4892bf1246de85cf436a254415434b7fb11ee68; XRP blob7d8f4971b8d2124e2223c697a297ce3389f0bb9f.

## Próximos vencimientos de simulación
ADA4h20/10 19:40Madrid; RUNE1h21/10 03:25; XRP30m21/10 04:00; TRX4h22/10 02:35. Son finales de ventana, no promesa de captura exacta: resolver sólo después de disponer de ventana íntegra y datos cerrados, sin rellenar huecos.

Sin mejora predictiva independiente demostrada; producción intacta. Continuar recogida congelada, revisar integridad/deduplicación y resultados futuros, antes de explorar hipótesis limitadas con reservas cronológicas y embargo. No reentrenar por captura. La matriz150 final sigue archivada en37623666317 (50,49% original frente41,50% alternativo;134 evaluables/16 no preparados); XRP histórico procede de37607394740 y sus53ventas/4episodios no validan el modelo nuevo. No repetir filtros rechazados ni fusionar indiscriminadamente PR#9.

Fuentes: [PR9](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/pull/9), [selected](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39380686979), [XRP](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39355948142), [ADA](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39366151115). Ficheros de estado: evaluation/selected-prospective y evaluation/prospective-xrp en research/expanded-replay-150; ADA evaluation/prospective/events.jsonl en main.
