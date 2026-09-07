package com.tangem.domain.tokens.actions

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** A joint token belongs to a Safe: the wallet's own keys spend nothing from it, so only receiving stays available. */
internal class JointAccountActionsFactoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val rampStateManager: RampStateManager = mockk()
    private val factory = JointAccountActionsFactory(walletManagersFacade, rampStateManager)

    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val userWallet: UserWallet = MockUserWalletFactory.create()

    @Test
    fun `GIVEN a joint token with an address WHEN create THEN receive copy analytics and hide are active`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual).containsExactly(
            ActionState.Analytics(ScenarioUnavailabilityReason.None),
            ActionState.CopyAddress(ScenarioUnavailabilityReason.None),
            ActionState.Receive(ScenarioUnavailabilityReason.None),
            ActionState.HideToken(ScenarioUnavailabilityReason.None),
            ActionState.Send(ScenarioUnavailabilityReason.JointAccount),
            ActionState.Swap(ScenarioUnavailabilityReason.JointAccount, shouldShowBadge = false),
            ActionState.Buy(ScenarioUnavailabilityReason.JointAccount),
            ActionState.Sell(ScenarioUnavailabilityReason.JointAccount),
            ActionState.Stake(ScenarioUnavailabilityReason.JointAccount, option = null),
        )
        verify {
            walletManagersFacade wasNot Called
            rampStateManager wasNot Called
        }
    }

    @Test
    fun `GIVEN a joint token without an address WHEN create THEN neither receive nor copy is offered`() = runTest {
        // Arrange
        val status = statusWithAddress(address = null)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.Receive>()).isEmpty()
        assertThat(actual.filterIsInstance<ActionState.CopyAddress>()).isEmpty()
        assertThat(actual.filterIsInstance<ActionState.Send>().single().unavailabilityReason)
            .isEqualTo(ScenarioUnavailabilityReason.JointAccount)
    }

    @Test
    fun `GIVEN yield supply available WHEN create THEN the yield button is disabled rather than hidden`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            yieldSupplyAvailability = YieldSupplyAvailability.Available(apy = APY),
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.YieldMode>().single())
            .isEqualTo(ActionState.YieldMode(unavailabilityReason = ScenarioUnavailabilityReason.JointAccount, apy = APY))
    }

    @Test
    fun `GIVEN yield supply unavailable WHEN create THEN there is no yield button at all`() = runTest {
        // Arrange
        val status = statusWithAddress(SAFE)

        // Act
        val actual = factory.create(
            userWallet = userWallet,
            cryptoCurrencyStatus = status,
            yieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
        )

        // Assert
        assertThat(actual.filterIsInstance<ActionState.YieldMode>()).isEmpty()
    }

    private fun statusWithAddress(address: String?): CryptoCurrencyStatus {
        val networkAddress = address?.let {
            NetworkAddress.Single(NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Primary))
        }
        val value = mockk<CryptoCurrencyStatus.Value> { every { this@mockk.networkAddress } returns networkAddress }

        return mockk {
            every { this@mockk.currency } returns this@JointAccountActionsFactoryTest.currency
            every { this@mockk.value } returns value
        }
    }

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
        const val APY = "4.2%"
    }
}