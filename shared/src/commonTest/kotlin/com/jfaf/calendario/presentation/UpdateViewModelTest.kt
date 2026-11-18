package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.model.UpdateStatus
import com.jfaf.calendario.domain.repository.AppConfigRepository
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.domain.repository.SettingsRepository
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.usecase.CheckForUpdateUseCase
import com.jfaf.calendario.domain.usecase.DismissRecommendedUpdateUseCase
import com.jfaf.calendario.domain.usecase.ObserveDismissedUpdateUseCase
import com.jfaf.calendario.presentation.update.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {
    private class FakeConfig(var result: () -> AppVersionPolicy?) : AppConfigRepository {
        var calls = 0
        override suspend fun fetchPolicy(): AppVersionPolicy? { calls++; return result() }
    }

    private class FakeSettings : SettingsRepository {
        val dismissed = MutableStateFlow(0)
        override fun observeThemeMode(): Flow<ThemeMode> = MutableStateFlow(ThemeMode.System)
        override suspend fun setThemeMode(mode: ThemeMode) {}
        override fun observeAppStyle(): Flow<AppStyle> = MutableStateFlow(AppStyle.Pop)
        override suspend fun setAppStyle(style: AppStyle) {}
        override fun observeDismissedRecommendedVersion(): Flow<Int> = dismissed
        override suspend fun setDismissedRecommendedVersion(versionCode: Int) { dismissed.value = versionCode }
    }

    private val installed = object : AppVersionProvider {
        override val versionCode = 5
        override val versionName = "test"
    }
    private val clock = TestTimeSource()
    private val foreground = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val settings = FakeSettings()

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun policy(min: Int = 1, recommended: Int = 1) = AppVersionPolicy(min, recommended, null, null)

    private fun viewModel(config: FakeConfig) = UpdateViewModel(
        CheckForUpdateUseCase(config, installed),
        ObserveDismissedUpdateUseCase(settings),
        DismissRecommendedUpdateUseCase(settings),
        foreground,
        clock,
        60.minutes,
    )

    private fun TestScope.settle() = advanceUntilIdle()

    @Test
    fun versionPorDebajoDelMinimoDaRequired() = runTest {
        val vm = viewModel(FakeConfig { policy(min = 6, recommended = 6) })
        settle()
        assertIs<UpdateStatus.Required>(vm.status.value)
    }

    @Test
    fun primerPlanoDentroDeLaHoraNoVuelveAConsultar() = runTest {
        val config = FakeConfig { policy() }
        viewModel(config); settle()
        clock += 30.minutes
        foreground.tryEmit(Unit); settle()
        assertEquals(1, config.calls)
        clock += 31.minutes
        foreground.tryEmit(Unit); settle()
        assertEquals(2, config.calls)
    }

    @Test
    fun unknownSeReintentaEnElSiguientePrimerPlano() = runTest {
        val config = FakeConfig { null }
        val vm = viewModel(config); settle()
        assertEquals(UpdateStatus.Unknown, vm.status.value)
        config.result = { policy(min = 9, recommended = 9) }
        foreground.tryEmit(Unit); settle()
        assertEquals(2, config.calls)
        assertIs<UpdateStatus.Required>(vm.status.value)
    }

    @Test
    fun fallarDespuesDeUnaRespuestaValidaConservaElUltimoEstado() = runTest {
        val config = FakeConfig { policy(min = 9, recommended = 9) }
        val vm = viewModel(config); settle()
        config.result = { null }
        clock += 61.minutes
        foreground.tryEmit(Unit); settle()
        assertIs<UpdateStatus.Required>(vm.status.value)
    }

    @Test
    fun descartarLaRecomendadaLaOcultaHastaQueElServidorLaSuba() = runTest {
        val config = FakeConfig { policy(recommended = 7) }
        val vm = viewModel(config); settle()
        assertIs<UpdateStatus.Recommended>(vm.status.value)
        vm.onDismissRecommended(); settle()
        assertEquals(UpdateStatus.UpToDate, vm.status.value)

        config.result = { policy(recommended = 8) }
        clock += 61.minutes
        foreground.tryEmit(Unit); settle()
        assertIs<UpdateStatus.Recommended>(vm.status.value)
    }

    @Test
    fun descartarNoAfectaAUnaActualizacionObligatoria() = runTest {
        val vm = viewModel(FakeConfig { policy(min = 7, recommended = 7) }); settle()
        vm.onDismissRecommended(); settle()
        assertIs<UpdateStatus.Required>(vm.status.value)
    }
}
