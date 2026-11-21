package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.usecase.ChangePasswordUseCase
import com.jfaf.calendario.domain.usecase.CompletePasswordResetUseCase
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.RequestPasswordResetUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.domain.usecase.SignInUseCase
import com.jfaf.calendario.domain.usecase.SignOutUseCase
import com.jfaf.calendario.domain.usecase.SignUpUseCase
import com.jfaf.calendario.domain.usecase.UnregisterPushTokenUseCase
import com.jfaf.calendario.fakes.FakeAuthRepository
import com.jfaf.calendario.fakes.FakeSocialRepository
import com.jfaf.calendario.fakes.FakeSyncRepository
import com.jfaf.calendario.presentation.settings.AccountMode
import com.jfaf.calendario.presentation.settings.AccountNotice
import com.jfaf.calendario.presentation.settings.AccountViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private lateinit var auth: FakeAuthRepository
    private lateinit var sync: FakeSyncRepository
    private lateinit var vm: AccountViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        auth = FakeAuthRepository()
        sync = FakeSyncRepository()
        val social = FakeSocialRepository()
        vm = AccountViewModel(
            observeAuth = ObserveAuthStateUseCase(auth),
            observeSync = ObserveSyncStateUseCase(sync),
            signIn = SignInUseCase(auth),
            signUp = SignUpUseCase(auth),
            signOut = SignOutUseCase(auth),
            requestSync = RequestSyncUseCase(sync),
            requestPasswordReset = RequestPasswordResetUseCase(auth),
            completePasswordReset = CompletePasswordResetUseCase(auth),
            changePassword = ChangePasswordUseCase(auth),
            unregisterPush = UnregisterPushTokenUseCase(social, null),
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun successfulSignInClearsTheForm() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onEmailChange("ana@correo.es")
        vm.onPasswordChange("secreto1")
        vm.onSignIn()
        val state = vm.uiState.value
        assertEquals("", state.password, "la contraseña no debe quedar en el estado tras entrar")
        assertEquals("", state.email)
        assertNull(state.error)
        assertFalse(state.busy)
        assertEquals(AuthState.SignedIn("user-1", "ana@correo.es"), state.auth)
    }

    @Test
    fun invalidEmailDoesNotCallServer() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onEmailChange("sin-arroba")
        vm.onPasswordChange("secreto1")
        vm.onSignIn()
        assertEquals(AuthError.InvalidEmail, vm.uiState.value.error)
        assertTrue(auth.calls.none { it.startsWith("signIn") })
        assertEquals("secreto1", vm.uiState.value.password, "con error se conservan los datos escritos")
    }

    @Test
    fun serverErrorKeepsTheFormAndShowsTheError() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        auth.signInError = AuthError.InvalidCredentials
        vm.onEmailChange("ana@correo.es")
        vm.onPasswordChange("secreto1")
        vm.onSignIn()
        val state = vm.uiState.value
        assertEquals(AuthError.InvalidCredentials, state.error)
        assertEquals("ana@correo.es", state.email)
        assertFalse(state.busy)
    }

    @Test
    fun editingAFieldClearsTheError() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        auth.signInError = AuthError.InvalidCredentials
        vm.onEmailChange("ana@correo.es")
        vm.onPasswordChange("secreto1")
        vm.onSignIn()
        vm.onPasswordChange("secreto2")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun forgotPasswordFlowMovesThroughCodeStep() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onEmailChange("ana@correo.es")
        vm.onForgotPassword()
        assertEquals(AccountMode.ForgotEmail, vm.uiState.value.mode)
        assertEquals("ana@correo.es", vm.uiState.value.email, "el correo se conserva al pedir el código")

        vm.onSendCode()
        val state = vm.uiState.value
        assertEquals(AccountMode.ForgotCode, state.mode)
        assertEquals(AccountNotice.CodeSent, state.notice)
        assertTrue(auth.calls.contains("reset:ana@correo.es"))

        vm.onBackToSignIn()
        assertEquals(AccountMode.SignIn, vm.uiState.value.mode)
        assertEquals("ana@correo.es", vm.uiState.value.email)
    }

    @Test
    fun changePasswordRejectsMismatchWithoutCallingServer() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onStartChangePassword()
        vm.onNewPasswordChange("Abcdef123")
        vm.onRepeatPasswordChange("Abcdef124")
        vm.onSaveNewPassword()
        assertEquals(AuthError.PasswordMismatch, vm.uiState.value.error)
        assertTrue(auth.calls.none { it == "update" })
        assertEquals(AccountMode.ChangePassword, vm.uiState.value.mode)
    }

    @Test
    fun changePasswordSuccessClearsPasswordsAndNotifies() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onStartChangePassword()
        vm.onNewPasswordChange("Abcdef123")
        vm.onRepeatPasswordChange("Abcdef123")
        vm.onSaveNewPassword()
        val state = vm.uiState.value
        assertTrue(auth.calls.contains("update"))
        assertEquals(AccountNotice.PasswordChanged, state.notice)
        assertEquals("", state.newPassword)
        assertEquals("", state.repeatPassword)
        assertEquals(AccountMode.SignIn, state.mode)
    }

    @Test
    fun signOutEndsSessionAndSyncNowRequestsSync() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onEmailChange("ana@correo.es")
        vm.onPasswordChange("secreto1")
        vm.onSignIn()
        vm.onSignOut()
        assertEquals(AuthState.SignedOut, vm.uiState.value.auth)

        sync.setState(SyncState.Idle(null))
        vm.onSyncNow()
        assertEquals(1, sync.syncRequests)
    }
}
