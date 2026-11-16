# Reglas de R8 para la versión release de Android (ver Calendario-arquitectura/03, «Ofuscación»).
#
# La mayoría de las librerías (Room, kotlinx.serialization, Ktor, Supabase, AndroidX Startup, coroutines)
# traen sus propias reglas ("consumer rules") y R8 las aplica solas. Aquí sólo va lo que la app necesita
# y no queda cubierto. Cada regla lleva su motivo; no añadir reglas "por si acaso".

# --- Trazas de error legibles ---------------------------------------------------------------------
# Conserva nombre de archivo y número de línea (el nombre de archivo se oculta). Para des-ofuscar una
# traza hace falta el `mapping.txt` de ESA compilación: app/build/outputs/mapping/release/mapping.txt
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room (Kotlin Multiplatform) ------------------------------------------------------------------
# Room localiza el objeto `CalendarDatabaseConstructor` (generado por KSP, `@ConstructedBy`) por
# reflexión. Nadie lo referencia en el código, así que R8 lo eliminaba por completo (aparecía en
# usage.txt). Se conservan todas las implementaciones de RoomDatabaseConstructor.
-keep class * implements androidx.room.RoomDatabaseConstructor { *; }
