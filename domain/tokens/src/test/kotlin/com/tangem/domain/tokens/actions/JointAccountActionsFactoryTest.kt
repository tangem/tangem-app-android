package com.tangem.domain.tokens.actions

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A joint token belongs to a Safe: the set mirrors what a personal account would offer in the same context, swap,
 * sell and stake are held back with [ScenarioUnavailabilityReason.JointAccount], and send and buy keep their
 * natural availability.
 */
internal class JointAccountActionsFactoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val rampStateManager: RampStateManager = mockk()
    private val factory = JointAccountActionsFactory(walletManagersFacade, rampStateManager)

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val userWallet: UserWallet = MockUserWalletFactory.create()

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, rampStateManager)
        coEvery { rampStateManager.availableForSell(any(), any(), any()) } returns Unit.right()
        coEvery {
            rampStateManager.getSendUnavailabilityReason(any(), any())
        } returns ScenarioUnavailabilityReason.None
    }

    @Test
    fun `GIVEN everything a personal account offers WHEN create THEN swap, sell and stake are held back and buy is open`() =
        runTest {
            // Arrange
            val status = statusWithAddress(SAFE)

            // Act
            val actual = factory.create(
                userWallet = userWallet,
                cryptoCurrencyStatus = status,
                stakingAvailability = StakingAvailability.Available(option = mockk()),
                yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
            )

            // Assert
            assertThat(actual).containsExactly(
                ActionState.Analytics(ScenarioUnavailabilityReason.None),
                ActionState.CopyAddress(ScenarioUnavailabilityReason.None),
                ActionState.Receive(ScenarioUnavailabilityReason.None),
                ActionState.Stake(ScenarioUnavailabilityReason.JointAccount, option = null),
                ActionState.Send(ScenarioUnavailabilityReason.None),
                ActionState.Swap(ScenarioUnavailabilityReason.JointAccount, shouldShowBadge = false),
                ActionState.Buy(ScenarioUnavailabilityReason.None),
                ActionState.Sell(ScenarioUnavailabilityReason.JointAccount),
                ActionState.HideToken(ScenarioUnavailabilityReason.None),
            ).inOrder()
        }

    @Test
    fun `GIVEN swap natural reason present WHEN create THEN swap keeps the natural reason`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE, isCustom = true)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Swap>().single().unavailabilityReason)
            .isEqualTo(ScenarioUnavailabilityReason.CustomToken(currency.name))
    }

    @Test
    fun `GIVEN an empty balance WHEN create THEN send keeps the natural reason`() = runTest {
        // Arrange
        val naturalReason = ScenarioUnavailabilityReason.EmptyBalance(
            withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SEND,
        )
        coEvery { rampStateManager.getSendUnavailabilityReason(any(), any()) } returns naturalReason
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Send>().single().unavailabilityReason)
            .isEqualTo(naturalReason)
    }

    @Test
    fun `GIVEN staking unavailable for the currency WHEN create THEN there is no stake action at all`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Stake>()).isEmpty()
    }

    @Test
    fun `GIVEN staking temporarily unavailable WHEN create THEN stake keeps the natural reason`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.TemporaryUnavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Stake>().single().unavailabilityReason)
            .isEqualTo(ScenarioUnavailabilityReason.StakingUnavailable(currency.name))
    }

    @Test
    fun `GIVEN sell unavailable for the currency WHEN create THEN sell keeps the natural reason`() = runTest {
        // Arrange
        val naturalReason = ScenarioUnavailabilityReason.NotSupportedBySellService(currency.name)
        coEvery { rampStateManager.availableForSell(any(), any(), any()) } returns naturalReason.left()
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Sell>().single().unavailabilityReason)
            .isEqualTo(naturalReason)
    }

    @Test
    fun `GIVEN a joint token without an address WHEN create THEN neither receive nor copy is offered`() = runTest {
        // Arrange
        val status = statusWithAddress(address = null)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Receive>()).isEmpty()
        assertThat(actual.filterIsInstance<ActionState.CopyAddress>()).isEmpty()
        assertThat(actual.filterIsInstance<ActionState.Send>().single().unavailabilityReason)
            .isEqualTo(ScenarioUnavailabilityReason.None)
    }

    @Test
    fun `GIVEN yield supply available WHEN create THEN the yield action is held back rather than hidden`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Available(apy = APY),
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.YieldMode>().single())
            .isEqualTo(
                ActionState.YieldMode(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount, apy = APY),
            )
    }

    @Test
    fun `GIVEN yield supply unavailable WHEN create THEN there is no yield action at all`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.Unavailable,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.YieldMode>()).isEmpty()
    }

    private fun statusWithAddress(address: String?, isCustom: Boolean = false): CryptoCurrencyStatus {
        val networkAddress = address?.let {
            NetworkAddress.Single(NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Primary))
        }
        val value = mockk<CryptoCurrencyStatus.Value> { every { this@mockk.networkAddress } returns networkAddress }
        val statusCurrency = if (isCustom) {
            (currency as CryptoCurrency.Coin).copy(isCustom = true)
        } else {
            currency
        }

        return mockk {
            every { this@mockk.currency } returns statusCurrency
            every { this@mockk.value } returns value
        }
    }

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
        const val APY = "4.2%"
    }
}