package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.backup.encodeBackupJson
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.BackupSnapshot
import com.jfaf.calendario.domain.model.ReminderSettings
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.repository.BackupStore
import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ExportBackupUseCase
import com.jfaf.calendario.domain.usecase.ImportBackupUseCase
import com.jfaf.calendario.domain.usecase.ObserveAppStyleUseCase
import com.jfaf.calendario.domain.usecase.ObserveReminderSettingsUseCase
import com.jfaf.calendario.domain.usecase.ObserveThemeModeUseCase
import com.jfaf.calendario.domain.usecase.SetAppStyleUseCase
import com.jfaf.calendario.domain.usecase.SetReminderEnabledUseCase
import com.jfaf.calendario.domain.usecase.SetReminderTimeUseCase
import com.jfaf.calendario.domain.usecase.SetThemeModeUseCase
import com.jfaf.calendario.fakes.FakeClock
import com.jfaf.calendario.fakes.FakeSettingsRepository
import com.jfaf.calendario.presentation.backup.BackupFileIo
import com.jfaf.calendario.presentation.backup.BackupNotice
import com.jfaf.calendario.presentation.backup.BackupViewModel
import com.jfaf.calendario.presentation.settings.ReminderSystem
import com.jfaf.calendario.presentation.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeReminderSettingsRepository : ReminderSettingsRepository {
    private val state = MutableStateFlow(ReminderSettings())
    override fun observeReminderSettings(): Flow<ReminderSettings> = state
    override suspend fun setReminderEnabled(enabled: Boolean) { state.value = state.value.copy(enabled = enabled) }
    override suspend fun setReminderTime(minutesOfDay: Int) { state.value = state.value.copy(minutesOfDay = minutesOfDay) }
}

private class FakeReminderSystem(
    override val supported: Boolean = true,
    var allowed: Boolean = true,
    var grantOnRequest: Boolean = true,
) : ReminderSystem {
    override fun notificationsAllowed(): Boolean = allowed
    override fun requestNotifications(onResult: (granted: Boolean) -> Unit) = onResult(grantOnRequest)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var settings: FakeSettingsRepository
    private lateinit var reminders: FakeReminderSettingsRepository
    private lateinit var system: FakeReminderSystem
    private lateinit var vm: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        settings = FakeSettingsRepository()
        reminders = FakeReminderSettingsRepository()
        system = FakeReminderSystem()
        vm = SettingsViewModel(
            observeThemeMode = ObserveThemeModeUseCase(settings),
            setThemeMode = SetThemeModeUseCase(settings),
            observeAppStyle = ObserveAppStyleUseCase(settings),
            setAppStyle = SetAppStyleUseCase(settings),
            observeReminder = ObserveReminderSettingsUseCase(reminders),
            setReminderEnabled = SetReminderEnabledUseCase(reminders),
            setReminderTime = SetReminderTimeUseCase(reminders),
            reminderSystem = system,
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun themeAndStyleArePersistedAndObserved() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.themeMode.collect {} }
        backgroundScope.launch { vm.appStyle.collect {} }
        vm.onThemeModeSelected(ThemeMode.Dark)
        vm.onAppStyleSelected(AppStyle.Neon)
        assertEquals(ThemeMode.Dark, vm.themeMode.value)
        assertEquals(AppStyle.Neon, vm.appStyle.value)
    }

    @Test
    fun enablingRemindersWithPermissionTurnsThemOn() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.reminder.collect {} }
        system.allowed = true
        vm.onReminderEnabledChange(true)
        assertTrue(vm.reminder.value.enabled)
        assertFalse(vm.reminder.value.permissionDenied)
    }

    @Test
    fun deniedPermissionKeepsRemindersOffAndFlagsIt() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.reminder.collect {} }
        system.allowed = false
        system.grantOnRequest = false
        vm.onReminderEnabledChange(true)
        assertFalse(vm.reminder.value.enabled)
        assertTrue(vm.reminder.value.permissionDenied)
    }

    @Test
    fun grantedPermissionAfterRequestTurnsRemindersOn() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.reminder.collect {} }
        system.allowed = false
        system.grantOnRequest = true
        vm.onReminderEnabledChange(true)
        assertTrue(vm.reminder.value.enabled)
        assertFalse(vm.reminder.value.permissionDenied)
    }

    @Test
    fun reminderTimeIsStored() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.reminder.collect {} }
        vm.onReminderTimeSelected(7 * 60 + 30)
        assertEquals(7 * 60 + 30, vm.reminder.value.minutesOfDay)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val today = LocalDate(2026, 11, 19)
    private val empty = BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList())

    private class FakeStore(var snapshot: BackupSnapshot) : BackupStore {
        var replaced: BackupSnapshot? = null
        override suspend fun readAll(): BackupSnapshot = snapshot
        override suspend fun replaceAll(snapshot: BackupSnapshot) { replaced = snapshot }
    }

    private class FakeFileIo(private val saveOk: Boolean = true, private val picked: String? = null) : BackupFileIo {
        val saved = mutableListOf<String>()
        override fun save(suggestedName: String, mimeType: String, content: String, onDone: (Boolean) -> Unit) {
            saved += suggestedName
            onDone(saveOk)
        }
        override fun open(onPicked: (String?) -> Unit) = onPicked(picked)
    }

    private lateinit var store: FakeStore
    private lateinit var vm: BackupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = FakeStore(empty)
        vm = BackupViewModel(ExportBackupUseCase(store), ImportBackupUseCase(store), TodayProvider { today }, FakeClock(0L))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun exportWritesJsonAndTwoCsvFiles() = runTest(UnconfinedTestDispatcher()) {
        val io = FakeFileIo()
        vm.exportAll(io)
        assertEquals(
            listOf("calendario-2026-11-19.json", "turnos-2026-11-19.csv", "notas-2026-11-19.csv"),
            io.saved,
        )
        assertEquals(BackupNotice.Exported, vm.uiState.value.notice)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun cancelledSaveStopsAndReportsFailure() = runTest(UnconfinedTestDispatcher()) {
        val io = FakeFileIo(saveOk = false)
        vm.exportAll(io)
        assertEquals(listOf("calendario-2026-11-19.json"), io.saved)
        assertEquals(BackupNotice.Failed, vm.uiState.value.notice)
    }

    @Test
    fun pickedFileWaitsForConfirmationBeforeReplacingData() = runTest(UnconfinedTestDispatcher()) {
        val text = encodeBackupJson(empty, "2026-11-19T00:00:00Z")
        vm.pickImport(FakeFileIo(picked = text))
        assertEquals(text, vm.uiState.value.pendingImport)
        assertNull(store.replaced, "no toca los datos hasta confirmar")

        vm.cancelImport()
        assertNull(vm.uiState.value.pendingImport)
        assertNull(store.replaced)
    }

    @Test
    fun confirmedValidImportReplacesEverything() = runTest(UnconfinedTestDispatcher()) {
        val text = encodeBackupJson(empty, "2026-11-19T00:00:00Z")
        vm.pickImport(FakeFileIo(picked = text))
        vm.confirmImport()
        assertEquals(empty, store.replaced)
        assertEquals(BackupNotice.Imported, vm.uiState.value.notice)
    }

    @Test
    fun invalidFileIsReportedAndNothingIsReplaced() = runTest(UnconfinedTestDispatcher()) {
        vm.pickImport(FakeFileIo(picked = "esto no es json"))
        vm.confirmImport()
        assertEquals(BackupNotice.InvalidFile, vm.uiState.value.notice)
        assertNull(store.replaced)
    }
}
