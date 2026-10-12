package com.tangem.features.introduction.impl.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IntroductionCardScannerTest {

    private val scanCardProcessor: ScanCardProcessor = mockk(relaxed = true)
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val walletBuilder: ColdUserWalletBuilder = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase = mockk()

    private val scanResponse: ScanResponse = mockk(relaxed = true)
    private val userWallet: UserWallet.Cold = mockk(relaxed = true)
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val scanner = IntroductionCardScanner(
        scanCardProcessor = scanCardProcessor,
        cardSdkConfigRepository = cardSdkConfigRepository,
        settingsRepository = settingsRepository,
        coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
        saveWalletUseCase = saveWalletUseCase,
        shouldAskPermissionUseCase = shouldAskPermissionUseCase,
        userWalletsListRepository = userWalletsListRepository,
        analyticsEventHandler = analyticsEventHandler,
        router = router,
        messageSender = messageSender,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            scanCardProcessor,
            cardSdkConfigRepository,
            settingsRepository,
            walletBuilder,
            coldUserWalletBuilderFactory,
            saveWalletUseCase,
            shouldAskPermissionUseCase,
            userWalletsListRepository,
            analyticsEventHandler,
            router,
            messageSender,
        )
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        every { coldUserWalletBuilderFactory.create(scanResponse = any()) } returns walletBuilder
        coEvery { walletBuilder.build() } returns userWallet
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        // The processor is the sdk boundary; deliver a read card straight to the success callback.
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers { arg<suspend (ScanResponse) -> Unit>(n = 7).invoke(scanResponse) }
        coEvery { shouldAskPermissionUseCase(any()) } returns false
    }

    @Test
    fun `GIVEN the wallet saves WHEN a card is scanned THEN the sign-in is reported and the wallet opens`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(any(), any(), any()) } returns Unit.right()

        var isLeavingReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { isLeavingReported = true },
            onFailed = { error("must not be called") },
        )

        // Assert
        assertThat(sentEvents.filterIsInstance<Basic.SignedIn>()).hasSize(1)
        assertThat(isLeavingReported).isTrue()
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
    }

    @Test
    fun `GIVEN push permission not asked yet WHEN a card is scanned THEN the wallet opens behind the push screen`() =
        runTest {
            // Arrange
            coEvery { saveWalletUseCase(any(), any(), any()) } returns Unit.right()
            coEvery { shouldAskPermissionUseCase(any()) } returns true

            // Act
            scanner.scanAndOpenWallet(
                analyticsSource = SOURCE,
                onNavigatedAway = {},
                onFailed = { error("must not be called") },
            )

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    AppRoute.PushNotification(
                        source = AppRoute.PushNotification.Source.FirstSignIn,
                        nextRoute = AppRoute.Wallet,
                    ),
                )
            }
        }

    @Test
    fun `GIVEN the wallet is already saved AND push not asked yet WHEN a card is scanned THEN no push screen`() =
        runTest {
            // Arrange
            coEvery { saveWalletUseCase(any(), any(), any()) } returns
                SaveWalletError.WalletAlreadySaved(messageId = 0).left()
            coEvery { userWalletsListRepository.unlock(any(), any()) } returns Unit.right()
            coEvery { shouldAskPermissionUseCase(any()) } returns true

            // Act
            scanner.scanAndOpenWallet(
                analyticsSource = SOURCE,
                onNavigatedAway = {},
                onFailed = { error("must not be called") },
            )

            // Assert
            verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        }

    @Test
    fun `GIVEN the wallet is already saved WHEN a card is scanned THEN it is unlocked by the scan and opened`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(any(), any(), any()) } returns
            SaveWalletError.WalletAlreadySaved(messageId = 0).left()
        coEvery { userWalletsListRepository.unlock(any(), any()) } returns Unit.right()
        var isLeavingReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { isLeavingReported = true },
            onFailed = { error("must not be called") },
        )

        // Assert
        coVerify(exactly = 1) {
            userWalletsListRepository.unlock(
                userWalletId = userWallet.walletId,
                unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(scanResponse = scanResponse, source = SOURCE),
            )
        }
        assertThat(sentEvents).isEmpty()
        assertThat(isLeavingReported).isTrue()
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
    }

    @Test
    fun `GIVEN the viewer cancels the scan WHEN the processor reports it THEN the cancellation is reported`() = runTest {
        // Arrange
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers { arg<suspend (TangemError) -> Unit>(n = 6).invoke(TangemSdkError.UserCancelled()) }
        var isFailureReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { error("must not be called") },
            onFailed = { isFailureReported = true },
        )

        // Assert
        assertThat(sentEvents).containsExactly(IntroductionProcess.ScanCancelled(SOURCE))
        assertThat(isFailureReported).isTrue()
        verify { router wasNot Called }
    }

    @Test
    fun `GIVEN the scan fails for another reason WHEN the processor reports it THEN no cancellation is reported`() = runTest {
        // Arrange
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers { arg<suspend (TangemError) -> Unit>(n = 6).invoke(TangemSdkError.NfcFeatureIsUnavailable()) }
        var isFailureReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { error("must not be called") },
            onFailed = { isFailureReported = true },
        )

        // Assert
        assertThat(sentEvents).isEmpty()
        assertThat(isFailureReported).isTrue()
    }

    @Test
    fun `GIVEN saving fails WHEN a card is scanned THEN the error is shown and the caller is told`() = runTest {
        // Arrange
        coEvery { saveWalletUseCase(any(), any(), any()) } returns
            SaveWalletError.DataError(messageId = null).left()
        var isFailureReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { error("must not be called") },
            onFailed = { isFailureReported = true },
        )

        // Assert
        assertThat(isFailureReported).isTrue()
        verify(exactly = 1) { messageSender.send(any<SnackbarMessage>()) }
        assertThat(sentEvents.filterIsInstance<Basic.SignedIn>()).isEmpty()
        verify { router wasNot Called }
    }

    @Test
    fun `GIVEN the user cancels the scan WHEN scanning THEN the caller is told it failed`() = runTest {
        // Arrange
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = any(),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } coAnswers { arg<suspend () -> Unit>(n = 5).invoke() }
        var isFailureReported = false

        // Act
        scanner.scanAndOpenWallet(
            analyticsSource = SOURCE,
            onNavigatedAway = { error("must not be called") },
            onFailed = { isFailureReported = true },
        )

        // Assert
        assertThat(isFailureReported).isTrue()
        verify { router wasNot Called }
    }

    @Test
    fun `GIVEN a card that needs activation WHEN scanned THEN the caller is told the flow leaves the screen`() =
        runTest {
            // Arrange
            coEvery {
                scanCardProcessor.scan(
                    analyticsSource = any(),
                    shouldCheckIsAlreadyActivated = any(),
                    cardId = any(),
                    onProgressStateChange = any(),
                    onWalletNotCreated = any(),
                    onCancel = any(),
                    onFailure = any(),
                    onSuccess = any(),
                )
            } coAnswers { arg<suspend () -> Unit>(n = 4).invoke() }
            var isLeavingReported = false

            // Act
            scanner.scanAndOpenWallet(
                analyticsSource = SOURCE,
                onNavigatedAway = { isLeavingReported = true },
                onFailed = { error("must not be called") },
            )

            // Assert
            assertThat(isLeavingReported).isTrue()
            verify { router wasNot Called }
        }

    private companion object {
        val SOURCE = AnalyticsParam.ScreensSources.Intro
    }
}