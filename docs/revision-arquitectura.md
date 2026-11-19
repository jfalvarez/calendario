# Revisión de arquitectura: Clean, MVVM y SOLID

Fecha: 2026-11-19 · Rama revisada: `develop` · Alcance: módulos `shared`, `app` (Android) y `desktopApp`.

Es una revisión estática de lectura de código (no se ha compilado ni ejecutado nada). Los archivos y líneas citados son de este momento y pueden moverse.

---

## 1. Resumen

El proyecto está **bastante bien encaminado**. La estructura Clean en tres capas existe de verdad y se respeta casi siempre. Hay margen de mejora en cinco sitios concretos, ninguno urgente ni peligroso.

| Área | Nota | Comentario |
|---|---|---|
| Regla de dependencias (Clean) | 8/10 | `domain` no importa nada de `data`, Android, Room ni Supabase. `presentation` no importa `data`. |
| MVVM | 7/10 | Un `StateFlow<UiState>` por VM y pantallas "tontas". Dos VMs se saltan los casos de uso. |
| SOLID | 7/10 | S, L y D bien. Hay pequeñas faltas de I y O, y una de S en `SyncEngine` y `CalendarScreen`. |
| Testabilidad | 6/10 | Buenos tests de dominio y sync. Faltan tests de la mayoría de ViewModels. |
| Limpieza | 7/10 | Imports con nombre completo en línea, `AppModule.kt` con muchos imports, KDoc desigual. |

### Lo que ya está bien (conviene no tocarlo)

- **Dominio puro**: `domain/` solo importa `kotlin`, `kotlinx.*` y su propio paquete. Esto es lo más valioso del proyecto.
- **Inversión de dependencias**: cada repositorio es una interfaz en `domain/repository` y la implementación (`Room*`, `Supabase*`, `DataStore*`) vive en `data`. Koin enlaza `single<Interfaz> { Implementación }`.
- **Puertos de plataforma** bien pensados: `TodayProvider`, `IdGenerator`, `ErrorReporter`, `PushTokenProvider`, `AppVersionProvider`, `ReminderSystem`, `UpdateLauncher`, `BackupFileIo`. Android y escritorio los implementan por separado.
- **Casos de uso pequeños** (un `invoke` cada uno), con validación en el dominio (`AccountUseCases`, `SaveShiftUseCase`…).
- **Offline-first** con `SyncEngine` + `Merge.kt` (función pura y con test).
- **Multiplataforma real**: UI Compose y VMs en `commonMain`; las plataformas solo aportan Koin, permisos y servicios.
- **Cadenas en recursos**: 198 usos de `stringResource` y ningún texto literal en `Text("…")` de `presentation`.
- Tests de dominio, `Merge`, `SyncEngine` y repositorios Room, con fakes en memoria.

---

## 2. Hallazgos

Ordenados por impacto. Cada uno lleva su ID para referenciarlo en el plan de acción.

### H1 · Los VMs sociales saltan la capa de casos de uso (MVVM/Clean, alto)

`SocialViewModel` y `SharedScheduleViewModel` reciben `SocialRepository` (y `SharedScheduleViewModel` además `ShiftRepository` y `ShiftAssignmentRepository`) directamente. El resto de VMs solo dependen de casos de uso, y el comentario de `CalendarViewModel` lo declara como regla ("Sólo depende de casos de uso").

Consecuencias:
- **Lógica de negocio dentro del VM**: `SharedScheduleViewModel.openProposal` calcula la ventana de 60 días, cruza asignaciones con nombres de turno, ordena y construye las opciones. Es lógica de dominio y hoy solo se podría probar con un VM.
- `SocialViewModel.refresh` encadena cinco llamadas al repositorio.
- La validación de alias (`isValidAlias`) se hace en el VM, no en un caso de uso (en el resto de funciones validan los casos de uso).
- Hay dos sitios con el mismo mapeo `(e as? SocialException)?.error ?: SocialError.Unknown`.

Archivos: `presentation/social/SocialViewModel.kt`, `presentation/social/SharedScheduleViewModel.kt`.

### H2 · `SocialRepository` es una interfaz demasiado ancha (ISP, medio)

