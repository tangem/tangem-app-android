package com.tangem.domain.tokens.actions

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.test.core.ProvideTestModels
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * With an active yield position, `amount` includes the supplied part, so the actions that spend it (send, sell, swap)
 * also need that position to be ACTUAL, not only the network status.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OutdatedDataActionsFactoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val rampStateManager: RampStateManager = mockk(relaxed = true)

    private val factory = OutdatedDataActionsFactory(
        walletManagersFacade = walletManagersFacade,
        rampStateManager = rampStateManager,
    )

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val cardTypesResolver: CardTypesResolver = mockk()
    private val userWallet: UserWallet.Cold = mockk(relaxed = true)

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
        clearMocks(walletManagersFacade, rampStateManager, cardTypesResolver, userWallet)

        every { userWallet.cardTypesResolver } returns cardTypesResolver
        every { cardTypesResolver.isStart2Coin() } returns false

        coEvery { rampStateManager.getSendUnavailabilityReason(any(), any()) } returns ScenarioUnavailabilityReason.None
        coEvery { rampStateManager.availableForSell(any(), any(), any()) } returns Unit.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN actual network WHEN create THEN send, sell and swap follow an active yield position source`(
        model: SpendingActionsModel,
    ) = runTest {
        // Act
        val actions = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status(contributions = model.contributions),
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        val actual = SpendingReasons(
            send = actions.filterIsInstance<ActionState.Send>().single().unavailabilityReason,
            sell = actions.filterIsInstance<ActionState.Sell>().single().unavailabilityReason,
            swap = actions.filterIsInstance<ActionState.Swap>().single().unavailabilityReason,
        )
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SpendingActionsModel(
            name = "no yield position: network decides",
            contributions = emptyList(),
            expected = SpendingReasons(available, available, available),
        ),
        SpendingActionsModel(
            name = "active position ACTUAL",
            contributions = listOf(position(isActive = true, source = StatusSource.ACTUAL)),
            expected = SpendingReasons(available, available, available),
        ),
        SpendingActionsModel(
            name = "active position CACHE: outdated, swap still loading",
            contributions = listOf(position(isActive = true, source = StatusSource.CACHE)),
            expected = SpendingReasons(outdated, outdated, loading),
        ),
        SpendingActionsModel(
            name = "active position ONLY_CACHE: outdated",
            contributions = listOf(position(isActive = true, source = StatusSource.ONLY_CACHE)),
            expected = SpendingReasons(outdated, outdated, outdated),
        ),
        SpendingActionsModel(
            name = "inactive position CACHE: nothing folded, network decides",
            contributions = listOf(position(isActive = false, source = StatusSource.CACHE)),
            expected = SpendingReasons(available, available, available),
        ),
    )

    data class SpendingActionsModel(
        val name: String,
        val contributions: List<BalanceContribution>,
        val expected: SpendingReasons,
    ) {
        override fun toString(): String = name
    }

    data class SpendingReasons(
        val send: ScenarioUnavailabilityReason,
        val sell: ScenarioUnavailabilityReason,
        val swap: ScenarioUnavailabilityReason,
    )

    private fun position(isActive: Boolean, source: StatusSource) = YieldSupplyContribution(
        status = YieldSupplyStatus(
            isActive = isActive,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = if (isActive) BigDecimal.ONE else null,
        ),
        source = source,
    )

    private fun status(contributions: List<BalanceContribution>) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(contributionSources = contributions.map(BalanceContribution::source)),
            contributions = contributions,
        ),
    )

    private companion object {
        val available = ScenarioUnavailabilityReason.None
        val outdated = ScenarioUnavailabilityReason.UsedOutdatedData
        val loading = ScenarioUnavailabilityReason.DataLoading
    }
}