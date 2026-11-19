# Convenciones de arquitectura

Complementa `docs/revision-arquitectura.md` (sección 4). Estas reglas se mantienen como convención del repositorio.

## Reglas

1. `domain` no importa nada de `data`, `presentation`, Android, Room, Ktor ni Supabase.
2. Un ViewModel depende solo de **casos de uso** (y de puertos triviales como `TodayProvider`). Nunca de repositorios.
3. Un ViewModel expone un único `StateFlow<UiState>` y funciones `onXxx`. La UI no calcula negocio.
4. Un caso de uso = una intención de usuario; si crece por encima de ~60 líneas, se parte.
5. Una interfaz de repositorio = un tema (ver `LinkRepository`, `SharedScheduleRepository`, `PushTokenRepository`).
6. Nada de `Clock.System`, `Dispatchers.*` ni `GlobalContext` fuera de los módulos de Koin, de la implementación por defecto (`DefaultAppDispatchers`) y de `AndroidServiceLocator`.
7. Un archivo de pantalla Compose no pasa de ~250 líneas; los componentes reutilizables van a `components/`.
8. Todo ViewModel nuevo entra con su test.

## Dónde viven los puertos de plataforma

`domain/platform/`: `ReminderSystem`, `UpdateLauncher`, `BackupFileIo`, `AppForegroundEvents` (interfaz) y `ForegroundNotifier` (lo emite cada plataforma). La implementación de cada plataforma vive en su módulo (`app`, `desktopApp`).

## Informes de errores

`ErrorReporter` es el puerto de informes (solo `report`). Las herramientas de diagnóstico (`canSendTest`, `sendTestReport`) están aparte, en `DiagnosticsTools`, y solo las implementan las plataformas que las ofrecen.

## Casos de uso triviales: se mantienen

Varios casos de uso solo delegan una llamada (`SignOutUseCase`, `RequestSyncUseCase`, `SetThemeModeUseCase`…). **Decisión: se mantienen.** Cada uno es el único punto de entrada de una intención, y el coste es solo declarativo (una factoría en `DomainModule`). Quitarlos haría que los ViewModels dependieran de repositorios, lo que rompe la regla 2.

## Componentes de Android sin constructor

Receivers y widget los crea el sistema, así que piden sus dependencias a Koin a través de `AndroidServiceLocator` (en `app/`), no con `GlobalContext`. Sus ámbitos de corrutina son el `appScope` de Koin (`named("appScope")`).
