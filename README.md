# EstructurasAleatorias

Plugin para Paper 1.21.11 que genera estructuras a partir de schematics (`.schem`) por el mundo,
con botín dinámico en los cofres y mobs/spawners de MythicMobs.

## Requisitos

- Paper 1.21.11 y Java 21
- WorldEdit 7.3.19 (o FastAsyncWorldEdit equivalente)
- MythicMobs (opcional: sin él solo se ignoran los ítems y mobs mythic)

## Compilar

```bash
mvn package
```

El jar queda en `target/EstructurasAleatorias-1.0.0.jar`.

## Primeros pasos

1. Copia el jar a `plugins/` y arranca el servidor una vez.
2. Mete tus `.schem` en `plugins/EstructurasAleatorias/schematics/`.
3. Edita `estructuras.yml` (una entrada por estructura) y `botin.yml` (tablas de loot).
4. `/estructuras recargar`
5. Pruébalo sin esperar a que se genere mundo nuevo: `/estructuras aqui <id>`.

## Cómo preparar un schematic

- Copia la estructura con WorldEdit (`//copy`) estando **de pie en el centro de la base**;
  así el ancla `CENTRO` y el `offset-y` funcionan de forma predecible. Guarda con `//schem save nombre`.
- **Cofres**: déjalos vacíos. El plugin los rellena al generarse con la tabla indicada en
  `cofres.tabla`. Si pones un bloque especial justo debajo (por ejemplo `GOLD_BLOCK`), ese
  cofre usa otra tabla y el bloque se sustituye automáticamente.
- **Mobs**: pon un bloque marcador (por ejemplo lana roja) donde quieras el spawn.
  Se borra al generar y aparece el MythicMob.
- **Spawners**: igual, con otro bloque marcador. Se convierte en un spawner vanilla "marcado";
  cuando intenta generar, el plugin cancela el spawn vanilla y pone el MythicMob.

## Comandos

| Comando | Qué hace |
|---|---|
| `/estructuras recargar` | Recarga los tres archivos de configuración |
| `/estructuras lista` | Estructuras cargadas y cuántas veces se han colocado |
| `/estructuras info <id>` | Detalle de una estructura |
| `/estructuras aqui <id>` | La pega en tu posición saltándose los filtros |
| `/estructuras pegar <id> <x> <z>` | La pega en unas coordenadas |
| `/estructuras limpiar` | Olvida las posiciones registradas (no borra lo construido) |

Permiso: `estructuras.admin` (op por defecto).

## Notas

- La probabilidad es **por chunk nuevo generado**. `0.004` ya da bastantes estructuras;
  empieza bajo y sube mirando el resultado.
- `colocadas.yml` guarda dónde se colocó cada estructura para respetar las distancias mínimas.
- Si notas tirones al generar estructuras grandes, baja `general.bloques-por-tick` en `config.yml`.
