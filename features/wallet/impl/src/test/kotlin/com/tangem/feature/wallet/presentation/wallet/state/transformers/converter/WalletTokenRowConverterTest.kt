package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account.Personal.Companion.createMainAccount
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletTokenRowConverterTest {

    private val userWalletId = UserWalletId("00")
    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val clickIntents: WalletContentClickIntents = mockk(relaxed = true)

    private val converter = WalletTokenRowConverter(
        appCurrency = AppCurrency.Default,
        accountId = createMainAccount(userWalletId).accountId,
        shouldShowPromo = false,
        clickIntents = clickIntents,
        yieldModuleApyMap = emptyMap(),
        stakingAvailabilityMap = emptyMap(),
        userWalletId = userWalletId,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN currency status WHEN convert THEN matching row variant`(model: VariantModel) {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = model.value))

        // Assert
        assertThat(actual.state).isInstanceOf(model.expected.java)
    }

    @Test
    fun `GIVEN loading status WHEN convert THEN long click is disabled`() {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = CryptoCurrencyStatus.Loading))

        // Assert
        assertThat(actual.onLongClick).isNull()
    }

    @Test
    fun `GIVEN loaded status WHEN convert THEN long click is enabled`() {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = loaded()))

        // Assert
        assertThat(actual.onLongClick).isNotNull()
    }

    private fun provideTestModels() = listOf(
        VariantModel(value = CryptoCurrencyStatus.Loading, expected = TangemTokenRow.State.Shimmer::class),
        VariantModel(value = loaded(), expected = TangemTokenRow.State.Content::class),
        VariantModel(
            value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
            expected = TangemTokenRow.State.Content::class,
        ),
        VariantModel(
            value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
            expected = TangemTokenRow.State.Unreachable::class,
        ),
        VariantModel(
            value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
            expected = TangemTokenRow.State.NoAddress::class,
        ),
    )

    private fun loaded() = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal.ONE,
        fiatAmount = BigDecimal.TEN,
        fiatRate = BigDecimal.TEN,
        priceChange = BigDecimal.ZERO,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    )

    data class VariantModel(
        val value: CryptoCurrencyStatus.Value,
        val expected: KClass<out TangemTokenRow.State>,
    )
}