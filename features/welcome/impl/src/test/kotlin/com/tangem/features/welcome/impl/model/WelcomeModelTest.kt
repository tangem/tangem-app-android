package com.tangem.features.welcome.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.SignIn
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GetCompletedBackupsUseCase
import com.tangem.domain.wallets.usecase.IsWalletBackedUpUseCase
import com.tangem.domain.wallets.usecase.NonBiometricUnlockWalletUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WelcomeModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase = mockk(relaxed = true)
    private val canUseBiometryUseCase: CanUseBiometryUseCase = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk(relaxed = true)
    private val saveWalletUseCase: SaveWalletUseCase = mockk(relaxed = true)
    private val walletsRepository: WalletsRepository = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val isWalletBackedUpUseCase: IsWalletBackedUpUseCase = mockk(relaxed = true)
    private val getCompletedBackupsUseCase: GetCompletedBackupsUseCase = mockk(relaxed = true)
    private val userWalletsFetcherFactory: UserWalletsFetcher.Factory = mockk()
    private val userWalletsFetcher: UserWalletsFetcher = mockk()
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val scanCardProcessor: ScanCardProcessor = mockk()
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles = mockk(relaxed = true)

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            userWalletsListRepository,
            nonBiometricUnlockWalletUseCase,
            canUseBiometryUseCase,
            coldUserWalletBuilderFactory,
            saveWalletUseCase,
            walletsRepository,
            trackingContextProxy,
            analyticsEventHandler,
            isWalletBackedUpUseCase,
            getCompletedBackupsUseCase,
            userWalletsFetcherFactory,
            userWalletsFetcher,
            hotWalletFeatureToggles,
            scanCardProcessor,
            onboardingV2FeatureToggles,
        )
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        every { userWalletsFetcher.userWallets } returns MutableStateFlow(persistentListOf(walletItem))
        every { userWalletsFetcherFactory.create(any(), any(), any(), any(), any()) } returns userWalletsFetcher
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(coldWallet)
        coEvery { canUseBiometryUseCase.strict() } returns false
        coEvery { scanCardProcessor.scan(any()) } just runs
    }

    @Test
    fun `GIVEN multiple mobile wallets enabled WHEN add wallet clicked THEN the add wallet sheet is requested`() =
        runTest {
            // Arrange
            setMultipleHotWalletsEnabled(isEnabled = true)
            val model = createModel()
            val sheetRequests = getEmittedValues(model.addWalletSheetRequests)
            sentEvents.clear()

            // Act
            model.selectWalletState().addWalletClick()
            advanceUntilIdle()

            // Assert
            assertThat(sheetRequests).containsExactly(Unit)
            assertThat(sentEvents).containsExactly(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.SignIn))
            coVerify(exactly = 0) { scanCardProcessor.scan(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN multiple mobile wallets disabled WHEN add wallet clicked THEN the card is scanned right away`() =
        runTest {
            // Arrange
            setMultipleHotWalletsEnabled(isEnabled = false)
            val model = createModel()
            val sheetRequests = getEmittedValues(model.addWalletSheetRequests)
            sentEvents.clear()

            // Act
            model.selectWalletState().addWalletClick()
            advanceUntilIdle()

            // Assert
            assertThat(sheetRequests).isEmpty()
            assertThat(sentEvents).containsExactly(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.SignIn))
            assertThat(capturedScanConfig().analyticsSource).isEqualTo(AnalyticsParam.ScreensSources.SignIn)
            model.onDestroy()
        }

    @Test
    fun `GIVEN the add wallet sheet WHEN its card row reports the tap THEN the card is scanned without a new event`() =
        runTest {
            // Arrange
            setMultipleHotWalletsEnabled(isEnabled = true)
            val model = createModel()
            sentEvents.clear()

            // Act
            model.addWalletModelCallbacks.onAddCardClick()
            advanceUntilIdle()

            // Assert
            assertThat(capturedScanConfig().analyticsSource).isEqualTo(AnalyticsParam.ScreensSources.SignIn)
            assertThat(sentEvents).isEmpty()
            model.onDestroy()
        }

    private fun setMultipleHotWalletsEnabled(isEnabled: Boolean) {
        every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns isEnabled
    }

    private fun capturedScanConfig(): ScanConfig {
        val config = slot<ScanConfig>()
        coVerify(exactly = 1) { scanCardProcessor.scan(capture(config)) }
        return config.captured
    }

    private fun WelcomeModel.selectWalletState(): WelcomeUM.SelectWallet =
        uiState.value as? WelcomeUM.SelectWallet ?: error("the screen is not on the wallet selection")

    private fun TestScope.createModel(): WelcomeModel {
        val model = WelcomeModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            uiMessageSender = uiMessageSender,
            userWalletsListRepository = userWalletsListRepository,
            nonBiometricUnlockWalletUseCase = nonBiometricUnlockWalletUseCase,
            canUseBiometryUseCase = canUseBiometryUseCase,
            coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
            saveWalletUseCase = saveWalletUseCase,
            walletsRepository = walletsRepository,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            isWalletBackedUpUseCase = isWalletBackedUpUseCase,
            getCompletedBackupsUseCase = getCompletedBackupsUseCase,
            userWalletsFetcherFactory = userWalletsFetcherFactory,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
            scanCardProcessor = scanCardProcessor,
            messageSender = uiMessageSender,
            onboardingV2FeatureToggles = onboardingV2FeatureToggles,
        )
        advanceUntilIdle()
        return model
    }

    private companion object {
        val walletItem: UserWalletItemUM = mockk()
        val coldWallet: UserWallet = mockk<UserWallet.Cold>()
    }
}