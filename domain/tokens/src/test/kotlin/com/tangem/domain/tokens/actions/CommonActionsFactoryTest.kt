package com.tangem.domain.tokens.actions

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CommonActionsFactoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val rampStateManager: RampStateManager = mockk(relaxed = true)

    private val factory = CommonActionsFactory(
        walletManagersFacade = walletManagersFacade,
        rampStateManager = rampStateManager,
    )

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val cardTypesResolver: CardTypesResolver = mockk()
    private val userWallet: UserWallet.Cold = mockk(relaxed = true)
    private val cryptoCurrencyStatus: CryptoCurrencyStatus = mockk()

    @BeforeAll
    fun setupStatic() {
        // cardTypesResolver is a UserWallet.Cold extension property, so it is stubbed via its file class.
        mockkStatic("com.tangem.domain.card.common.util.ScanResponseExtKt")
    }

    @AfterAll
    fun tearDownStatic() {
        unmockkStatic("com.tangem.domain.card.common.util.ScanResponseExtKt")
    }

    @BeforeEach
    fun setup() {
        clearMocks(walletManagersFacade, rampStateManager, cardTypesResolver, userWallet, cryptoCurrencyStatus)

        val value = mockk<CryptoCurrencyStatus.Value>(relaxed = true)
        every { cryptoCurrencyStatus.value } returns value
        every { cryptoCurrencyStatus.currency } returns currency

        every { userWallet.cardTypesResolver } returns cardTypesResolver
        every { userWallet.isMultiCurrency } returns false

        coEvery { rampStateManager.getSendUnavailabilityReason(any(), any()) } returns ScenarioUnavailabilityReason.None
        coEvery { rampStateManager.availableForSell(any(), any(), any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN Start2Coin cold wallet WHEN create THEN buy action is unavailable`() = runTest {
        // Arrange
        every { cardTypesResolver.isStart2Coin() } returns true

        // Act
        val buyAction = createBuyAction()

        // Assert
        assertThat(buyAction.unavailabilityReason)
            .isEqualTo(ScenarioUnavailabilityReason.BuyUnavailable(currency.name))
    }

    @Test
    fun `GIVEN non-Start2Coin cold wallet WHEN create THEN buy action is available`() = runTest {
        // Arrange
        every { cardTypesResolver.isStart2Coin() } returns false

        // Act
        val buyAction = createBuyAction()

        // Assert
        assertThat(buyAction.unavailabilityReason).isEqualTo(ScenarioUnavailabilityReason.None)
    }

    @Test
    fun `GIVEN region unavailable WHEN create THEN no stake action offered`() = runTest {
        // Arrange
        every { cardTypesResolver.isStart2Coin() } returns false

        // Act
        val actions = createActions(stakingAvailability = StakingAvailability.RegionUnavailable)

        // Assert
        assertThat(actions.filterIsInstance<ActionState.Stake>()).isEmpty()
    }

    private suspend fun createBuyAction(): ActionState.Buy {
        val actions = createActions(stakingAvailability = StakingAvailability.Unavailable)
        return actions.filterIsInstance<ActionState.Buy>().single()
    }

    private suspend fun createActions(stakingAvailability: StakingAvailability): Set<ActionState> {
        return factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            stakingAvailability = stakingAvailability,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
            shouldShowSwapStories = false,
        )
    }
}