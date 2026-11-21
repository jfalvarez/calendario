# Deuda técnica

Lista de problemas conocidos que se han decidido aplazar. Cada entrada dice qué pasa, por qué no se arregla ahora y qué lo desbloquea.

## Avisos de `sun.misc.Unsafe` al ejecutar la app de escritorio

- **Qué pasa:** con JDK 25, al usar DataStore aparecen los avisos `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called` (`UnsafeUtil.arrayBaseOffset`). Vienen del protobuf embebido en `androidx.datastore:datastore-preferences-external-protobuf` (versión 1.3.0).
- **Por qué no se arregla ahora:** no hay una versión de DataStore que lo corrija. Es un aviso de la JVM, no un fallo de la app. Silenciarlo con `--sun-misc-unsafe-memory-access=allow` solo funciona en JDK 24 o posterior.
- **Riesgo:** el método se eliminará en una versión futura del JDK, y entonces DataStore dejaría de funcionar en escritorio.
- **Qué lo desbloquea:** una versión de DataStore sin `Unsafe`. Referencias: [protocolbuffers/protobuf #20760](https://github.com/protocolbuffers/protobuf/issues/20760), [grafana/pyroscope-java #361](https://github.com/grafana/pyroscope-java/issues/361).
- **Cuándo revisarlo:** al actualizar dependencias y, como mínimo, antes de cambiar el JDK de la distribución de escritorio.
