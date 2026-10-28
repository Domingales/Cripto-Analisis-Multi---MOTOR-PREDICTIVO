# Diagnóstico de calibración por dirección — 28/10/2026
Fuente inmutable:run40580602201/artefacto12768818184, SHA2564738051e6fd0538d1c45b3c0a409c55d03f9c62ff3ffd355a7540bfc303e0184. Modelo candidato magnitud/plazo SHA256781f093ff8b87cb733fab9dfe906b124772ccd7be2afcef5ba09b21782d2a10e.
Sólo diagnóstico:sin fit/recalibración/reentrenamiento, sin cambio de umbral0,90, sin modificar producción ni modelos prospectivos.

## Datos permitidos
Reproducir phase(t) y purga120h del experimento:
TRAIN etiquetas maduras antes de2023-04-01 (descriptivo dentro del entrenamiento).
CALIBRATION abril–junio2023 con maduración antes de2023-07-01 (calibración aparente; calibrador ya vio estos casos).
DEVELOPMENT_CHECK julio–septiembre2023 con maduración estrictamente antes de2023-10-02 (diagnóstico temporal posterior a calibración; ahora pasa a ser desarrollo y no se reutilizará como reserva independiente).
Excluir EXAMINATION desde2023-10-02 y todos los EMBARGO. No consultar sus etiquetas para diagnosticar ni elegir otro candidato.
Comparar toda puntuación BUY/SELL madura; PENDING separado, nunca negativo.

## Métricas fijadas
Probabilidad raw del GBC vs probabilidad calibrada logística congelada.
Por dirección/fase: prevalencia HIT exacto24h, media estimada, sesgo estimación−frecuencia, Brier, logloss, AUC(si dosclases), bins fijos0,1, ECE descriptivo y control constante con prevalencia TRAIN.
Umbrales0,50/0,60/0,70/0,80/0,90 sólo para diagnosticar cobertura y precisión, no optimizar ni activar un nuevo umbral.
Desgloses por régimen original conocido en cada instante y magnitud objetivo en bins fijos [0,0,5),[0,5,1),[1,2),[2,4),[4,infinito).
Perfiles mensuales para detectar deriva. Alertar subgrupos conmenos100casos o12bloques; tamaño bruto no equivale a independencia.
Bloques temporales14dUTC, bootstrap descriptivo1.000réplicas semilla28 sólo conalmenos12bloques. Si haymenos:no intervalo, explicar insuficiencia. Dependencia cruzada/no estacionariedad y ajustes sobre datos hacen que esto NO certifique90%.
BUY/SELL no son sucesos complementarios:ambos objetivos pueden alcanzarse en24h. Informar ambos/ninguno exacto y no forzar suma de probabilidades1.

## Integridad y entrega
Comprobar archive/model/code digests y fronteras. Guardar dataset diagnóstico sólo permitido; prueba de invariancia al alterar resultados/variables del examen. Tests Python/Android enActions. Conservar informe y código en research/expanded-replay-150/PR9.
No repetir el replay antiguo para generar los mismos labels:recuperar su artefacto. Si hay un error, distinguir fallo técnico de hallazgo de calibración. No convertir diagnóstico en mejora del predictor.
