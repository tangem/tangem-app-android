package com.tangem.features.send.v2.send.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.sendnft.confirm.model.transformers.NFTSendConfirmationNotificationsTransformer
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

class NFTSendConfirmationNotificationsTransformerTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")
    private val analyticsCategoryName = "test_category"

    @Test
    fun `GIVEN non content state WHEN transform THEN returns original state`() = runTest {
        // GIVEN
        val feeUM: FeeUM = mockk(relaxed = true)
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState: ConfirmUM = ConfirmUM.Empty

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isEqualTo(initialState)
    }

    @Test
    fun `GIVEN fee UM not content WHEN transform THEN returns original state`() = runTest {
        // GIVEN
        val feeUM: FeeUM = FeeUM.Empty()
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState: ConfirmUM.Content = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isEqualTo(initialState)
    }

    @Test
    fun `GIVEN normal fee WHEN transform THEN returns state with footer and no notifications`() = runTest {
        // GIVEN
        val feeUM = createNormalFeeUM()
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isInstanceOf(ConfirmUM.Content::class.java)
        val content = result as ConfirmUM.Content
        assertThat(content.notifications).isEmpty()
        assertThat(content.sendingFooter).isNotEqualTo(initialState.sendingFooter)
    }

    @Test
    fun `GIVEN fee too high WHEN transform THEN returns state with too high notification`() = runTest {
        // GIVEN
        val feeUM = createFeeTooHighUM()
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isInstanceOf(ConfirmUM.Content::class.java)
        val content = result as ConfirmUM.Content
        assertThat(content.notifications).hasSize(1)
        assertThat(content.notifications.first()).isInstanceOf(NotificationUM.Warning.TooHigh::class.java)
    }

    @Test
    fun `GIVEN fee too low WHEN transform THEN returns state with too low notification`() = runTest {
        // GIVEN
        val feeUM = createFeeTooLowUM()
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isInstanceOf(ConfirmUM.Content::class.java)
        val content = result as ConfirmUM.Content
        assertThat(content.notifications).hasSize(1)
        assertThat(content.notifications.first()).isInstanceOf(NotificationUM.Warning.FeeTooLow::class.java)
        verify { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN both fee too high and too low WHEN transform THEN returns state with both notifications`() = runTest {
        // GIVEN
        val feeUM = createFeeTooHighAndTooLowUM()
        val transformer = NFTSendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isInstanceOf(ConfirmUM.Content::class.java)
        val content = result as ConfirmUM.Content
        assertThat(content.notifications).hasSize(2)
        assertThat(content.notifications.any { it is NotificationUM.Warning.TooHigh }).isTrue()
        assertThat(content.notifications.any { it is NotificationUM.Warning.FeeTooLow }).isTrue()
    }

    private fun createTestConfirmUM(): ConfirmUM.Content {
        return ConfirmUM.Content(
            isPrimaryButtonEnabled = true,
            walletName = mockk(relaxed = true),
            isSending = false,
            showTapHelp = false,
            sendingFooter = mockk(relaxed = true),
            notifications = persistentListOf(),
        )
    }

    private fun createNormalFeeUM(): FeeUM.Content {
        val fee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Single(fee)
        return FeeUM.Content(
            feeSelectorUM = FeeSelectorUM.Content(
                fees = transactionFee,
                selectedType = FeeType.Market,
                selectedFee = fee,
                customValues = persistentListOf(),
                nonce = BigInteger.ZERO,
            ),
            rate = BigDecimal("50000"),
            isFeeConvertibleToFiat = true,
            isFeeApproximate = false,
            isTronToken = false,
            isEditingDisabled = false,
            isPrimaryButtonEnabled = true,
            appCurrency = AppCurrency.Default,
            isCustomSelected = false,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooHighUM(): FeeUM.Content {
        val priorityFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.01"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = minimumFee,
            priority = priorityFee,
        )
        return FeeUM.Content(
            feeSelectorUM = FeeSelectorUM.Content(
                fees = transactionFee,
                selectedType = FeeType.Custom,
                selectedFee = customFee,
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.01",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "SOL",
                        decimals = 8,
                        title = mockk(relaxed = true),
                        footer = mockk(relaxed = true),
                    ),
                ),
                nonce = BigInteger.ZERO,
            ),
            rate = BigDecimal("50000"),
            isFeeConvertibleToFiat = true,
            isFeeApproximate = false,
            isTronToken = false,
            isEditingDisabled = false,
            isPrimaryButtonEnabled = true,
            appCurrency = AppCurrency.Default,
            isCustomSelected = false,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooLowUM(): FeeUM.Content {
        val fee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.0001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = fee,
            priority = fee,
        )
        return FeeUM.Content(
            feeSelectorUM = FeeSelectorUM.Content(
                fees = transactionFee,
                selectedType = FeeType.Custom,
                selectedFee = fee,
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.0001",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "SOL",
                        decimals = 8,
                        title = mockk(relaxed = true),
                        footer = mockk(relaxed = true),
                    ),
                ),
                nonce = BigInteger.ZERO,
            ),
            rate = BigDecimal("50000"),
            isFeeConvertibleToFiat = true,
            isFeeApproximate = false,
            isTronToken = false,
            isEditingDisabled = false,
            isPrimaryButtonEnabled = true,
            appCurrency = AppCurrency.Default,
            isCustomSelected = false,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooHighAndTooLowUM(): FeeUM.Content {
        val priorityFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.01"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.008"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = minimumFee,
            priority = priorityFee,
        )
        return FeeUM.Content(
            feeSelectorUM = FeeSelectorUM.Content(
                fees = transactionFee,
                selectedType = FeeType.Custom,
                selectedFee = customFee,
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.008",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "SOL",
                        decimals = 8,
                        title = mockk(relaxed = true),
                        footer = mockk(relaxed = true),
                    ),
                ),
                nonce = BigInteger.ZERO,
            ),
            rate = BigDecimal("50000"),
            isFeeConvertibleToFiat = true,
            isFeeApproximate = false,
            isTronToken = false,
            isEditingDisabled = false,
            isPrimaryButtonEnabled = true,
            appCurrency = AppCurrency.Default,
            isCustomSelected = false,
            notifications = persistentListOf(),
        )
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpLocale() {
            Locale.setDefault(Locale.US)
        }
    }
}