Tiene 17 métodos de tres temas distintos: **vínculos** (alias, solicitudes, bloqueos), **calendario compartido y cambios** (propuestas) y **avisos push** (`registerPushToken`/`unregisterPushToken`). `PushRegistration` depende de toda la interfaz solo para usar dos métodos. Cualquier fake de test tiene que implementar los 17.

Archivos: `domain/repository/SocialRepository.kt`, `data/remote/SupabaseSocialRepository.kt` (265 líneas, también mezcla DTOs, mapeo y traducción de errores).

### H3 · `SyncEngine` hace demasiado y conoce Room (SRP/DIP, medio-alto)

`SyncEngine` (214 líneas) a la vez:
1. orquesta el ciclo de vida (`start`, observar sesión, temporizador, debounce),
2. hace el push de cuatro entidades,
3. hace el pull de cuatro entidades con el mismo patrón copiado cuatro veces,
4. gestiona el cursor en DataStore,
5. decide qué hacer al cambiar de cuenta (`adoptAccount`, que borra tablas y reinserta turnos por defecto).

Además recibe `CalendarDatabase` (clase concreta de Room) y `DataStore<Preferences>` (concreta). Para añadir una quinta entidad hay que tocar `push`, `pull`, el `combine` de `observeDirtyCount` y `adoptAccount`: viola abierto/cerrado.

Archivos: `data/sync/SyncEngine.kt`, `di/AppModule.kt:135`.

### H4 · `CalendarScreen.kt` y `CalendarViewModel` concentran demasiado (SRP, medio)

- `CalendarScreen.kt` tiene **693 líneas** con pantalla, barras, diálogos, celdas, menú y estado de sincronización. Es el archivo más grande del proyecto, y casi tres veces el segundo del módulo.
- `CalendarViewModel` recibe **18 dependencias** en el constructor: pintar, copiar/pegar, notas, deudas, sincronización, festivos, primer plano. Es señal de que mezcla tres responsabilidades: *edición de días*, *notas* y *sincronización*.
- `CalendarScreen` instancia además `SocialViewModel` (`koinViewModel()`) solo para el chip de solicitudes: una pantalla que depende de dos VMs de dominios distintos.
- `CalendarViewModel` contiene lógica de selección de rango (`onDayLongPress`, `plusOneDay`) que es regla pura y encajaría en una función de dominio testeable.

### H5 · Los VMs no tienen casi tests (calidad, medio)

Solo existen `ShiftsViewModelTest`, `UpdateViewModelTest` y `SyncIndicatorTest`. Sin test: `CalendarViewModel` (el más complejo), `SocialViewModel`, `SharedScheduleViewModel`, `AccountViewModel` (máquina de estados de formulario con 5 modos), `SettingsViewModel`, `StatsViewModel`, `DebtsViewModel`, `BackupViewModel`. `AccountUseCases` sí está probado, pero no la transición de estados del VM.

### H6 · Reloj y "hoy" repartidos (DIP/testabilidad, medio-bajo)

- `nowMillis()` en `data/repository/Ids.kt` es una función libre con `Clock.System`, llamada desde 5 repositorios: no se puede fijar la hora en un test de repositorio.
- `BackupViewModel` llama a `Clock.System.now()` directamente, aunque ya recibe `TodayProvider`.
- `SyncEngine` sí recibe `clock` por constructor (bien), pero es una lambda propia en vez de un puerto común.
- `TodayProvider` y el reloj son dos conceptos casi iguales.

### H7 · Android: acceso a Koin con `GlobalContext` (DIP, bajo-medio)

`ReminderReceiver`, `ReminderRescheduleReceiver`, `ShiftWidget` y `ShiftWidgetReceiver` hacen `GlobalContext.get().get<…>()` (service locator). `ReminderReceiver` además crea su propio `CoroutineScope(Dispatchers.Default)`. Es admisible porque Android crea esas clases y no admiten constructor con parámetros, pero conviene concentrarlo y que no pida repositorios sueltos (ahora `ReminderReceiver` pide `ShiftAssignmentRepository` y `ShiftRepository` y filtra turnos con horario: lógica de negocio dentro del receiver).

Relacionado: `MainActivity` inyecta `AndroidReminderSystem` y `PlayUpdateLauncher` por su tipo concreto, y mezcla en `onCreate` el permiso de notificaciones, el registro push y la configuración de barras del sistema.

