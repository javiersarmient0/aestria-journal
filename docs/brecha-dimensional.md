# Brecha dimensional

Primera integración del cielo de Dedsafio 4 Remake en Aestria Journal para Fabric 1.21.1.

## Comandos de prueba

Los comandos requieren nivel de permiso 2 (OP):

- `/brecha iniciar`: inicia la secuencia de apertura.
- `/brecha reiniciar`: vuelve a iniciar la secuencia desde cero.
- `/brecha detener`: empieza a cerrar la brecha con un fundido.

El evento se sincroniza desde el servidor a los clientes que tengan Aestria Journal instalado. El estado del evento se mantiene en memoria y se reinicia cuando se detiene el servidor.

## Licencia y procedencia

El shader se adaptó desde Dedsafio 4 Remake, de Álvaro842DEV, rama `1.21.1-fabric`:
https://github.com/Alvaro842DEV/Dedsafio4-Remake/tree/1.21.1-fabric

El shader derivado está cubierto por LGPL-3.0. Consultar `LICENSES/Dedsafio4-Remake-LICENSE.txt` y `LICENSES/THIRD_PARTY_NOTICES.md`. Los componentes originales de Aestria Journal mantienen sus avisos/licencia propios.

## Estado

Este cambio es una primera adaptación. Debe compilarse y probarse en una instancia real de Minecraft 1.21.1 Fabric antes de considerarse listo para integrar en `main`. En particular, hay que validar el registro del shader, el punto de renderizado y la compatibilidad con Sodium/Iris y otros mods de renderizado.
