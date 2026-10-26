package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class WalletTokenRowConverterTest {

    private val userWalletId = UserWalletId("00")
    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val clickIntents: WalletContentClickIntents = mockk(relaxed = true)

    private val converter = createConverter()

    private fun createConverter(jointPendingOperations: List<JointPendingOperation> = emptyList()) =
        WalletTokenRowConverter(
            appCurrency = AppCurrency.Default,
            accountId = createMainAccount(userWalletId).accountId,
            clickIntents = clickIntents,
            yieldModuleApyMap = emptyMap(),
            stakingAvailabilityMap = emptyMap(),
            userWalletId = userWalletId,
            jointPendingOperations = jointPendingOperations,
        )

    @Test
    fun `GIVEN loading status WHEN convert THEN long click is disabled`() {
        // Act
        val actual = converter.convert(
            CryptoCurrencyStatus(currency = coin, value = CryptoCurrencyStatus.Loading),
            shouldShowPromo = false,
        )

        // Assert
        assertThat(actual.onLongClick).isNull()
    }

    @Test
    fun `GIVEN loaded status WHEN convert THEN long click is enabled`() {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = loaded()), shouldShowPromo = false)

        // Assert
        assertThat(actual.onLongClick).isNotNull()
    }

    @Test
    fun `GIVEN no amount status with pending joint operation WHEN convert THEN pending bubble is shown`() {
        // Arrange
        val operation: JointPendingOperation = mockk(relaxed = true) {
            every { transfer.currency } returns coin
        }
        val jointConverter = createConverter(jointPendingOperations = listOf(operation))
        val status = CryptoCurrencyStatus(
            currency = coin,
            value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
        )

        // Act
        val actual = jointConverter.convert(status, shouldShowPromo = true)

        // Assert
        assertThat((actual.state as TangemTokenRow.State.Content).messageBubble).isNotNull()
        assertThat(actual.onPromoShown).isNull()
    }

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.TEN,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
    )

    private val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
    )
}