`ReminderUpdater` y `ShiftWidgetUpdater` crean cada uno su `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, igual que `SyncEngine` en `AppModule`. Son tres ámbitos sin dueño ni cancelación.

### H8 · Dispatchers fijos en el código (testabilidad, bajo)

`Dispatchers.IO`/`Default` aparecen escritos en `CalendarDatabase.kt:55`, `PreferencesDataStore.kt:20`, `AppModule.kt:135`, `ReminderUpdater`, `ShiftWidgetUpdater` y `ReminderReceiver`. No hay un proveedor de dispatchers inyectable. Los VMs no lo necesitan (usan `viewModelScope`), pero los componentes de fondo sí.

### H9 · Puertos de plataforma ubicados en `presentation` (Clean, bajo)

`ReminderSystem` (`presentation/settings`), `UpdateLauncher` (`presentation/update`), `BackupFileIo` (`presentation/backup`) y `AppForegroundEvents` (`presentation/update`) son abstracciones de plataforma, no de UI. Funciona, pero `AppForegroundEvents` lo usa también `SyncEngine`/Application, y sería más coherente tenerlos en `domain` (puertos) o en un paquete `platform`. Además `PushRegistration` es un caso de uso con nombre distinto al resto (sin sufijo `UseCase`), y `ErrorReporter` mezcla `report` con `sendTestReport`/`canSendTest` (ajuste de diagnóstico).

### H10 · Interfaces anchas y casos de uso "tubería" (ISP/YAGNI, bajo)

- `DataStoreSettingsRepository` implementa `SettingsRepository` y `ReminderSettingsRepository` (bien, vía `binds`), pero `SettingsRepository` mezcla tema, estilo y "versión de aviso descartada" (esta última es política de actualización, no ajuste visual).
- Varios casos de uso solo delegan una llamada: `SignOutUseCase`, `ObserveAuthStateUseCase`, `ObserveSyncStateUseCase`, `RequestSyncUseCase`, `ObserveThemeModeUseCase`, `SetThemeModeUseCase`, `ObserveAppStyleUseCase`, `SetAppStyleUseCase`, `ObserveShiftsUseCase`, `PaintDayUseCase`… Es coherente y aceptable por uniformidad, pero añade 40 registros `factory` y un VM con 15 parámetros. Mantenerlos es una decisión, no un error; ver Fase 6.

### H11 · Higiene de código (limpieza, bajo)

- `AppModule.kt`: ~60 imports, algunos desordenados y varios con nombre totalmente cualificado en línea (`com.jfaf.calendario.domain.repository.BackupStore`, `com.jfaf.calendario.domain.usecase.ExportBackupUseCase`…). Los tres módulos (`dataModule`, `domainModule`, `presentationModule`) están en un solo archivo.
- Nombres completos en línea en `CalendarViewModel` (`kotlinx.coroutines.flow.flowOf`, `com.jfaf.calendario.domain.model.DayNoteMarkers`, `kotlinx.datetime.LocalDate.fromEpochDays`) y `MainActivity` (`com.jfaf.calendario.backup.AndroidBackupFileIo`). `LocalDate.plusOneDay()` reinventa `plus(DatePeriod(days = 1))`.
- Los dos `CrashReporting.kt` (Android y escritorio) repiten estructura; `catch (e: Exception)` en `SupabaseAuthRepository`, `SupabaseSocialRepository`, `BackupCodec` con distintos criterios.
- `SupabaseSocialRepository`: los `when` de cadenas a enums (`"pending" -> …`) están repartidos y con un `else` que oculta valores nuevos del servidor.
- `SupabaseConfig` aplica un destino inválido `https://not-configured.invalid` con comentario "falla sin avisar" dentro de `AppModule`: es política de configuración que debería estar en un único sitio.
- Estado de UI: `CalendarUiState` y `AccountUiState` tienen 11 y 12 campos; vale, pero `AccountViewModel.Form` y `AccountUiState` duplican los mismos campos.

---

## 3. Plan de acción

Cada fase es independiente, se puede hacer en una rama y entra en `develop` con los tests en verde. Orden pensado para **menor riesgo primero** y para que lo que se prueba antes (H5) proteja lo que se refactoriza después (H1, H3, H4).

