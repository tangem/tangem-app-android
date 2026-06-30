package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldSupplyStopEarningFeeContentTransformerTest {

    private val token = createToken()
    private val appCurrency = AppCurrency.Default

    @Test
    fun `GIVEN currency status loading WHEN transform THEN fee Loading and button flag preserved`() {
        // Arrange — prevState button flag is false; the Loading branch must not flip it
        val transformer = createTransformer(currencyStatus = loadingStatus(), feeFiatRate = BigDecimal("1"))

        // Act
        val result = transformer.transform(prevState())

        // Assert
        assertThat(result.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN loaded status with fee rate WHEN transform THEN only fiat fee set and the rest EMPTY`() {
        // Arrange — feeValue 0.5, feeFiatRate 2 → fiat fee = 1.0; all other fee fields are intentionally EMPTY
        val transformer = createTransformer(currencyStatus = customStatus(BigDecimal("1")), feeFiatRate = BigDecimal("2"))

        // Act
        val result = transformer.transform(prevState())

        // Assert
        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.yieldSupplyFeeUM).isEqualTo(
            YieldSupplyFeeUM.Content(
                transactionDataList = persistentListOf(),
                feeFiatValue = stringReference(fiatText(BigDecimal("0.5").multiply(BigDecimal("2")))),
                estimatedFiatValue = TextReference.EMPTY,
                maxNetworkFeeFiatValue = TextReference.EMPTY,
                minTopUpFiatValue = TextReference.EMPTY,
                feeNoteValue = TextReference.EMPTY,
            ),
        )
    }

    @Test
    fun `GIVEN loaded status but missing fee rate WHEN transform THEN fiat fee is the placeholder`() {
        // Arrange — negative: fee fiat rate unavailable, fiat fee text becomes the placeholder
        val transformer = createTransformer(currencyStatus = customStatus(BigDecimal("1")), feeFiatRate = null)

        // Act
        val result = transformer.transform(prevState())

        // Assert
        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.yieldSupplyFeeUM).isEqualTo(
            YieldSupplyFeeUM.Content(
                transactionDataList = persistentListOf(),
                feeFiatValue = stringReference(fiatText(null)),
                estimatedFiatValue = TextReference.EMPTY,
                maxNetworkFeeFiatValue = TextReference.EMPTY,
                minTopUpFiatValue = TextReference.EMPTY,
                feeNoteValue = TextReference.EMPTY,
            ),
        )
    }

    private fun fiatText(value: BigDecimal?): String = value.format { fiat(appCurrency.code, appCurrency.symbol) }

    private fun createTransformer(
        currencyStatus: CryptoCurrencyStatus,
        feeFiatRate: BigDecimal?,
    ): YieldSupplyStopEarningFeeContentTransformer = YieldSupplyStopEarningFeeContentTransformer(
        cryptoCurrencyStatus = currencyStatus,
        feeCryptoCurrencyStatus = customStatus(feeFiatRate),
        appCurrency = appCurrency,
        transactions = emptyList(),
        feeValue = BigDecimal("0.5"),
    )

    private fun customStatus(fiatRate: BigDecimal?): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.ZERO,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "0x0000000000000000000000000000000000000000",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun loadingStatus(): CryptoCurrencyStatus =
        CryptoCurrencyStatus(currency = token, value = CryptoCurrencyStatus.Loading)

    private fun prevState(): YieldSupplyActionUM = YieldSupplyActionUM(
        title = stringReference(""),
        subtitle = stringReference(""),
        footer = stringReference(""),
        footerLink = stringReference(""),
        currencyIconState = mockk<CurrencyIconState>(relaxed = true),
        yieldSupplyFeeUM = YieldSupplyFeeUM.Error,
        isPrimaryButtonEnabled = false,
        isTransactionSending = false,
        isHoldToConfirmEnabled = false,
    )

    private fun createToken(): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }
}