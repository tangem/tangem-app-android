package com.tangem.features.yield.supply.impl.active.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
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

internal class YieldSupplyActiveMinAmountTransformerTest {

    private val analyticsHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val token = createToken()
    private val appCurrency = AppCurrency.Default
    private var approveClicked = false

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsHandler)
        approveClicked = false
    }

    @Test
    fun `GIVEN spending not allowed and nothing un-supplied WHEN transform THEN approval notification and min amount texts`() {
        // Arrange
        val status = status(amount = BigDecimal("5"), isAllowedToSpend = false, effectiveProtocolBalance = BigDecimal("5"))
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — minAmount uses the fiat value (minAmount * fiatRate); minFeeDescription carries [fiat, crypto] in order
        val expectedMinFiat = fiatText(MIN_AMOUNT.multiply(BigDecimal("1")))
        val expectedMinCrypto = cryptoText(MIN_AMOUNT)
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first()).isInstanceOf(NotificationUM.Error::class.java)
        assertThat(result.minAmount).isEqualTo(stringReference(expectedMinFiat))
        assertThat(result.minFeeDescription).isEqualTo(
            resourceReference(
                id = R.string.yield_module_fee_policy_sheet_min_amount_note,
                formatArgs = wrappedList(expectedMinFiat, expectedMinCrypto),
            ),
        )
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN spending allowed and un-supplied above dust WHEN transform THEN not-supplied notification with amount and analytics`() {
        // Arrange — un-supplied = amount(10) - protocolBalance(1) = 9
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("1"),
            fiatRate = BigDecimal("1"),
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))
        val eventSlot = slot<AnalyticsEvent>()

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.notifications).hasSize(1)
        val notification = result.notifications.first() as NotificationUM.Info.YieldSupplyNotAllAmountSupplied
        assertThat(notification.symbol).isEqualTo(TOKEN_SYMBOL)
        assertThat(notification.formattedAmount).isEqualTo(notSuppliedText(BigDecimal("9")))
        verify(exactly = 1) { analyticsHandler.send(capture(eventSlot)) }
        val event = eventSlot.captured as YieldSupplyAnalytics.NoticeAmountNotDeposited
        assertThat(event.token).isEqualTo(TOKEN_SYMBOL)
        assertThat(event.blockchain).isEqualTo("Ethereum")
    }

    @Test
    fun `GIVEN spending allowed and fully supplied WHEN transform THEN no notifications`() {
        // Arrange
        val status = status(amount = BigDecimal("5"), isAllowedToSpend = true, effectiveProtocolBalance = BigDecimal("5"))
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.notifications).isEmpty()
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN un-supplied amount below dust threshold WHEN transform THEN no not-supplied notification`() {
        // Arrange — un-supplied = 1 (fiat), dust threshold = 5 → below threshold
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("9"),
            fiatRate = BigDecimal("1"),
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("5"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.notifications).isEmpty()
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN un-supplied fiat equals dust threshold WHEN transform THEN not-supplied notification shown`() {
        // Arrange — boundary: shouldShowNotSuppliedNotification uses >=, so equality must show the notification
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("5"),
            fiatRate = BigDecimal("1"),
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("5"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — un-supplied fiat = (10-5)*1 = 5 == dust 5
        assertThat(result.notifications).hasSize(1)
        assertThat(result.notifications.first())
            .isInstanceOf(NotificationUM.Info.YieldSupplyNotAllAmountSupplied::class.java)
        verify(exactly = 1) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN supply inactive WHEN transform THEN no not-supplied notification even if balance differs`() {
        // Arrange — isActive=false short-circuits notSupplied calculation
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = true,
            isActive = false,
            effectiveProtocolBalance = BigDecimal("1"),
            fiatRate = BigDecimal("1"),
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert
        assertThat(result.notifications).isEmpty()
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN missing fiat rate WHEN transform THEN min amount is the placeholder and no not-supplied notification`() {
        // Arrange — null fiat rate: fiat min amount cannot be computed, not-supplied calc is skipped
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = true,
            isActive = false,
            effectiveProtocolBalance = BigDecimal("1"),
            fiatRate = null,
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — minAmount falls back to the null-rate placeholder
        assertThat(result.minAmount).isEqualTo(stringReference(fiatText(null)))
        assertThat(result.notifications).isEmpty()
        verify(exactly = 0) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN approval needed and un-supplied above dust WHEN transform THEN both notifications in order`() {
        // Arrange
        val status = status(
            amount = BigDecimal("10"),
            isAllowedToSpend = false,
            effectiveProtocolBalance = BigDecimal("1"),
            fiatRate = BigDecimal("1"),
        )
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())

        // Assert — approval first, then not-supplied (listOfNotNull order)
        assertThat(result.notifications).hasSize(2)
        assertThat(result.notifications[0]).isInstanceOf(NotificationUM.Error::class.java)
        assertThat(result.notifications[1])
            .isInstanceOf(NotificationUM.Info.YieldSupplyNotAllAmountSupplied::class.java)
        verify(exactly = 1) { analyticsHandler.send(any()) }
    }

    @Test
    fun `GIVEN approval notification WHEN its button clicked THEN onApprove fires`() {
        // Arrange
        val status = status(amount = BigDecimal("5"), isAllowedToSpend = false, effectiveProtocolBalance = BigDecimal("5"))
        val transformer = createTransformer(status = status, dustMinAmount = BigDecimal("1"))

        // Act
        val result = transformer.transform(emptyContent())
        val button = (result.notifications.first() as NotificationUM.Error)
            .config.buttonsState as NotificationConfig.ButtonsState.PrimaryButtonConfig
        button.onClick()

        // Assert
        assertThat(approveClicked).isTrue()
    }

    private fun cryptoText(value: BigDecimal): String = value.format { crypto(token) }

    private fun fiatText(value: BigDecimal?): String = value.format { fiat(appCurrency.code, appCurrency.symbol) }

    private fun notSuppliedText(value: BigDecimal): String = value.format { crypto(symbol = "", decimals = token.decimals) }

    private fun createTransformer(
        status: CryptoCurrencyStatus,
        dustMinAmount: BigDecimal,
    ): YieldSupplyActiveMinAmountTransformer = YieldSupplyActiveMinAmountTransformer(
        cryptoCurrencyStatus = status,
        appCurrency = appCurrency,
        minAmount = MIN_AMOUNT,
        dustMinAmount = dustMinAmount,
        analyticsHandler = analyticsHandler,
        onApprove = { approveClicked = true },
    )

    private fun status(
        amount: BigDecimal,
        isAllowedToSpend: Boolean,
        isActive: Boolean = true,
        effectiveProtocolBalance: BigDecimal? = null,
        fiatRate: BigDecimal? = BigDecimal("1"),
    ): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token,
        value = CryptoCurrencyStatus.Custom(
            amount = amount,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = fiatRate,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = isActive,
                isInitialized = true,
                isAllowedToSpend = isAllowedToSpend,
                effectiveProtocolBalance = effectiveProtocolBalance,
            ),
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
            symbol = TOKEN_SYMBOL,
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }

    private companion object {
        const val TOKEN_SYMBOL = "TTK"
        val MIN_AMOUNT: BigDecimal = BigDecimal("2")
    }
}