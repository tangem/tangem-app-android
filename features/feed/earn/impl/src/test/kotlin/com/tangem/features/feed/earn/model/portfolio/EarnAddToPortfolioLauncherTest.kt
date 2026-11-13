package com.tangem.features.feed.earn.model.portfolio

import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetAvailabilityUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorBridge
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.feed.earn.createEarnToken
import com.tangem.features.feed.earn.createEarnTokenWithCurrency
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnAddToPortfolioLauncherTest {

    private val earnFeatureToggles: EarnFeatureToggles = mockk()
    private val addToPortfolioManager: AddToPortfolioManager = mockk(relaxed = true)
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk()
    private val portfolioSelectorBridge: PortfolioSelectorBridge = mockk()
    private val portfolioSelectorBridgeFactory: PortfolioSelectorBridge.Factory = mockk()
    private val router: Router = mockk(relaxed = true)
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase = mockk()
    private val yieldSupplyGetAvailabilityUseCase: YieldSupplyGetAvailabilityUseCase = mockk()

    private val onSuccessAdded = Channel<AddToPortfolioManager.Result>(Channel.UNLIMITED)
    private val onAddedTokenClick = Channel<AddToPortfolioManager.Result>(Channel.UNLIMITED)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            earnFeatureToggles,
            addToPortfolioManager,
            addToPortfolioManagerFactory,
            portfolioSelectorBridgeFactory,
            router,
            getStakingAvailabilityUseCase,
            yieldSupplyGetAvailabilityUseCase,
        )
        every { addToPortfolioManager.onDismiss } returns Channel()
        every { addToPortfolioManager.onSuccessAdded } returns onSuccessAdded
        every { addToPortfolioManager.onAddedTokenClick } returns onAddedTokenClick
        every { addToPortfolioManagerFactory.create(any(), any(), any(), any()) } returns addToPortfolioManager
        every { portfolioSelectorBridgeFactory.create(any(), any(), any()) } returns portfolioSelectorBridge
    }

    private fun createLauncher(modelScope: CoroutineScope) = EarnAddToPortfolioLauncher(
        addToPortfolioManagerFactory = addToPortfolioManagerFactory,
        portfolioSelectorBridgeFactory = portfolioSelectorBridgeFactory,
        earnFeatureToggles = earnFeatureToggles,
        getStakingAvailabilityUseCase = getStakingAvailabilityUseCase,
        yieldSupplyGetAvailabilityUseCase = yieldSupplyGetAvailabilityUseCase,
        analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        router = router,
        modelScope = modelScope,
        showSheet = { _, _ -> },
        dismissSheet = {},
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LaunchMode {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the user portfolio toggle WHEN an opportunity is launched THEN the sheet opens in the matching mode`(
            model: LaunchModeModel,
        ) = runTest {
            // Arrange
            every { earnFeatureToggles.isUserPortfolioEnabled } returns model.isUserPortfolioEnabled
            val launcher = createLauncher(modelScope = backgroundScope)

            // Act
            launcher.launch(createEarnTokenWithCurrency(), EarnSource.BEST_OPPORTUNITIES_SOURCE)

            // Assert
            verify(exactly = 1) { addToPortfolioManager.updateLaunchMode(model.expected) }
        }

        private fun provideTestModels() = listOf(
            LaunchModeModel(
                isUserPortfolioEnabled = true,
                expected = AddToPortfolioManager.LaunchMode.ViaUserPortfolio(shouldScopeToNetworks = true),
            ),
            LaunchModeModel(isUserPortfolioEnabled = false, expected = AddToPortfolioManager.LaunchMode.Preselected),
        )
    }

    @Nested
    inner class ManagerSettings {

        @Test
        fun `GIVEN an opportunity WHEN it is launched THEN the sheet loads multi-currency wallets without joint accounts`() = runTest {
            // Arrange
            every { earnFeatureToggles.isUserPortfolioEnabled } returns true
            val launcher = createLauncher(modelScope = backgroundScope)

            // Act
            launcher.launch(createEarnTokenWithCurrency(), EarnSource.BEST_OPPORTUNITIES_SOURCE)

            // Assert
            verify(exactly = 1) {
                portfolioSelectorBridgeFactory.create(
                    mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
                    scope = any(),
                    isJointAccountsIncluded = false,
                )
            }
            verify(exactly = 1) {
                addToPortfolioManagerFactory.create(
                    scope = any(),
                    settings = AddToPortfolioManager.Settings(shouldSkipTokenActionsScreen = true),
                    analyticsParams = any(),
                    portfolioSelectorBridge = portfolioSelectorBridge,
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RouteAfterSheet {

        private val userWalletId = UserWalletId("0011")
        private val currencyFactory = MockCryptoCurrencyFactory()
        private val solana = currencyFactory.createCoin(Blockchain.Solana)
        private val solanaStaking: StakingOption = mockk {
            every { integrationId } returns StakingIntegrationID.StakeKit.Coin.Solana
        }
        private val usdt = currencyFactory.createToken(Blockchain.Ethereum, id = "tether", symbol = "USDT")

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an earn opportunity WHEN the sheet returns a currency THEN the matching earn screen opens`(
            model: RouteModel,
        ) = runTest {
            // Arrange
            every { earnFeatureToggles.isUserPortfolioEnabled } returns model.isUserPortfolioEnabled
            coEvery { getStakingAvailabilityUseCase.invokeSync(userWalletId, model.addedCurrency) } returns
                (model.stakingAvailability?.right() ?: StakingError.DomainError(message = null).left())
            coEvery { yieldSupplyGetAvailabilityUseCase(model.addedCurrency) } returns
                (model.yieldAvailability?.right() ?: IllegalStateException().left())
            val launcher = createLauncher(modelScope = backgroundScope)
            val earnToken = createEarnToken(type = model.earnType, apy = EARN_LIST_APY)
            launcher.launch(
                earnTokenWithCurrency = createEarnTokenWithCurrency(earnToken = earnToken),
                source = EarnSource.BEST_OPPORTUNITIES_SOURCE,
            )

            // Act
            model.trigger.channel().trySend(createResult(model.addedCurrency))
            runCurrent()

            // Assert
            verify(exactly = 1) { router.push(model.expected, any()) }
        }

        private fun Trigger.channel() = when (this) {
            Trigger.SUCCESS_ADDED -> onSuccessAdded
            Trigger.ADDED_TOKEN_CLICK -> onAddedTokenClick
        }

        private fun createResult(currency: CryptoCurrency): AddToPortfolioManager.Result {
            val wallet: UserWallet = mockk { every { walletId } returns userWalletId }
            val status: CryptoCurrencyStatus = mockk { every { this@mockk.currency } returns currency }
            return AddToPortfolioManager.Result(wallet = wallet, account = mockk(), addedCurrency = status)
        }

        private fun provideTestModels() = Trigger.entries.flatMap { trigger ->
            listOf(
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.STAKING,
                    addedCurrency = solana,
                    stakingAvailability = StakingAvailability.Available(solanaStaking),
                    expected = AppRoute.Staking(userWalletId, solana, StakingIntegrationID.StakeKit.Coin.Solana),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.STAKING,
                    addedCurrency = solana,
                    stakingAvailability = StakingAvailability.TemporaryUnavailable,
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = solana),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.STAKING,
                    addedCurrency = solana,
                    stakingAvailability = null,
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = solana),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.YIELD,
                    addedCurrency = usdt,
                    yieldAvailability = YieldSupplyAvailability.Available(apy = AVAILABLE_APY),
                    expected = AppRoute.YieldSupplyEntry(
                        userWalletId = userWalletId,
                        cryptoCurrency = usdt,
                        apy = AVAILABLE_APY,
                    ),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.YIELD,
                    addedCurrency = usdt,
                    yieldAvailability = YieldSupplyAvailability.Unavailable,
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = usdt),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.YIELD,
                    addedCurrency = usdt,
                    yieldAvailability = null,
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = usdt),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.STAKING,
                    addedCurrency = solana,
                    isUserPortfolioEnabled = false,
                    stakingAvailability = StakingAvailability.Available(solanaStaking),
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = solana),
                ),
                RouteModel(
                    trigger = trigger,
                    earnType = EarnType.YIELD,
                    addedCurrency = usdt,
                    isUserPortfolioEnabled = false,
                    yieldAvailability = YieldSupplyAvailability.Available(apy = AVAILABLE_APY),
                    expected = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = usdt),
                ),
            )
        }
    }

    data class LaunchModeModel(
        val isUserPortfolioEnabled: Boolean,
        val expected: AddToPortfolioManager.LaunchMode,
    )

    enum class Trigger { SUCCESS_ADDED, ADDED_TOKEN_CLICK }

    data class RouteModel(
        val trigger: Trigger,
        val earnType: EarnType,
        val addedCurrency: CryptoCurrency,
        val expected: AppRoute,
        val isUserPortfolioEnabled: Boolean = true,
        /** `null` makes the use case fail. */
        val stakingAvailability: StakingAvailability? = null,
        /** `null` makes the use case fail. */
        val yieldAvailability: YieldSupplyAvailability? = null,
    )

    private companion object {
        /** The earn list carries a fraction; the route must not pass it on. */
        const val EARN_LIST_APY = "0.055"
        const val AVAILABLE_APY = "5.5"
    }
}