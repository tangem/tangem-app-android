package com.tangem.features.send.v2.send.confirm.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.features.send.api.entity.CustomFeeFieldUM
import com.tangem.features.send.api.entity.FeeExtraInfo
import com.tangem.features.send.api.entity.FeeFiatRateUM
import com.tangem.features.send.api.entity.FeeItem
import com.tangem.features.send.api.entity.FeeNonce
import com.tangem.features.send.api.entity.FeeSelectorUM
import com.tangem.features.send.api.utils.formatFooterFiatFee
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.impl.R
import com.tangem.features.send.send.confirm.model.transformers.SendConfirmationNotificationsTransformerV2
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

    private val cryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = CryptoCurrency.Coin(
            id = CryptoCurrency.ID.fromValue("coin⟨BITCOIN⟩bitcoin"),
            network = Network(
                id = Network.ID(
                    value = "bitcoin",
                    derivationPath = Network.DerivationPath.None,
                ),
                name = "Bitcoin",
                currencySymbol = "BTC",
                derivationPath = Network.DerivationPath.None,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified("bitcoin"),
                hasFiatFeeRate = false,
                canHandleTokens = false,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = "Bitcoin",
            symbol = "BTC",
            decimals = 8,
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/medium/bitcoin.png",
            isCustom = false,
        ),
        value = CryptoCurrencyStatus.Loading,
    )

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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
    fun `GIVEN fee subtracted from amount WHEN transform THEN footer sending excludes the fee`() = runTest {
        // GIVEN: fee is taken out of the entered amount, so the footer must show the amount alone (not amount + fee).
        val feeSelectorUM = createFiatConvertibleFeeSelectorUM(feeValue = BigDecimal("0.001"))
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
            isFeeSubtractedFromAmount = true,
            isFeeExceedingBalance = false,
        )

        // WHEN
        val result = transformer.transform(createTestConfirmUM())

        // THEN: sending = entered fiat amount (50.00), fee NOT added on top.
        val content = result as ConfirmUM.Content
        assertThat(content.sendingFooter).isEqualTo(
            expectedFiatFooter(sendingValue = BigDecimal("50.00"), feeSelectorUM = feeSelectorUM),
        )
    }

    @Test
    fun `GIVEN fee exceeds balance WHEN transform THEN footer sending is zero`() = runTest {
        // GIVEN: the fee alone exceeds the balance → nothing can be sent.
        val feeSelectorUM = createFiatConvertibleFeeSelectorUM(feeValue = BigDecimal("0.001"))
        val amountUM = createTestAmountUM()
        val transformer = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
            isFeeSubtractedFromAmount = true,
            isFeeExceedingBalance = true,
        )

        // WHEN
        val result = transformer.transform(createTestConfirmUM())

        // THEN: sending = $0.
        val content = result as ConfirmUM.Content
        assertThat(content.sendingFooter).isEqualTo(
            expectedFiatFooter(sendingValue = BigDecimal.ZERO, feeSelectorUM = feeSelectorUM),
        )
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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
            isFeeSubtractedFromAmount = false,
            isFeeExceedingBalance = false,
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
            isShowTapHelp = false,
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
            accountTitleUM = mockk(relaxed = true),
            availableBalanceCrypto = mockk(relaxed = true),
            availableBalanceFiat = mockk(relaxed = true),
            tokenName = mockk(relaxed = true),
            tokenIconState = mockk(relaxed = true),
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

    private fun createFiatConvertibleFeeSelectorUM(feeValue: BigDecimal): FeeSelectorUM.Content {
        val fee = Fee.Common(amount = Amount(currencySymbol = "SOL", value = feeValue, decimals = 8))
        return FeeSelectorUM.Content(
            isPrimaryButtonEnabled = true,
            fees = TransactionFee.Single(fee),
            feeItems = persistentListOf(FeeItem.Market(fee)),
            selectedFeeItem = FeeItem.Market(fee),
            feeExtraInfo = FeeExtraInfo(
                isFeeApproximate = false,
                isFeeConvertibleToFiat = true,
                isTronToken = false,
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
            ),
            feeFiatRateUM = FeeFiatRateUM(rate = BigDecimal("50000"), appCurrency = appCurrency),
            feeNonce = FeeNonce.Nonce(nonce = BigInteger.ZERO, onNonceChange = {}),
        )
    }

    /** Builds the expected footer reference for a fiat-convertible fee, mirroring the transformer's formatting. */
    private fun expectedFiatFooter(sendingValue: BigDecimal, feeSelectorUM: FeeSelectorUM.Content): TextReference {
        val fee = feeSelectorUM.selectedFeeItem.fee
        val fiatFeeValue = fee.amount.value?.multiply(feeSelectorUM.feeFiatRateUM!!.rate)
        val sending = sendingValue.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
        val feeText = formatFooterFiatFee(
            amount = fee.amount.copy(value = fiatFeeValue),
            isFeeConvertibleToFiat = true,
            isFeeApproximate = feeSelectorUM.feeExtraInfo.isFeeApproximate,
            appCurrency = appCurrency,
        )
        return resourceReference(
            id = R.string.send_summary_transaction_description,
            formatArgs = wrappedList(sending, feeText),
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
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
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
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
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
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
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
                feeCryptoCurrencyStatus = cryptoCurrencyStatus,
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