package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.model.UpdateStatus
import com.jfaf.calendario.domain.repository.AppConfigRepository
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Compara la versión instalada con la política. Sin política (`null`) no se puede decidir: `Unknown`. */
fun evaluateUpdateStatus(currentVersionCode: Int, policy: AppVersionPolicy?): UpdateStatus = when {
    policy == null -> UpdateStatus.Unknown
    currentVersionCode < policy.minVersionCode -> UpdateStatus.Required(policy)
    currentVersionCode < policy.recommendedVersionCode -> UpdateStatus.Recommended(policy)
    else -> UpdateStatus.UpToDate
}

/**
 * Consulta la política al servidor y la evalúa. Falla en abierto: cualquier error o demora
 * (más de [timeoutMillis]) da `Unknown`, nunca bloquea al usuario.
 */
class CheckForUpdateUseCase(
    private val repository: AppConfigRepository,
    private val versionProvider: AppVersionProvider,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    suspend operator fun invoke(): UpdateStatus {
        val policy = try {
            withTimeoutOrNull(timeoutMillis) { repository.fetchPolicy() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return evaluateUpdateStatus(versionProvider.versionCode, policy)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    }
}

class ObserveDismissedUpdateUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Int> = settings.observeDismissedRecommendedVersion()
}

/** Recuerda que el usuario descartó el aviso de esta versión recomendada; no vuelve a salir hasta que el servidor la suba. */
class DismissRecommendedUpdateUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(recommendedVersionCode: Int) = settings.setDismissedRecommendedVersion(recommendedVersionCode)
}
