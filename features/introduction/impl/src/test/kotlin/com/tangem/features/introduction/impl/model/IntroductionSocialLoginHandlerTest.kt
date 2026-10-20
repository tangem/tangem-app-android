package com.tangem.features.introduction.impl.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginConsent
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.introduction.impl.analytics.SocialLoginAnalyticsEvents
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.test.core.ProvideTestModels
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
import io.mockk.verifyOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionSocialLoginHandlerTest {

    private val socialLoginUseCase: SocialLoginUseCase = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val referral: IntroductionReferral = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val getAvailableSocialLoginProvidersUseCase: GetAvailableSocialLoginProvidersUseCase = mockk()

    private val userWallet: UserWallet = mockk { every { walletId } returns WALLET_ID }

    private val handler = IntroductionSocialLoginHandler(
        getAvailableSocialLoginProvidersUseCase = getAvailableSocialLoginProvidersUseCase,
        socialLoginUseCase = socialLoginUseCase,
        userWalletsListRepository = userWalletsListRepository,
        analyticsEventHandler = analyticsEventHandler,
        trackingContextProxy = trackingContextProxy,
        referral = referral,
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
            analyticsEventHandler,
            trackingContextProxy,
            urlOpener,
            router,
            uiMessageSender,
        )
        coEvery { userWalletsListRepository.select(WALLET_ID) } returns userWallet.right()
        host.reset()
    }

    @Test
    fun `GIVEN wallet created WHEN signIn THEN wallet is selected and the main screen is opened`() = runTest {
        // Arrange
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any(), any(), any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        coVerify(exactly = 1) { userWalletsListRepository.select(WALLET_ID) }
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(host.finishedCount).isEqualTo(1)
    }

    @ParameterizedTest
    @MethodSource("provideSuccessToastModels")
    fun `GIVEN a wallet signed in WHEN signIn THEN the success toast is shown with its status icon`(
        model: SuccessToastModel,
    ) = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns model.outcome.right()
        val message = slot<SnackbarMessage>()
        every { uiMessageSender.send(capture(message)) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(message.captured).isEqualTo(
            SnackbarMessage(
                message = resourceReference(model.messageId),
                startIconId = CoreUiR.drawable.ic_success_20,
                isIconTinted = false,
            ),
        )
    }

    private fun provideSuccessToastModels() = listOf(
        SuccessToastModel(
            outcome = SocialLoginOutcome.WalletCreated(WALLET_ID),
            messageId = CoreResR.string.social_login_toast_wallet_created,
        ),
        SuccessToastModel(
            outcome = SocialLoginOutcome.WalletRestored(WALLET_ID),
            messageId = CoreResR.string.social_login_toast_wallet_added,
        ),
    )

    data class SuccessToastModel(val outcome: SocialLoginOutcome, val messageId: Int)

    @Test
    fun `GIVEN the wallet cannot be selected WHEN signIn THEN the main screen stays closed`() = runTest {
        // Arrange — opening the portfolio of whichever wallet was selected before would read as success
        coEvery {
            socialLoginUseCase.signIn(PROVIDER, any(), any(), any())
        } returns SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        coEvery { userWalletsListRepository.select(WALLET_ID) } returns SELECT_ERROR.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify { router wasNot Called }
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
    }

    @Test
    fun `GIVEN the consent declined WHEN signIn THEN provider is signed out and no message is shown`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.ConsentDeclined.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

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
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.NetworkError.left()
        coEvery { socialLoginUseCase.signOut(PROVIDER) } answers { order += "signOut" }
        host.onFinishedCallback = { order += "finished" }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(order).containsExactly("signOut", "finished").inOrder()
    }

    @Test
    fun `GIVEN the sign-in throws WHEN signIn THEN the progress is cleared and the entry points reopen`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } throws IllegalStateException()

        // Act
        val thrown = runCatching { handler.signIn(PROVIDER, SOURCE, host) }.exceptionOrNull()

        // Assert
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(host.progress.last()).isNull()
        assertThat(host.finishedCount).isEqualTo(1)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a failure WHEN signIn THEN it is shown the way the spec puts it`(model: FailureModel) = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns model.error.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

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

    @ParameterizedTest
    @MethodSource("provideGenericFailures")
    fun `GIVEN a failure with no wording of its own WHEN signIn THEN the generic snackbar is shown`(
        error: SocialLoginError,
    ) = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns error.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message).isEqualTo(resourceReference(CoreResR.string.social_login_error_generic))
    }

    private fun provideGenericFailures() = listOf(
        SocialLoginError.BindingConflict,
        SocialLoginError.NoProviderAccount,
        SocialLoginError.ProviderUnavailable,
        SocialLoginError.ServerError(),
    )

    @Test
    fun `GIVEN two failed attempts WHEN support is contacted THEN each opens the chat under its own id`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.BackupNotFound.left()
        val dialogs = mutableListOf<DialogMessage>()
        val routes = mutableListOf<AppRoute>()
        every { uiMessageSender.send(capture(dialogs)) } just Runs
        every { router.push(capture(routes), any()) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)
        dialogs.last().firstAction.onClick()
        handler.signIn(PROVIDER, SOURCE, host)
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
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.BackupNotFound.left()
        val dialogs = mutableListOf<DialogMessage>()
        val routes = mutableListOf<AppRoute>()
        every { uiMessageSender.send(capture(dialogs)) } just Runs
        every { router.push(capture(routes), any()) } just Runs
        handler.signIn(PROVIDER, SOURCE, host)
        handler.signIn(PROVIDER, SOURCE, host)

        // Act
        dialogs[1].firstAction.onClick()
        dialogs[0].firstAction.onClick()

        // Assert
        val ids = routes.filterIsInstance<AppRoute.Usedesk>().map { it.clientEmail }
        assertThat(ids).hasSize(2)
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun `GIVEN a failure WHEN signIn THEN it is reported as shown`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.NetworkError.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.ErrorShown>()) }
    }

    @Test
    fun `GIVEN the consent declined WHEN signIn THEN no error is reported`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns SocialLoginError.ConsentDeclined.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.ErrorShown>()) }
    }

    @Test
    fun `GIVEN a wallet created WHEN signIn THEN its creation is reported as a social login one`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        val events = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(events)) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        val created = events.filterIsInstance<OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully>()
        assertThat(created.single().params["Creation Type"]).isEqualTo("New Social Login")
        assertThat(created.single().params[AnalyticsParam.SOURCE]).isEqualTo(SOURCE.value)
    }

    @Test
    fun `GIVEN the selection fails WHEN a wallet is created THEN its creation is still reported`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        coEvery { userWalletsListRepository.select(WALLET_ID) } returns SELECT_ERROR.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(ofType<OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully>())
        }
    }

    @Test
    fun `GIVEN a wallet restored WHEN signIn THEN it is reported as a social login import`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns
            SocialLoginOutcome.WalletRestored(WALLET_ID).right()
        val events = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(events)) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        val created = events.filterIsInstance<OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully>()
        assertThat(created.single().params["Creation Type"]).isEqualTo("Social Login Import")
    }

    @Test
    fun `GIVEN the backup write fails WHEN retry is picked THEN the flow is told to retry`() = runTest {
        // Arrange
        val requestBackupRetry = slot<suspend () -> Boolean>()
        var answer: Boolean? = null
        val dialog = slot<DialogMessage>()
        every { uiMessageSender.send(capture(dialog)) } just Runs
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), capture(requestBackupRetry), any()) } coAnswers {
            answer = coroutineScope {
                val asked = async { requestBackupRetry.captured() }
                yield()
                dialog.captured.firstAction.onClick()
                asked.await()
            }
            SocialLoginError.ConsentDeclined.left()
        }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(answer).isTrue()
        assertThat(dialog.captured.message)
            .isEqualTo(resourceReference(CoreResR.string.social_login_error_backup_not_saved))
    }

    @ParameterizedTest
    @MethodSource("provideSilentErrors")
    fun `GIVEN the viewer backed out WHEN signIn THEN no error is shown`(error: SocialLoginError) = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns error.left()

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.ErrorShown>()) }
    }

    @Test
    fun `GIVEN the wallet is already on the device WHEN signIn THEN the already saved dialog is shown`() = runTest {
        // Arrange
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), any()) } returns
            SocialLoginError.WalletAlreadySaved.left()
        val message = slot<DialogMessage>()
        every { uiMessageSender.send(capture(message)) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(message.captured.message)
            .isEqualTo(resourceReference(CoreResR.string.user_wallet_list_error_wallet_already_saved))
    }

    private fun provideSilentErrors() = listOf(
        SocialLoginError.SignInCanceled,
        SocialLoginError.ConsentDeclined,
        SocialLoginError.BackupAbandoned,
    )

    data class FailureModel(val error: SocialLoginError, val isSupportDialog: Boolean)

    private fun provideTestModels() = listOf(
        FailureModel(error = SocialLoginError.SignInFailed(), isSupportDialog = false),
        FailureModel(error = SocialLoginError.NetworkError, isSupportDialog = false),
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
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any(), any()) } answers {
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            SocialLoginError.NetworkError.left()
        }
        var progressDuringSignOut: SocialLoginProgressUM? = null
        coEvery { socialLoginUseCase.signOut(PROVIDER) } answers { progressDuringSignOut = host.progress.last() }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(progressDuringSignOut).isNotNull()
        assertThat(host.progress.last()).isNull()
    }

    @Test
    fun `GIVEN stages reported WHEN signIn THEN progress is shown and then cleared`() = runTest {
        // Arrange
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any(), any()) } answers {
            onStageChanged.captured(SocialLoginStage.SigningIn)
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            onStageChanged.captured(SocialLoginStage.RestoringWallet)
            SocialLoginOutcome.WalletRestored(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(host.progress).hasSize(4)
        assertThat(host.progress[0]).isNull()
        assertThat(host.progress[1]).isEqualTo(SocialLoginProgressUM.Loader)
        assertThat(host.progress[2]).isInstanceOf(SocialLoginProgressUM.Stage::class.java)
        assertThat(host.progress[3]).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideStageMinDisplayModels")
    fun `GIVEN a stage finished at once WHEN signIn THEN the stage stays up for the minimum time`(
        model: StageMinDisplayModel,
    ) = runTest {
        // Arrange
        stubStageThenResult(stage = model.stage, workDurationMs = 0, result = model.outcome.right())

        // Act
        launch { handler.signIn(PROVIDER, SOURCE, host) }
        advanceTimeBy(STAGE_MIN_DISPLAY_MS - 1)
        runCurrent()
        val isLeftEarly = host.progress.last() == null
        advanceTimeBy(1)
        runCurrent()

        // Assert
        assertThat(isLeftEarly).isFalse()
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        assertThat(host.progress.last()).isNull()
    }

    private fun provideStageMinDisplayModels() = listOf(
        StageMinDisplayModel(
            stage = SocialLoginStage.CreatingWallet,
            outcome = SocialLoginOutcome.WalletCreated(WALLET_ID),
        ),
        StageMinDisplayModel(
            stage = SocialLoginStage.RestoringWallet,
            outcome = SocialLoginOutcome.WalletRestored(WALLET_ID),
        ),
    )

    data class StageMinDisplayModel(val stage: SocialLoginStage, val outcome: SocialLoginOutcome)

    @Test
    fun `GIVEN a stage outlasting the minimum time WHEN signIn succeeds THEN the main screen opens at once`() = runTest {
        // Arrange
        stubStageThenResult(
            stage = SocialLoginStage.CreatingWallet,
            workDurationMs = STAGE_MIN_DISPLAY_MS + 500,
            result = SocialLoginOutcome.WalletCreated(WALLET_ID).right(),
        )

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(currentTime).isEqualTo(STAGE_MIN_DISPLAY_MS + 500)
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
    }

    @Test
    fun `GIVEN a stage shown WHEN signIn fails at once THEN the failure is not held back`() = runTest {
        // Arrange
        stubStageThenResult(
            stage = SocialLoginStage.RestoringWallet,
            workDurationMs = 0,
            result = SocialLoginError.NetworkError.left(),
        )

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(currentTime).isEqualTo(0)
        verify(exactly = 1) { uiMessageSender.send(any<SnackbarMessage>()) }
        assertThat(host.progress.last()).isNull()
    }

    @Test
    fun `GIVEN a stage held for the minimum time WHEN the attempt is cancelled THEN it stops without navigating`() =
        runTest {
            // Arrange
            stubStageThenResult(
                stage = SocialLoginStage.CreatingWallet,
                workDurationMs = 0,
                result = SocialLoginOutcome.WalletCreated(WALLET_ID).right(),
            )
            val attempt = launch { handler.signIn(PROVIDER, SOURCE, host) }
            runCurrent()

            // Act
            attempt.cancel()
            advanceUntilIdle()

            // Assert
            assertThat(attempt.isCancelled).isTrue()
            verify(exactly = 0) { router.replaceAll(any()) }
            assertThat(host.progress.last()).isNull()
        }

    private fun stubStageThenResult(
        stage: SocialLoginStage,
        workDurationMs: Long,
        result: Either<SocialLoginError, SocialLoginOutcome>,
    ) {
        val onStageChanged = slot<(SocialLoginStage) -> Unit>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any(), any()) } coAnswers {
            onStageChanged.captured(stage)
            delay(workDurationMs)
            result
        }
    }

    @Test
    fun `GIVEN the consent is confirmed WHEN signIn THEN the choice reaches the flow`() = runTest {
        // Arrange
        val requestConsent = slot<suspend () -> SocialLoginConsent?>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), capture(requestConsent)) } coAnswers {
            host.onConsentShown = { consent ->
                consent.onAcknowledgedChange(true)
                host.consent!!.onEmailSharingChange(true)
                host.consent!!.onConfirmClick(CONSENT_TEXT)
            }
            assertThat(requestConsent.captured()).isEqualTo(SocialLoginConsent(isEmailSharingAllowed = true))
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

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
            socialLoginUseCase.signIn(PROVIDER, capture(onStageChanged), any(), capture(requestConsent))
        } coAnswers {
            onStageChanged.captured(SocialLoginStage.VerifyingAccount)
            host.onConsentShown = { consent ->
                consent.onAcknowledgedChange(true)
                host.consent!!.onConfirmClick(CONSENT_TEXT)
            }
            requestConsent.captured()
            SocialLoginOutcome.WalletCreated(WALLET_ID).right()
        }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        assertThat(host.progress).containsExactly(host.progress[0], null, null).inOrder()
        assertThat(host.progress[0]).isNotNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN the consent is confirmed WHEN a wallet is created THEN the marketing consent is reported once`(
        isEmailSharingAllowed: Boolean,
    ) = runTest {
        // Arrange
        stubSignIn(
            consentAction = ConsentAction.Confirm(isEmailSharingAllowed),
            result = SocialLoginOutcome.WalletCreated(WALLET_ID).right(),
        )
        val events = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(events)) } just Runs

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        val consent = events.filterIsInstance<SocialLoginAnalyticsEvents.MarketingEmailsConsent>().single()
        assertThat(consent.params).isEqualTo(
            mapOf(
                "Consent Type" to "Offers & Updates by email",
                "Consent Given" to if (isEmailSharingAllowed) "Yes" else "No",
                "Consent Text" to CONSENT_TEXT,
                AnalyticsParam.PRODUCT_TYPE to AnalyticsParam.ProductType.MobileWallet.value,
            ),
        )
    }

    @Test
    fun `GIVEN the consent is confirmed WHEN a wallet is created THEN the tracking context is set first`() =
        runTest {
            // Arrange
            stubSignIn(
                consentAction = ConsentAction.Confirm(isEmailSharingAllowed = true),
                result = SocialLoginOutcome.WalletCreated(WALLET_ID).right(),
            )

            // Act
            handler.signIn(PROVIDER, SOURCE, host)

            // Assert
            verifyOrder {
                trackingContextProxy.setContext(userWallet)
                analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.MarketingEmailsConsent>())
            }
        }

    @Test
    fun `GIVEN the consent is confirmed WHEN the prompt returns THEN the marketing consent is not reported yet`() =
        runTest {
            // Arrange
            val requestConsent = slot<suspend () -> SocialLoginConsent?>()
            coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), capture(requestConsent)) } coAnswers {
                host.onConsentShown = { consent ->
                    consent.onAcknowledgedChange(true)
                    host.consent!!.onConfirmClick(CONSENT_TEXT)
                }
                requestConsent.captured()
                verify(exactly = 0) {
                    analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.MarketingEmailsConsent>())
                }
                verify(exactly = 0) { trackingContextProxy.setContext(any<UserWallet>()) }
                SocialLoginOutcome.WalletCreated(WALLET_ID).right()
            }

            // Act
            handler.signIn(PROVIDER, SOURCE, host)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.MarketingEmailsConsent>())
            }
        }

    @ParameterizedTest
    @MethodSource("provideConsentNotReportedModels")
    fun `GIVEN no confirmed consent or no selected new wallet WHEN signIn THEN the consent is not reported`(
        model: ConsentNotReportedModel,
    ) = runTest {
        // Arrange
        stubSignIn(consentAction = model.consentAction, result = model.result)
        if (model.isSelectFailing) {
            coEvery { userWalletsListRepository.select(WALLET_ID) } returns SELECT_ERROR.left()
        }

        // Act
        handler.signIn(PROVIDER, SOURCE, host)

        // Assert
        verify(exactly = 0) {
            analyticsEventHandler.send(ofType<SocialLoginAnalyticsEvents.MarketingEmailsConsent>())
        }
        verify(exactly = 0) { trackingContextProxy.setContext(any<UserWallet>()) }
    }

    private fun provideConsentNotReportedModels() = listOf(
        ConsentNotReportedModel(
            name = "sign-in failed after the consent",
            consentAction = ConsentAction.Confirm(isEmailSharingAllowed = true),
            result = SocialLoginError.NetworkError.left(),
        ),
        ConsentNotReportedModel(
            name = "consent closed",
            consentAction = ConsentAction.Close,
            result = SocialLoginError.ConsentDeclined.left(),
        ),
        ConsentNotReportedModel(
            name = "wallet restored",
            consentAction = ConsentAction.None,
            result = SocialLoginOutcome.WalletRestored(WALLET_ID).right(),
        ),
        ConsentNotReportedModel(
            name = "selection failed",
            consentAction = ConsentAction.Confirm(isEmailSharingAllowed = true),
            result = SocialLoginOutcome.WalletCreated(WALLET_ID).right(),
            isSelectFailing = true,
        ),
    )

    data class ConsentNotReportedModel(
        val name: String,
        val consentAction: ConsentAction,
        val result: Either<SocialLoginError, SocialLoginOutcome>,
        val isSelectFailing: Boolean = false,
    ) {
        override fun toString(): String = name
    }

    sealed interface ConsentAction {
        data class Confirm(val isEmailSharingAllowed: Boolean) : ConsentAction
        data object Close : ConsentAction
        data object None : ConsentAction
    }

    private fun stubSignIn(consentAction: ConsentAction, result: Either<SocialLoginError, SocialLoginOutcome>) {
        val requestConsent = slot<suspend () -> SocialLoginConsent?>()
        coEvery { socialLoginUseCase.signIn(PROVIDER, any(), any(), capture(requestConsent)) } coAnswers {
            host.onConsentShown = { consent ->
                when (consentAction) {
                    is ConsentAction.Confirm -> {
                        consent.onAcknowledgedChange(true)
                        host.consent!!.onEmailSharingChange(consentAction.isEmailSharingAllowed)
                        host.consent!!.onConfirmClick(CONSENT_TEXT)
                    }
                    ConsentAction.Close -> consent.onCloseClick()
                    ConsentAction.None -> Unit
                }
            }
            if (consentAction != ConsentAction.None) requestConsent.captured()
            result
        }
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
        const val CONSENT_TEXT = "Teilen Sie meine E-Mail mit Tangem"
        val SOURCE = AnalyticsParam.ScreensSources.ImportWallet
        const val STAGE_MIN_DISPLAY_MS = 3_000L
    }
}