| Fase | Contenido | Hallazgos | Riesgo | Esfuerzo |
|---|---|---|---|---|
| 0 | Red de seguridad: tests de VMs | H5 | Nulo | M |
| 1 | Higiene y orden (sin cambiar comportamiento) | H11 | Muy bajo | S |
| 2 | Reloj y dispatchers inyectables | H6, H8 | Bajo | S |
| 3 | Capa social: casos de uso y partir la interfaz | H1, H2 | Medio | M |
| 4 | Partir `CalendarViewModel` y `CalendarScreen` | H4 | Medio | M-L |
| 5 | `SyncEngine` extensible | H3 | Medio-alto | L |
| 6 | Android y puertos de plataforma | H7, H9, H10 | Bajo-medio | M |

(S = medio día, M = 1-2 días, L = 3 días o más.)

### Fase 0 · Red de seguridad: tests de ViewModels

Objetivo: poder refactorizar las fases 3 y 4 sin miedo.

1. Añadir dependencia de test `kotlinx-coroutines-test` y `turbine` si no están en `gradle/libs.versions.toml` (ya hay `ShiftsViewModelTest` como patrón).
2. Crear fakes que falten en `commonTest/fakes`: `FakeSocialRepository`, `FakeAuthRepository`, `FakeSyncRepository`, `FakeSettingsRepository`, `FakeDayNoteRepository`, `FakeTodayProvider`.
3. Tests mínimos por VM:
   - `CalendarViewModelTest`: pintar un día, selección por toque y por rango (pulsación larga hacia atrás y hacia delante), copiar → pegar, cortar, asignar con segundo turno, cambio de modo limpia selección, `onToday`, recálculo de "hoy" al volver a primer plano.
   - `AccountViewModelTest`: transiciones `SignIn → ForgotEmail → ForgotCode → SignIn`, que la contraseña no queda en el estado tras éxito, que `busy` evita doble envío.
   - `SocialViewModelTest`: alias inválido no llama al repositorio, `perform` recarga solo si hay éxito, `busy` bloquea acciones concurrentes.
   - `SharedScheduleViewModelTest`: descarta respuesta si cambió el mes, construye opciones de propuesta, error al enviar.
   - `StatsViewModelTest`, `DebtsViewModelTest`, `SettingsViewModelTest`, `BackupViewModelTest`: un caso feliz y uno de error cada uno.
4. Ejecutar `./gradlew :shared:desktopTest` y dejar la base en verde.

Criterio de hecho: cada VM con al menos un test; suite en verde.

### Fase 1 · Higiene y orden (sin cambios de comportamiento)

1. `AppModule.kt`: ordenar imports, quitar nombres totalmente cualificados en línea (`BackupStore`, `Export/ImportBackupUseCase`), y partir en `DataModule.kt`, `DomainModule.kt`, `PresentationModule.kt` (mismos nombres públicos para no tocar a Android/escritorio; `appModules` se queda en `AppModule.kt`).
2. `CalendarViewModel`: sustituir `kotlinx.coroutines.flow.flowOf`, `DayNoteMarkers` y `LocalDate.fromEpochDays(…)` por imports normales; `plusOneDay()` → `date.plus(1, DateTimeUnit.DAY)`.
3. `MainActivity`: importar `AndroidBackupFileIo`; sacar la configuración de barras del sistema a una función privada `applySystemBars(dark)`.
4. Unificar en un helper (`Result<T>.toSocialError()`, en `domain/repository/SocialRepository.kt`) el mapeo `(e as? SocialException)?.error ?: SocialError.Unknown` repetido en los VMs sociales.
5. `SupabaseSocialRepository`: mover los DTOs (`RemoteProfile`, `RemoteLink`, `RemoteSwap`…) y sus `toDomain()` a `data/remote/SocialDtos.kt`, y `socialErrorOf` a `SocialErrors.kt`. El repositorio queda con solo las llamadas. En los `when` de cadenas a enum, registrar un aviso (`Logger.w`) en el `else` en lugar de asumir un valor.
6. Ejecutar `./gradlew :shared:desktopTest :app:assembleDebug :desktopApp:compileKotlinDesktop` (o los equivalentes que use el proyecto).

Criterio de hecho: ninguna línea de lógica cambiada; mismos tests en verde; diff solo de movimiento/imports.

### Fase 2 · Reloj y dispatchers inyectables

