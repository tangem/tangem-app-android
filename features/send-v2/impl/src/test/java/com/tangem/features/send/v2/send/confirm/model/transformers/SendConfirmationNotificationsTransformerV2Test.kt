package com.tangem.features.send.v2.send.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.entity.*
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale
import com.tangem.domain.tokens.model.Amount as DomainAmount

class SendConfirmationNotificationsTransformerV2Test {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")
    private val analyticsCategoryName = "test_category"

    @Test
    fun `GIVEN non content state WHEN transform THEN returns original state`() = runTest {
        // GIVEN
        val feeSelectorUM: FeeSelectorUM = mockk(relaxed = true)
        val amountUM: AmountState = mockk(relaxed = true)
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
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
    fun `GIVEN fee selector not content WHEN transform THEN returns original state`() = runTest {
        // GIVEN
        val feeSelectorUM: FeeSelectorUM = FeeSelectorUM.Loading
        val amountUM: AmountState = mockk(relaxed = true)
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )
        val initialState = createTestConfirmUM()

        // WHEN
        val result = transformer.transform(initialState)

        // THEN
        assertThat(result).isEqualTo(initialState)
    }

    @Test
    fun `GIVEN normal fee WHEN transform THEN returns state with footer and no notifications`() = runTest {
        // GIVEN
        val feeSelectorUM = createNormalFeeSelectorUM()
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
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
        val feeSelectorUM = createFeeTooHighUM()
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
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
        val feeSelectorUM = createFeeTooLowUM()
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
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
        val feeSelectorUM = createFeeTooHighAndTooLowUM()
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
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

    private fun createTestAmountUM(): AmountState.Data {
        val cryptoAmount = DomainAmount(
            currencySymbol = "SOL",
            value = BigDecimal("1.5"),
            decimals = 8,
        )
        val fiatAmount = DomainAmount(
            currencySymbol = "USD",
            value = BigDecimal("50.00"),
            decimals = 2,
        )

        return AmountState.Data(
            isPrimaryButtonEnabled = true,
            isRedesignEnabled = false,
            title = mockk(relaxed = true),
            availableBalance = mockk(relaxed = true),
            availableBalanceShort = mockk(relaxed = true),
            tokenName = mockk(relaxed = true),
            tokenIconState = mockk(relaxed = true),
            segmentedButtonConfig = persistentListOf(),
            selectedButton = 0,
            isSegmentedButtonsEnabled = false,
            amountTextField = AmountFieldModel(
                value = "1.5",
                onValueChange = {},
                keyboardOptions = mockk(relaxed = true),
                keyboardActions = mockk(relaxed = true),
                cryptoAmount = cryptoAmount,
                fiatAmount = fiatAmount,
                isFiatValue = false,
                fiatValue = "50.00",
                isFiatUnavailable = false,
                isValuePasted = false,
                onValuePastedTriggerDismiss = {},
                isError = false,
                isWarning = false,
                error = mockk(relaxed = true),
            ),
            appCurrency = appCurrency,
            isEditingDisabled = false,
            reduceAmountBy = BigDecimal.ZERO,
            isIgnoreReduce = false,
        )
    }

    private fun createNormalFeeSelectorUM(): FeeSelectorUM.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Single(fee)
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(FeeItem.Market(fee)),
            selectedFeeItem = FeeItem.Market(fee),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = false,
                isTronToken = false,
            ),
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("50000"),
                appCurrency = appCurrency,
            ),
            feeNonce = FeeNonce.Nonce(
                nonce = BigInteger.ZERO,
                onNonceChange = {},
            ),
        )
    }

    private fun createFeeTooHighUM(): FeeSelectorUM.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = minimumFee,
            priority = priorityFee,
        )
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Custom(
                    fee = Fee.Common(
                        amount = Amount(
                            currencySymbol = "SOL",
                            value = BigDecimal("0.01"),
                            decimals = 8,
                        ),
                    ),
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
                ),
            ),
            selectedFeeItem = FeeItem.Custom(
                fee = Fee.Common(
                    amount = Amount(
                        currencySymbol = "SOL",
                        value = BigDecimal("0.01"),
                        decimals = 8,
                    ),
                ),
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
            ),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = false,
                isTronToken = false,
            ),
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("50000"),
                appCurrency = appCurrency,
            ),
            feeNonce = FeeNonce.Nonce(
                nonce = BigInteger.ZERO,
                onNonceChange = {},
            ),
        )
    }

    private fun createFeeTooLowUM(): FeeSelectorUM.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.0001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
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
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Custom(
                    fee = fee,
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
                ),
            ),
            selectedFeeItem = FeeItem.Custom(
                fee = fee,
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
            ),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = false,
                isTronToken = false,
            ),
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("50000"),
                appCurrency = appCurrency,
            ),
            feeNonce = FeeNonce.Nonce(
                nonce = BigInteger.ZERO,
                onNonceChange = {},
            ),
        )
    }

    private fun createFeeTooHighAndTooLowUM(): FeeSelectorUM.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "SOL",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
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
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Custom(
                    fee = Fee.Common(
                        amount = Amount(
                            currencySymbol = "SOL",
                            value = BigDecimal("0.008"),
                            decimals = 8,
                        ),
                    ),
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
                ),
            ),
            selectedFeeItem = FeeItem.Custom(
                fee = Fee.Common(
                    amount = Amount(
                        currencySymbol = "SOL",
                        value = BigDecimal("0.008"),
                        decimals = 8,
                    ),
                ),
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
            ),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = false,
                isTronToken = false,
            ),
            feeFiatRateUM = FeeFiatRateUM(
                rate = BigDecimal("50000"),
                appCurrency = appCurrency,
            ),
            feeNonce = FeeNonce.Nonce(
                nonce = BigInteger.ZERO,
                onNonceChange = {},
            ),
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