package com.tangem.features.yield.supply.impl.active.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
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
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.active.entity.YieldSupplyActiveContentUM
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldSupplyActiveFeeContentTransformerTest {

    private val analyticsHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val token = createToken()
    private val appCurrency = AppCurrency.Default

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsHandler)
    }

    @Test
    fun `GIVEN fee below max WHEN transform THEN not high fee and computed fee texts`() {
        // Arrange — fee 1, maxToken 2, maxFiat 4, fiatRate 1
        val transformer = createTransformer(feeValue = BigDecimal("1"), tokenMaxFee = BigDecimal("2"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — currentFee is the token fiat fee (feeValue * fiatRate); feeDescription holds the 4 args in order
        val expectedFiatFee = fiatText(BigDecimal("1").multiply(BigDecimal("1")))
        assertThat(result.isHighFee).isFalse()
        assertThat(result.currentFee).isEqualTo(stringReference(expectedFiatFee))
        assertThat(result.feeDescription).isEqualTo(
            resourceReference(
                id = R.string.yield_module_fee_policy_sheet_fee_note,
                formatArgs = wrappedList(
                    stringReference(expectedFiatFee),
                    stringReference(cryptoText(BigDecimal("1"))),
                    stringReference(fiatText(BigDecimal("4"))),
                    stringReference(cryptoText(BigDecimal("2"))),
                ),
            ),
        )
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN fee above max WHEN transform THEN high fee and analytics carries token and blockchain`() {
        // Arrange
        val transformer = createTransformer(feeValue = BigDecimal("3"), tokenMaxFee = BigDecimal("2"))
        val eventSlot = slot<AnalyticsEvent>()

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.isHighFee).isTrue()
        verify(exactly = 1) { analyticsHandler.send(capture(eventSlot)) }
        val event = eventSlot.captured as YieldSupplyAnalytics.NoticeHighNetworkFee
        assertThat(event.token).isEqualTo("TTK")
        assertThat(event.blockchain).isEqualTo("Ethereum")
    }

    @Test
    fun `GIVEN fee equal to max WHEN transform THEN not high fee`() {
        // Arrange — boundary: comparison is strictly greater-than
        val transformer = createTransformer(feeValue = BigDecimal("2"), tokenMaxFee = BigDecimal("2"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.isHighFee).isFalse()
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN missing fiat rate WHEN transform THEN current fee is the placeholder and high fee resolved by crypto`() {
        // Arrange — null fiat rate: fiat fee text falls back to the placeholder, high-fee logic unaffected
        val transformer = createTransformer(
            feeValue = BigDecimal("3"),
            tokenMaxFee = BigDecimal("2"),
            fiatRate = null,
        )

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — placeholder differs from a populated fiat value, proving the null branch was taken
        assertThat(result.currentFee).isEqualTo(stringReference(fiatText(null)))
        assertThat(result.isHighFee).isTrue()
        verify(exactly = 1) { analyticsHandler.send(any()) }
    }

    private fun cryptoText(value: BigDecimal): String = value.format { crypto(token) }

    private fun fiatText(value: BigDecimal?): String = value.format { fiat(appCurrency.code, appCurrency.symbol) }

    private fun createTransformer(
        feeValue: BigDecimal,
        tokenMaxFee: BigDecimal,
        fiatRate: BigDecimal? = BigDecimal("1"),
    ): YieldSupplyActiveFeeContentTransformer = YieldSupplyActiveFeeContentTransformer(
        cryptoCurrencyStatus = status(fiatRate = fiatRate),
        appCurrency = appCurrency,
        feeValue = feeValue,
        maxNetworkFee = YieldSupplyMaxFee(
            nativeMaxFee = BigDecimal("0.01"),
            tokenMaxFee = tokenMaxFee,
            fiatMaxFee = BigDecimal("4"),
        ),
        analyticsHandler = analyticsHandler,
    )

    private fun status(fiatRate: BigDecimal?): CryptoCurrencyStatus = CryptoCurrencyStatus(
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

    private fun emptyContent(): YieldSupplyActiveContentUM = YieldSupplyActiveContentUM(
        totalEarnings = stringReference(""),
        availableBalance = null,
        providerTitle = stringReference(""),
        subtitle = stringReference(""),
        subtitleLink = stringReference(""),
        notifications = persistentListOf(),
        minAmount = null,
        currentFee = null,
        feeDescription = null,
        minFeeDescription = null,
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