1. Crear en `domain/repository` el puerto `Clock`-like. Opción simple y recomendada: reutilizar `kotlin.time.Clock` (ya en la librería estándar) y registrar `single<Clock> { Clock.System }` en `dataModule`.
2. Sustituir `nowMillis()` por `clock.now().toEpochMilliseconds()` inyectado en los cinco repositorios Room (`RoomShiftRepository`, `RoomShiftAssignmentRepository`, `RoomDayNoteRepository`, `RoomLocalHolidayRepository`, `RoomBackupStore`). Eliminar `nowMillis` de `Ids.kt`.
3. `SyncEngine`: cambiar la lambda `clock: () -> Long` por el mismo `Clock`.
4. `SystemTodayProvider` pasa a derivarse de `Clock` + `TimeZone` (`TodayProvider` se mantiene como puerto).
5. `BackupViewModel`: usar el `Clock` inyectado en vez de `Clock.System.now()`.
6. Crear `interface AppDispatchers { val io; val default; val main }` con implementación por defecto; inyectarlo en `ReminderUpdater`, `ShiftWidgetUpdater`, `SyncEngine` y las funciones de construcción de base de datos/DataStore.
7. Tests: añadir un `FakeClock` en `commonTest/fakes` y un test que compruebe que `updatedAt` usa la hora inyectada.

Criterio de hecho: `grep -rn "Clock.System\|System.currentTimeMillis\|Dispatchers\." shared/src/commonMain` solo devuelve la implementación por defecto y el módulo Koin.

### Fase 3 · Capa social: casos de uso y partir la interfaz

1. **Partir `SocialRepository`** (H2), manteniendo una sola clase de implementación que implemente las tres:
   - `LinkRepository`: `setAlias`, `myAlias`, `requestLink`, `pendingRequests`, `myLinks`, `acceptRequest`, `rejectRequest`, `revokeLink`, `blockRequester`, `blockedUsers`, `unblock`.
   - `SharedScheduleRepository`: `sharedSchedule`, `proposeSwap`, `respondSwap`, `cancelSwap`, `mySwaps`.
   - `PushTokenRepository`: `registerPushToken`, `unregisterPushToken`.
   - `SupabaseSocialRepository` implementa las tres y Koin las enlaza con `binds`, igual que `DataStoreSettingsRepository`. `PushRegistration` pasa a depender solo de `PushTokenRepository`.
2. **Casos de uso nuevos** en `domain/usecase/SocialUseCases.kt` (uno por intención, mismo estilo que `AccountUseCases.kt`):
   - `SaveAliasUseCase` y `RequestLinkUseCase` (validan con `isValidAlias` y devuelven un resultado de dominio, no un booleano en el VM).
   - `LoadSocialOverviewUseCase`: devuelve `SocialOverview(alias, pending, links, swaps, blocked)` (sustituye las cinco llamadas de `refresh`).
   - `AcceptRequestUseCase`, `RejectRequestUseCase`, `BlockRequesterUseCase`, `UnblockUseCase`, `RevokeLinkUseCase`, `RespondSwapUseCase`, `CancelSwapUseCase`. Si se prefiere menos ficheros, agruparlos en una sola clase `ManageLinksUseCases` con varias funciones.
   - `ObserveSharedScheduleUseCase` (o `LoadSharedScheduleUseCase`) para `sharedSchedule`.
   - `BuildSwapOptionsUseCase(from, days = 60)`: mueve aquí la lógica de `openProposal` (cruce asignaciones/nombres, ordenación). Se testea sin VM.
   - `ProposeSwapUseCase`.
3. Los VMs quedan solo con `StateFlow`, llamadas a casos de uso y mapeo de errores. `SharedScheduleViewModel` deja de conocer `ShiftRepository` y `ShiftAssignmentRepository`.
4. Mover `MyShiftOption` y `ProposalDraft` fuera del archivo del VM a `presentation/social/SharedScheduleUiState.kt` (el estado de UI no debe vivir junto a la lógica).
5. Actualizar `AppModule` (factory de los casos de uso) y `SharedScheduleViewModel` en Koin.
6. Tests: casos de uso nuevos con `FakeSocialRepository` (hay ya `SocialValidationTest` y `SwapPreviewTest` como base).

Criterio de hecho: ningún VM de `presentation` importa `domain.repository.*` salvo `TodayProvider` y los errores de dominio.

