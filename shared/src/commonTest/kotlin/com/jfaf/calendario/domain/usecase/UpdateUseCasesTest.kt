package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.model.UpdateStatus
import com.jfaf.calendario.domain.repository.AppConfigRepository
import com.jfaf.calendario.domain.repository.AppVersionProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateUseCasesTest {
    private val policy = AppVersionPolicy(minVersionCode = 5, recommendedVersionCode = 8, message = "Arreglo importante")

    private fun version(code: Int) = object : AppVersionProvider {
        override val versionCode = code
        override val versionName = "test"
    }

    private fun repo(block: suspend () -> AppVersionPolicy?) = object : AppConfigRepository {
        override suspend fun fetchPolicy(): AppVersionPolicy? = block()
    }

    @Test
    fun withoutPolicyIsUnknown() {
        assertEquals(UpdateStatus.Unknown, evaluateUpdateStatus(1, null))
    }

    @Test
    fun belowMinimumIsRequired() {
        assertEquals(UpdateStatus.Required(policy), evaluateUpdateStatus(4, policy))
        assertEquals(UpdateStatus.Required(policy), evaluateUpdateStatus(1, policy))
    }

    @Test
    fun exactlyMinimumIsRecommendedNotRequired() {
        assertEquals(UpdateStatus.Recommended(policy), evaluateUpdateStatus(5, policy))
        assertEquals(UpdateStatus.Recommended(policy), evaluateUpdateStatus(7, policy))
    }

    @Test
    fun atOrAboveRecommendedIsUpToDate() {
        assertEquals(UpdateStatus.UpToDate, evaluateUpdateStatus(8, policy))
        assertEquals(UpdateStatus.UpToDate, evaluateUpdateStatus(99, policy))
    }

    @Test
    fun minEqualsRecommendedNeverRecommends() {
        val p = AppVersionPolicy(minVersionCode = 3, recommendedVersionCode = 3)
        assertEquals(UpdateStatus.Required(p), evaluateUpdateStatus(2, p))
        assertEquals(UpdateStatus.UpToDate, evaluateUpdateStatus(3, p))
    }

    @Test
    fun checkEvaluatesFetchedPolicy() = runTest {
        assertEquals(UpdateStatus.Required(policy), CheckForUpdateUseCase(repo { policy }, version(2))())
        assertEquals(UpdateStatus.UpToDate, CheckForUpdateUseCase(repo { policy }, version(8))())
    }

    @Test
    fun checkWithoutPolicyIsUnknown() = runTest {
        assertEquals(UpdateStatus.Unknown, CheckForUpdateUseCase(repo { null }, version(1))())
    }

    @Test
    fun checkFailsOpenOnException() = runTest {
        val status = CheckForUpdateUseCase(repo { error("sin red") }, version(1))()
        assertEquals(UpdateStatus.Unknown, status)
    }

    @Test
    fun checkFailsOpenOnTimeout() = runTest {
        val slow = repo { delay(60_000); policy }
        assertEquals(UpdateStatus.Unknown, CheckForUpdateUseCase(slow, version(1), timeoutMillis = 5_000)())
    }

    @Test
    fun checkPropagatesCancellation() = runTest {
        val cancelled = repo { throw CancellationException("cancelado") }
        assertFailsWith<CancellationException> { CheckForUpdateUseCase(cancelled, version(1))() }
    }
}
