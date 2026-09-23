package com.tangem.features.feed.components.v2.blocks.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.usecase.GetTopEarnTokensUseCase
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.feed.components.v2.blocks.FeedBlockModalCallbacks
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.ui.v2.state.EarnBlockUM
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class EarnBlockModelTest {

    private val router: Router = mockk(relaxed = true)
    private val callbacks: FeedBlockModalCallbacks = mockk(relaxed = true)
    private val paramsContainer: ParamsContainer = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase = mockk()
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase = mockk()
    private val getTopEarnTokensUseCase: GetTopEarnTokensUseCase = mockk()

    private val walletAId = UserWalletId("aa")
    private val lockedWalletId = UserWalletId("bb")

    private lateinit var wallets: MutableStateFlow<List<UserWallet>?>
    private lateinit var accountsByWallet: MutableStateFlow<LinkedHashMap<UserWalletId, AccountStatusList>>
    private lateinit var yieldSupplyApy: MutableStateFlow<Map<String, BigDecimal>>
    private lateinit var topEarnTokens: MutableStateFlow<EarnTopToken?>

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            callbacks,
            userWalletsListRepository,
            multiAccountStatusListSupplier,
            stakingAvailabilityListUseCase,
            yieldSupplyApyFlowUseCase,
            getTopEarnTokensUseCase,
        )
        wallets = MutableStateFlow(emptyList())
        accountsByWallet = MutableStateFlow(linkedMapOf())
        yieldSupplyApy = MutableStateFlow(emptyMap())
        topEarnTokens = MutableStateFlow(null)
        every { paramsContainer.require<FeedBlockModalCallbacks>() } returns callbacks
        every { userWalletsListRepository.userWallets } returns wallets
        every { multiAccountStatusListSupplier.invokeAsMap() } returns accountsByWallet
        every { yieldSupplyApyFlowUseCase() } returns yieldSupplyApy
        every { getTopEarnTokensUseCase() } returns topEarnTokens
        coEvery { stakingAvailabilityListUseCase.invokeSync(any(), any()) } returns emptyMap()
    }

    @Test
    fun `GIVEN nothing is earning yet WHEN the model is created THEN a tap opens the Earn tab`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onClick()

        // Assert
        assertThat(model.state.value.target).isEqualTo(EarnBlockUM.Target.EarnTab)
        verify(exactly = 1) { callbacks.onOpenTab(FeedTabId.Earn) }
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the catalogue offers a rate WHEN nothing is earning THEN the onboarding pitch names it`() = runTest {
        // Arrange — the catalogue reports a fraction, the card shows the percent it means
        topEarnTokens.value = listOf(earnToken(apy = "0.055"), earnToken(apy = "0.1264")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.pitch).isEqualTo(EarnBlockUM.Pitch.Onboarding(apy = "12.64%"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN a catalogue rate WHEN nothing is earning THEN it reaches the pitch unrescaled`() = runTest {
        // Arrange
        topEarnTokens.value = listOf(earnToken(apy = "0.08")).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.pitch).isEqualTo(EarnBlockUM.Pitch.Onboarding(apy = "8.00%"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN no rate is known yet WHEN the model is created THEN the card pitches without one`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.pitch).isEqualTo(EarnBlockUM.Pitch.Plain)
        model.onDestroy()
    }

    @Test
    fun `GIVEN the user already lends WHEN the block is tapped THEN For You opens instead of the tab`() = runTest {
        // Arrange
        val token = lendingToken()
        wallets.value = listOf(wallet(walletAId))
        accountsByWallet.value = linkedMapOf(walletAId to accountStatusList(lendingStatus(token)))
        yieldSupplyApy.value = mapOf(token.yieldSupplyKey() to BigDecimal("7"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onClick()

        // Assert
        assertThat(model.state.value.target).isEqualTo(EarnBlockUM.Target.ForYou)
        assertThat(model.state.value.pitch)
            .isEqualTo(EarnBlockUM.Pitch.Growth(apy = "7.00%", tokenSymbol = token.symbol))
        verify(exactly = 1) { router.push(route = FeedRoute.ForYou, onComplete = any()) }
        verify(exactly = 0) { callbacks.onOpenTab(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a locked wallet holds the only earning token WHEN the model is created THEN it is not counted`() = runTest {
        // Arrange
        val token = lendingToken()
        wallets.value = listOf(wallet(lockedWalletId, isLocked = true))
        accountsByWallet.value = linkedMapOf(lockedWalletId to accountStatusList(lendingStatus(token)))
        yieldSupplyApy.value = mapOf(token.yieldSupplyKey() to BigDecimal("7"))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.target).isEqualTo(EarnBlockUM.Target.EarnTab)
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): EarnBlockModel = EarnBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        paramsContainer = paramsContainer,
        router = router,
        userWalletsListRepository = userWalletsListRepository,
        multiAccountStatusListSupplier = multiAccountStatusListSupplier,
        stakingAvailabilityListUseCase = stakingAvailabilityListUseCase,
        yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
        getTopEarnTokensUseCase = getTopEarnTokensUseCase,
    )

    private fun wallet(id: UserWalletId, isLocked: Boolean = false): UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns id
        every { this@mockk.isLocked } returns isLocked
    }

    private fun accountStatusList(vararg statuses: CryptoCurrencyStatus): AccountStatusList = mockk {
        every { flattenPersonalCurrencies() } returns statuses.toList()
    }

    /** Real currencies: mocking one means mocking its inline-class members, which MockK cannot retransform. */
    private fun lendingToken(): CryptoCurrency.Token =
        MockCryptoCurrencyFactory().createToken(blockchain = Blockchain.Ethereum)

    private fun lendingStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus {
        val value: CryptoCurrencyStatus.Value = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { yieldSupplyStatus } returns YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            )
            every { fiatAmount } returns BigDecimal("100")
        }
        return CryptoCurrencyStatus(currency = token, value = value)
    }

    private fun earnToken(apy: String): EarnTokenWithCurrency = mockk {
        every { earnToken } returns mockk<EarnToken> { every { this@mockk.apy } returns apy }
    }
}