### Fase 4 · Partir `CalendarViewModel` y `CalendarScreen`

1. **Extraer lógica pura** a dominio, con test:
   - `SelectDaysUseCase`/función `dateRange(a, b)` para la selección por rango (hoy `onDayLongPress` + `plusOneDay`).
   - El cálculo del rango de tres meses para observar notas (`buildMonthGrid(m.previous…)`) pasa a un caso de uso `ObserveNoteMarkersUseCase(month)` que devuelve ya los `markers`.
2. **Dividir el VM por responsabilidad**, manteniendo la pantalla única:
   - `CalendarViewModel`: mes mostrado, pintar y modo.
   - `DayEditingViewModel` (o un *state holder* `DayEditor`): selección, portapapeles, copiar/cortar/pegar/borrar, asignar, repetir, festivos.
   - `DayNotesViewModel`: notas del día, marcadores, liquidar, borrar, personas con deudas.
   - La sincronización (`syncIndicator`, `onSyncClick`) pasa al VM propio que ya usan `AccountViewModel` y `SocialViewModel`, o a un `SyncStatusViewModel` pequeño.
   - La pantalla combina los estados con `koinViewModel()` por cada uno. Cada VM queda por debajo de ~8 dependencias.
3. **Dividir `CalendarScreen.kt` (693 líneas)** por carpetas de componentes, sin cambiar la UI:
   - `calendar/components/DayCell.kt`, `MonthGrid.kt`
   - `calendar/components/PaintBar.kt`, `EditBar.kt`
   - `calendar/components/AssignDialog.kt`, `RepeatDialog.kt`
   - `calendar/components/OverflowMenu.kt`, `SyncStatus.kt`, `NoteMarker.kt`
   - `CalendarScreen.kt` solo conecta VMs y `CalendarContent`.
   - Mantener `CalendarContent(state, actions)` stateless con `@Preview`/pruebas de captura si el proyecto los usa.
4. El chip de solicitudes (`RequestsChip`) recibe el estado por parámetros desde `CalendarScreen` en vez de que la pantalla de calendario instancie `SocialViewModel`.
5. Comparar visualmente antes y después en Android y escritorio (mismo mes, mismo modo).

Criterio de hecho: ningún archivo de `presentation/calendar` supera ~250 líneas; ningún VM del calendario supera ~8 dependencias; los tests de Fase 0 siguen en verde.

### Fase 5 · `SyncEngine` extensible

1. **Una estrategia por entidad**: definir

   ```kotlin
   internal interface SyncTable {
       val name: String                       // "shifts", "assignments", "holidays", "day_notes"
       fun observeDirtyCount(): Flow<Int>
       suspend fun push(userId: String)
       suspend fun pull(userId: String, cursor: String?): String?   // devuelve el cursor nuevo
       suspend fun clearLocal()               // para el cambio de cuenta
   }
   ```

   con cuatro implementaciones (`ShiftSyncTable`, `AssignmentSyncTable`, `HolidaySyncTable`, `DayNoteSyncTable`) que encapsulan DAO + conversión + `merge*`. Las cuatro copias del bloque de `pull` desaparecen.
2. **Cursores**: extraer `SyncCursorStore` (interfaz) con una implementación sobre `DataStore<Preferences>`. `SyncEngine` deja de importar `androidx.datastore`.
3. **Cambio de cuenta**: extraer `AccountAdoption` (`adoptAccount`) con su propia interfaz y test; que reciba la lista de `SyncTable` para llamar a `clearLocal()` y un `DefaultDataSeeder` para reinsertar turnos por defecto.
4. `SyncEngine` queda como orquestador: sesión, `debounce`, temporizador, `mutex`, estado. Recibe `List<SyncTable>`, `SyncCursorStore`, `AccountAdoption`, `AuthRepository`, `Clock`, `AppDispatchers`/`CoroutineScope`, `ErrorReporter`. Ya no recibe `CalendarDatabase`.
5. Dar un dueño al `CoroutineScope` del motor: crearlo en Koin como `single(named("appScope"))` y compartirlo con `ReminderUpdater` y `ShiftWidgetUpdater` (Fase 6).
6. Tests: ya existe `SyncEngineTest` (desktop); mantenerlo verde y añadir un test por `SyncTable` y otro de "añadir una tabla nueva no toca el motor".

