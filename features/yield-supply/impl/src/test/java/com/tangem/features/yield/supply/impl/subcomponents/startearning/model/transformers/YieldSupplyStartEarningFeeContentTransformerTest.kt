package com.tangem.features.yield.supply.impl.subcomponents.startearning.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldSupplyStartEarningFeeContentTransformerTest {

    private val token = createToken()
    private val appCurrency = AppCurrency.Default

    @Test
    fun `GIVEN currency status loading WHEN transform THEN fee Loading and button flag preserved`() {
        // Arrange — prevState button flag is false; the Loading branch must not flip it
        val transformer = createTransformer(currencyStatus = loadingStatus())

        // Act
        val result = transformer.transform(prevState())

        // Assert
        assertThat(result.yieldSupplyFeeUM).isEqualTo(YieldSupplyFeeUM.Loading)
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN loaded status with rates WHEN transform THEN fee Content with every fiat field computed`() {
        // Arrange — tokenFiatRate 1, feeFiatRate 2; feeValue 0.5, estimatedToken 0.4, minAmount 3, maxFee 2 token / 4 fiat
        val transformer = createTransformer(currencyStatus = customStatus(BigDecimal("1")), feeFiatRate = BigDecimal("2"))

        // Act
        val result = transformer.transform(prevState())

        // Assert — whole Content compared field-by-field (no fields touched on isPrimaryButtonEnabled)
        assertThat(result.yieldSupplyFeeUM).isEqualTo(
            expectedContent(tokenFiatRate = BigDecimal("1"), feeFiatRate = BigDecimal("2")),
        )
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN loaded status but missing rates WHEN transform THEN fiat fields collapse to placeholders`() {
        // Arrange — negative: both token and fee fiat rates unavailable
        val transformer = createTransformer(currencyStatus = customStatus(null), feeFiatRate = null)

        // Act
        val result = transformer.transform(prevState())

        // Assert — fiat-derived fields become the placeholder; crypto fields and the max fiat fee stay populated
        assertThat(result.yieldSupplyFeeUM).isEqualTo(
            expectedContent(tokenFiatRate = null, feeFiatRate = null),
        )
    }

    private fun expectedContent(tokenFiatRate: BigDecimal?, feeFiatRate: BigDecimal?): YieldSupplyFeeUM.Content {
        val feeFiatText = fiatText(feeFiatRate?.let(FEE_VALUE::multiply))
        val estimatedFiatText = fiatText(tokenFiatRate?.let(ESTIMATED_TOKEN::multiply))
        val estimatedCryptoText = cryptoText(ESTIMATED_TOKEN)
        val maxFiatText = fiatText(MAX_FIAT_FEE)
        val maxCryptoText = cryptoText(MAX_TOKEN_FEE)
        val minFiatText = fiatText(tokenFiatRate?.let(MIN_AMOUNT::multiply))
        val minCryptoText = cryptoText(MIN_AMOUNT)
        return YieldSupplyFeeUM.Content(
            transactionDataList = persistentListOf(),
            feeFiatValue = stringReference(feeFiatText),
            estimatedFiatValue = stringReference(estimatedFiatText),
            maxNetworkFeeFiatValue = stringReference(maxFiatText),
            minTopUpFiatValue = stringReference(minFiatText),
            feeNoteValue = resourceReference(
                id = R.string.yield_module_fee_policy_sheet_fee_note,
                formatArgs = wrappedList(estimatedFiatText, estimatedCryptoText, maxFiatText, maxCryptoText),
            ),
            minFeeNoteValue = resourceReference(
                id = R.string.yield_module_fee_policy_sheet_min_amount_note,
                formatArgs = wrappedList(minFiatText, minCryptoText),
            ),
        )
    }

    private fun cryptoText(value: BigDecimal): String = value.format { crypto(token) }

    private fun fiatText(value: BigDecimal?): String = value.format { fiat(appCurrency.code, appCurrency.symbol) }

    private fun createTransformer(
        currencyStatus: CryptoCurrencyStatus,
        feeFiatRate: BigDecimal? = BigDecimal("1"),
    ): YieldSupplyStartEarningFeeContentTransformer = YieldSupplyStartEarningFeeContentTransformer(
        cryptoCurrencyStatus = currencyStatus,
        feeCryptoCurrencyStatus = customStatus(feeFiatRate),
        appCurrency = appCurrency,
        updatedTransactionList = emptyList(),
        feeValue = FEE_VALUE,
        estimatedFeeValueInTokenCurrency = ESTIMATED_TOKEN,
        maxNetworkFee = YieldSupplyMaxFee(
            nativeMaxFee = BigDecimal("0.01"),
            tokenMaxFee = MAX_TOKEN_FEE,
            fiatMaxFee = MAX_FIAT_FEE,
        ),
        minAmount = MIN_AMOUNT,
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

    private companion object {
        val FEE_VALUE: BigDecimal = BigDecimal("0.5")
        val ESTIMATED_TOKEN: BigDecimal = BigDecimal("0.4")
        val MIN_AMOUNT: BigDecimal = BigDecimal("3")
        val MAX_TOKEN_FEE: BigDecimal = BigDecimal("2")
        val MAX_FIAT_FEE: BigDecimal = BigDecimal("4")
    }
}