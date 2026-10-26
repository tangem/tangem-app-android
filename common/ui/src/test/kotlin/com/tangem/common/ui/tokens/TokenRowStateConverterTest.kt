package com.tangem.common.ui.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenRowStateConverterTest {

    private val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
    private val converter = TokenRowStateConverter(appCurrency = AppCurrency.Default)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN currency status WHEN convert THEN matching row variant`(model: VariantModel) {
        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = model.value), onClick = {})

        // Assert
        assertThat(actual).isInstanceOf(model.expected.java)
    }

    @Test
    fun `GIVEN custom status without price change WHEN convert THEN price change is hidden`() {
        // Arrange
        val value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.ONE,
            fiatAmount = null,
            fiatRate = null,
            priceChange = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
        )

        // Act
        val actual = converter.convert(CryptoCurrencyStatus(currency = coin, value = value), onClick = {})

        // Assert
        assertThat((actual as TangemTokenRow.State.Content).priceChange).isNull()
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
        networkAddress = networkAddress,
        sources = CryptoCurrencyStatus.Sources(),
    )

    private val networkAddress = NetworkAddress.Single(
        defaultAddress = NetworkAddress.Address(value = "addr", type = NetworkAddress.Address.Type.Primary),
    )

    data class VariantModel(
        val value: CryptoCurrencyStatus.Value,
        val expected: KClass<out TangemTokenRow.State>,
    )
}