Criterio de hecho: añadir una quinta entidad sincronizable exige solo una clase nueva y una línea en Koin.

### Fase 6 · Android y puertos de plataforma

1. **Puertos a su sitio** (H9): mover `ReminderSystem`, `UpdateLauncher`, `BackupFileIo` a `domain/platform/` (o `presentation/platform/` si se decide que son de UI) y `AppForegroundEvents` a `domain/platform/` con interfaz + implementación. Es un cambio de paquete; Android y escritorio solo ajustan imports.
2. **Receivers y widget sin lógica de negocio** (H7): crear casos de uso `GetShiftsForDayUseCase(date)` (que devuelve los turnos con horario de ese día) y úsalo desde `ReminderReceiver`; los receivers y `ShiftWidget` piden *un* servicio (`koin.get<ReminderFacade>()`/el caso de uso) y no repositorios sueltos. Concentrar el acceso a `GlobalContext` en un único objeto `AndroidServiceLocator`, con comentario de por qué existe.
3. `MainActivity`: inyectar `ReminderSystem` y `UpdateLauncher` por su interfaz cuando sea posible; el `attach(activity)` queda en una interfaz pequeña `ActivityBound` o se mantiene con el tipo concreto (decisión consciente).
4. `ReminderUpdater` y `ShiftWidgetUpdater` reciben el `CoroutineScope` de aplicación inyectado (Fase 5, punto 5) en lugar de crear el suyo.
5. `ErrorReporter`: separar el "informe de prueba" (`canSendTest`, `sendTestReport`) en un `DiagnosticsTools` opcional para que el puerto principal tenga solo `report`.
6. Revisar la regla de casos de uso triviales (H10): decidir y dejar escrito en `docs/` si se mantienen (uniformidad) o si los VMs pueden consumir directamente repositorios de solo lectura. **Recomendación: mantenerlos**, ya que dan un único punto de entrada por intención y el coste es solo declarativo. Si se mantiene, agruparlos por archivo de tema (ya se hace) y usar `factoryOf(::X)` para reducir el ruido del módulo Koin.
7. Pasar `PushRegistration` a `RegisterPushTokenUseCase`/`UnregisterPushTokenUseCase` para ser coherentes con los demás nombres.

Criterio de hecho: `grep -rn GlobalContext app/` aparece solo en el localizador; el receiver del recordatorio no importa repositorios.

---

## 4. Reglas para no volver atrás

Conviene dejarlas como convención del repositorio (por ejemplo en un `CONTRIBUTING.md` o en este mismo documento):

1. `domain` no importa nada de `data`, `presentation`, Android, Room, Ktor ni Supabase.
2. Un ViewModel solo depende de **casos de uso** (y de puertos triviales como `TodayProvider`). Nunca de repositorios.
3. Un ViewModel expone un único `StateFlow<UiState>` y funciones `onXxx`. La UI no calcula negocio.
4. Un caso de uso = una intención de usuario; si crece por encima de ~60 líneas, se parte.
5. Una interfaz de repositorio = un tema. Si una clase de prueba tiene que implementar métodos que no usa, se parte.
6. Nada de `Clock.System`, `Dispatchers.*` ni `GlobalContext` fuera de los módulos de Koin y de los receivers de Android.
7. Un archivo de pantalla Compose no pasa de ~250 líneas; los componentes reutilizables van a `components/`.
8. Todo VM nuevo entra con su test.

## 5. Orden recomendado y qué no hacer

- Empezar por **Fase 0 y Fase 1**: dan seguridad y limpian sin riesgo; se pueden entregar en una sola PR.
- Fases 3 y 4 pueden hacerse en paralelo (tocan archivos distintos). La Fase 5 conviene hacerla sola.
- **No** conviene: pasar a un framework de navegación o MVI distinto, introducir módulos Gradle separados por capa, ni eliminar los casos de uso triviales. El coste supera el beneficio para una app de este tamaño (unas 7.500 líneas en `commonMain`).
- Si se quiere reforzar el límite entre capas por herramienta, la opción de menor coste es una regla de Konsist o `ArchUnit`-style en un test que falle si `domain` importa `data` o `presentation` importa `data` (hoy se cumple y no hay nada que lo garantice).
