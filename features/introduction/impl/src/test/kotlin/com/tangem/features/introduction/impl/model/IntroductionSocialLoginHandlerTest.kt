package com.tangem.features.introduction.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionSocialLoginHandlerTest {

    private val socialLoginUseCase: SocialLoginUseCase = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val appCoroutineScope: AppCoroutineScope = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val getAvailableSocialLoginProvidersUseCase: GetAvailableSocialLoginProvidersUseCase = mockk()

    private val handler = IntroductionSocialLoginHandler(
        getAvailableSocialLoginProvidersUseCase = getAvailableSocialLoginProvidersUseCase,
        socialLoginUseCase = socialLoginUseCase,
        userWalletsListRepository = userWalletsListRepository,
        appCoroutineScope = appCoroutineScope,
        urlOpener = urlOpener,
        router = router,
        uiMessageSender = uiMessageSender,
    )

    private val host = RecordingHost()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getAvailableSocialLoginProvidersUseCase,
            socialLoginUseCase,
            userWalletsListRepository,
            urlOpener,
            router,
            uiMessageSender,
        )
        host.reset()
    }

    @Test
    fun `GIVEN wallet created WHEN signIn THEN wallet is selected and the main screen is opened`() = runTest {
        // Arrange
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any(), any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        coVerify(exactly = 1) { userWalletsListRepository.select(WALLET_ID) }
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(host.finishedCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN the wallet cannot be selected WHEN signIn THEN the main screen stays closed`() = runTest {
        // Arrange — opening the portfolio of whichever wallet was selected before would read as success
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any(), any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        coEvery { userWalletsListRepository.select(WALLET_ID) } returns SELECT_ERROR.left()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        verify { router wasNot Called }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
    }

    @Test
    fun `GIVEN the consent declined WHEN signIn THEN provider is signed out and no message is shown`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any()) } returns SocialLoginError.ConsentDeclined.left()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        coVerify(exactly = 1) { socialLoginUseCase.signOut(PROVIDER) }
        verify { uiMessageSender wasNot Called }
        verify { router wasNot Called }
        coVerify(exactly = 0) { userWalletsListRepository.select(any()) }
    }

    @Test
    fun `GIVEN sign-in failed WHEN signIn THEN the entry points reopen only after the sign-out`() = runTest {
        // Arrange — a retry entering signIn while the sign-out still runs would read a half-cleared account
        val order = mutableListOf<String>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any()) } returns SocialLoginError.NetworkError.left()
        coEvery { socialLoginUseCase.signOut(PROVIDER) } answers { order += "signOut" }
        host.onFinishedCallback = { order += "finished" }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(order).containsExactly("signOut", "finished").inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a failure WHEN signIn THEN it is shown the way the spec puts it`(model: FailureModel) = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any()) } returns model.error.left()

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        if (model.isSupportDialog) {
            val message = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(message)) }
            assertThat(message.captured.firstAction).isNotNull()
        } else {
            verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
            verify(exactly = 0) { uiMessageSender.send(any<DialogMessage>()) }
        }
    }

    @Test
    fun `GIVEN two failed attempts WHEN support is contacted THEN each opens the chat under its own id`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any()) } returns SocialLoginError.BackupNotFound.left()
        val dialogs = mutableListOf<DialogMessage>()
        val routes = mutableListOf<AppRoute>()
        every { uiMessageSender.send(capture(dialogs)) } just Runs
        every { router.push(capture(routes), any()) } just Runs

        // Act
        handler.signIn(PROVIDER, host)
        dialogs.last().firstAction.onClick()
        handler.signIn(PROVIDER, host)
        dialogs.last().firstAction.onClick()

        // Assert
        val chats = routes.filterIsInstance<AppRoute.Usedesk>()
        assertThat(chats.map { it.source }).containsExactly(
            AnalyticsParam.ScreensSources.SocialLogin,
            AnalyticsParam.ScreensSources.SocialLogin,
        )
        assertThat(chats.mapNotNull { it.clientEmail }.distinct()).hasSize(2)
    }

    @Test
    fun `GIVEN a newer attempt WHEN the older dialog contacts support THEN the chat keeps the older id`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any()) } returns SocialLoginError.BackupNotFound.left()
        val dialogs = mutableListOf<DialogMessage>()
        val routes = mutableListOf<AppRoute>()
        every { uiMessageSender.send(capture(dialogs)) } just Runs
        every { router.push(capture(routes), any()) } just Runs
        handler.signIn(PROVIDER, host)
        handler.signIn(PROVIDER, host)

        // Act
        dialogs[1].firstAction.onClick()
        dialogs[0].firstAction.onClick()

        // Assert
        val ids = routes.filterIsInstance<AppRoute.Usedesk>().map { it.clientEmail }
        assertThat(ids).hasSize(2)
        assertThat(ids.distinct()).hasSize(2)
    }

    data class FailureModel(val error: SocialLoginError, val isSupportDialog: Boolean)

    private fun provideTestModels() = listOf(
        FailureModel(error = SocialLoginError.SignInCanceled, isSupportDialog = false),
        FailureModel(error = SocialLoginError.NetworkError, isSupportDialog = false),
        FailureModel(error = SocialLoginError.SessionExpired, isSupportDialog = false),
        FailureModel(error = SocialLoginError.ServerError(), isSupportDialog = false),
        FailureModel(error = SocialLoginError.BackupWriteFailed(), isSupportDialog = false),
        FailureModel(error = SocialLoginError.BindingConflict, isSupportDialog = false),
        FailureModel(error = SocialLoginError.BackupNotFound, isSupportDialog = true),
        FailureModel(error = SocialLoginError.InvalidBackupFile, isSupportDialog = true),
        FailureModel(error = SocialLoginError.RecoveryKeyDecryptionFailed(), isSupportDialog = true),
        FailureModel(error = SocialLoginError.PassphraseNotSupported, isSupportDialog = true),
        FailureModel(error = SocialLoginError.UnsupportedBackupVersion(version = 2), isSupportDialog = true),
        FailureModel(error = SocialLoginError.CloudUnavailable, isSupportDialog = true),
    )

    @Test
    fun `GIVEN sign-in failed WHEN signIn THEN the progress stays up until the sign-out finishes`() = runTest {
        // Arrange
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any()) } answers {
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            SocialLoginError.NetworkError.left()
        }
        var progressDuringSignOut: SocialLoginProgressUM? = null
        coEvery { socialLoginUseCase.signOut(PROVIDER) } answers { progressDuringSignOut = host.progress.last() }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(progressDuringSignOut).isNotNull()
        assertThat(host.progress.last()).isNull()
    }

    @Test
    fun `GIVEN stages reported WHEN signIn THEN progress is shown and then cleared`() = runTest {
        // Arrange
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any()) } answers {
            onStageChanged.captured(SocialLoginStage.SigningIn)
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            onStageChanged.captured(SocialLoginStage.RestoringWallet)
            SocialLoginOutcome.WalletRestored(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(host.progress).hasSize(4)
        assertThat(host.progress[0]).isNull()
        assertThat(host.progress[1]).isNotNull()
        assertThat(host.progress[2]).isNotNull()
        assertThat(host.progress[3]).isNull()
    }

    @Test
    fun `GIVEN the consent is confirmed WHEN signIn THEN the choice reaches the flow`() = runTest {
        // Arrange
        val requestConsent = slot<suspend () -> SocialLoginConsent?>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), capture(requestConsent)) } coAnswers {
            host.onConsentShown = { consent ->
                consent.onAcknowledgedChange(true)
                host.consent!!.onEmailSharingChange(true)
                host.consent!!.onConfirmClick()
            }
            assertThat(requestConsent.captured()).isEqualTo(SocialLoginConsent(isEmailSharingAllowed = true))
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(host.consent).isNull()
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
    }

    @Test
    fun `GIVEN the consent is asked WHEN it is shown THEN the progress underneath is taken down`() = runTest {
        // Arrange
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        val requestConsent = slot<suspend () -> SocialLoginConsent?>()
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), capture(requestConsent))
        } coAnswers {
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            host.onConsentShown = { consent ->
                consent.onAcknowledgedChange(true)
                host.consent!!.onConfirmClick()
            }
            requestConsent.captured()
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, host)

        // Assert
        assertThat(host.progress).containsExactly(host.progress[0], null, null).inOrder()
        assertThat(host.progress[0]).isNotNull()
    }

    private class RecordingHost : IntroductionSocialLoginHandler.Host {

        val progress = mutableListOf<SocialLoginProgressUM?>()
        var finishedCount = 0
            private set

        var consent: SocialLoginConsentUM? = null
            private set

        var onConsentShown: (SocialLoginConsentUM) -> Unit = {}

        private var isDispatching = false

        override fun onProgress(progress: SocialLoginProgressUM?) {
            this.progress += progress
        }

        override fun onConsent(consent: SocialLoginConsentUM?) {
            this.consent = consent
            if (consent == null || isDispatching) return
            isDispatching = true
            try {
                onConsentShown(consent)
            } finally {
                isDispatching = false
            }
        }

        var onFinishedCallback: () -> Unit = {}

        override fun onFinished() {
            finishedCount++
            onFinishedCallback()
        }

        fun reset() {
            progress.clear()
            consent = null
            finishedCount = 0
            onConsentShown = {}
            onFinishedCallback = {}
        }
    }

    private companion object {
        val PROVIDER = SocialLoginProvider.Google
        val WALLET_ID = UserWalletId(stringValue = "011")
        val SELECT_ERROR = SelectWalletError.UnableToSelectUserWallet
    }
}