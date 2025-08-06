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
import com.tangem.domain.tokens.model.Amount as DomainAmount
import com.tangem.features.send.v2.api.entity.FeeSelectorUM as FeeSelectorUMV2

class TransformersComparisonTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")
    private val analyticsCategoryName = "test_category"

    @Test
    fun `GIVEN equivalent input data WHEN both transformers transform THEN they produce equal ConfirmUM`() = runTest {
        // GIVEN
        val initialConfirmUM = createTestConfirmUM()
        val amountUM = createTestAmountUM()

        val feeUM = createTestFeeUM()
        val feeSelectorUMV2 = createTestFeeSelectorUMV2()

        // WHEN
        val transformerV1 = SendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val transformerV2 = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUMV2,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val resultV1 = transformerV1.transform(initialConfirmUM)
        val resultV2 = transformerV2.transform(initialConfirmUM)

        // THEN
        assertThat(resultV1).isInstanceOf(ConfirmUM.Content::class.java)
        assertThat(resultV2).isInstanceOf(ConfirmUM.Content::class.java)

        val contentV1 = resultV1 as ConfirmUM.Content
        val contentV2 = resultV2 as ConfirmUM.Content

        assertThat(contentV1.isPrimaryButtonEnabled).isEqualTo(contentV2.isPrimaryButtonEnabled)
        assertThat(contentV1.notifications.size).isEqualTo(contentV2.notifications.size)
        assertThat(contentV1.sendingFooter).isEqualTo(contentV2.sendingFooter)
        assertThat(contentV1.isPrimaryButtonEnabled).isEqualTo(contentV2.isPrimaryButtonEnabled)
        assertThat(contentV1.isSending).isEqualTo(contentV2.isSending)
        assertThat(contentV1.showTapHelp).isEqualTo(contentV2.showTapHelp)
    }

    @Test
    fun `GIVEN fee too low WHEN both transformers transform THEN they produce equal notifications`() = runTest {
        // GIVEN
        val initialConfirmUM = createTestConfirmUM()
        val amountUM = createTestAmountUM()

        val feeUM = createFeeTooLowUM()
        val feeSelectorUMV2 = createFeeTooLowUMV2()

        // WHEN
        val transformerV1 = SendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val transformerV2 = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUMV2,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val resultV1 = transformerV1.transform(initialConfirmUM)
        val resultV2 = transformerV2.transform(initialConfirmUM)

        // THEN
        assertThat(resultV1).isInstanceOf(ConfirmUM.Content::class.java)
        assertThat(resultV2).isInstanceOf(ConfirmUM.Content::class.java)

        val contentV1 = resultV1 as ConfirmUM.Content
        val contentV2 = resultV2 as ConfirmUM.Content

        assertThat(contentV1.isPrimaryButtonEnabled).isTrue()
        assertThat(contentV2.isPrimaryButtonEnabled).isTrue()
        assertThat(contentV1.notifications).hasSize(1)
        assertThat(contentV2.notifications).hasSize(1)
        assertThat(contentV1.notifications.first()).isInstanceOf(NotificationUM.Warning.FeeTooLow::class.java)
        assertThat(contentV2.notifications.first()).isInstanceOf(NotificationUM.Warning.FeeTooLow::class.java)
        assertThat(contentV1.sendingFooter).isEqualTo(contentV2.sendingFooter)

        // Verify analytics event was sent for both transformers
        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN fee too high WHEN both transformers transform THEN they produce equal notifications`() = runTest {
        // GIVEN
        val initialConfirmUM = createTestConfirmUM()
        val amountUM = createTestAmountUM()

        val feeUM = createFeeTooHighUM()
        val feeSelectorUMV2 = createFeeTooHighUMV2()

        // WHEN
        val transformerV1 = SendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val transformerV2 = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUMV2,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val resultV1 = transformerV1.transform(initialConfirmUM)
        val resultV2 = transformerV2.transform(initialConfirmUM)

        // THEN
        assertThat(resultV1).isInstanceOf(ConfirmUM.Content::class.java)
        assertThat(resultV2).isInstanceOf(ConfirmUM.Content::class.java)

        val contentV1 = resultV1 as ConfirmUM.Content
        val contentV2 = resultV2 as ConfirmUM.Content

        assertThat(contentV1.isPrimaryButtonEnabled).isTrue()
        assertThat(contentV2.isPrimaryButtonEnabled).isTrue()
        assertThat(contentV1.notifications).hasSize(1)
        assertThat(contentV2.notifications).hasSize(1)
        assertThat(contentV1.notifications.first()).isInstanceOf(NotificationUM.Warning.TooHigh::class.java)
        assertThat(contentV2.notifications.first()).isInstanceOf(NotificationUM.Warning.TooHigh::class.java)

        val tooHighV1 = contentV1.notifications.first() as NotificationUM.Warning.TooHigh
        val tooHighV2 = contentV2.notifications.first() as NotificationUM.Warning.TooHigh
        assertThat(tooHighV1.value).isEqualTo(tooHighV2.value)
        assertThat(contentV1.sendingFooter).isEqualTo(contentV2.sendingFooter)
    }

    @Test
    fun `GIVEN fee both too high and too low WHEN both transformers transform THEN they produce equal notifications`() =
        runTest {
            // GIVEN
            val initialConfirmUM = createTestConfirmUM()
            val amountUM = createTestAmountUM()

            val feeUM = createFeeTooHighAndTooLowUM()
            val feeSelectorUMV2 = createFeeTooHighAndTooLowUMV2()

            // WHEN
            val transformerV1 = SendConfirmationNotificationsTransformer(
                feeUM = feeUM,
                amountUM = amountUM,
                analyticsEventHandler = analyticsEventHandler,
                cryptoCurrency = cryptoCurrency,
                appCurrency = appCurrency,
                analyticsCategoryName = analyticsCategoryName,
            )

            val transformerV2 = SendConfirmationNotificationsTransformerV2(
                feeSelectorUM = feeSelectorUMV2,
                amountUM = amountUM,
                analyticsEventHandler = analyticsEventHandler,
                cryptoCurrency = cryptoCurrency,
                appCurrency = appCurrency,
                analyticsCategoryName = analyticsCategoryName,
            )

            val resultV1 = transformerV1.transform(initialConfirmUM)
            val resultV2 = transformerV2.transform(initialConfirmUM)

            // THEN
            assertThat(resultV1).isInstanceOf(ConfirmUM.Content::class.java)
            assertThat(resultV2).isInstanceOf(ConfirmUM.Content::class.java)

            val contentV1 = resultV1 as ConfirmUM.Content
            val contentV2 = resultV2 as ConfirmUM.Content

            assertThat(contentV1.isPrimaryButtonEnabled).isTrue()
            assertThat(contentV2.isPrimaryButtonEnabled).isTrue()
            assertThat(contentV1.notifications).hasSize(2)
            assertThat(contentV2.notifications).hasSize(2)

            assertThat(contentV1.notifications.any { it is NotificationUM.Warning.FeeTooLow }).isTrue()
            assertThat(contentV1.notifications.any { it is NotificationUM.Warning.TooHigh }).isTrue()
            assertThat(contentV2.notifications.any { it is NotificationUM.Warning.FeeTooLow }).isTrue()
            assertThat(contentV2.notifications.any { it is NotificationUM.Warning.TooHigh }).isTrue()

            assertThat(contentV1.sendingFooter).isEqualTo(contentV2.sendingFooter)

            verify(exactly = 2) { analyticsEventHandler.send(any()) }
        }

    @Test
    fun `GIVEN normal fee WHEN both transformers transform THEN they produce equal notifications`() = runTest {
        // GIVEN
        val initialConfirmUM = createTestConfirmUM()
        val amountUM = createTestAmountUM()

        val feeUM = createNormalFeeUM()
        val feeSelectorUMV2 = createNormalFeeSelectorUMV2()

        // WHEN
        val transformerV1 = SendConfirmationNotificationsTransformer(
            feeUM = feeUM,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val transformerV2 = SendConfirmationNotificationsTransformerV2(
            feeSelectorUM = feeSelectorUMV2,
            amountUM = amountUM,
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrency = cryptoCurrency,
            appCurrency = appCurrency,
            analyticsCategoryName = analyticsCategoryName,
        )

        val resultV1 = transformerV1.transform(initialConfirmUM)
        val resultV2 = transformerV2.transform(initialConfirmUM)

        // THEN
        assertThat(resultV1).isInstanceOf(ConfirmUM.Content::class.java)
        assertThat(resultV2).isInstanceOf(ConfirmUM.Content::class.java)

        val contentV1 = resultV1 as ConfirmUM.Content
        val contentV2 = resultV2 as ConfirmUM.Content

        assertThat(contentV1.notifications).isEmpty()
        assertThat(contentV2.notifications).isEmpty()
        assertThat(contentV1.isPrimaryButtonEnabled).isEqualTo(contentV2.isPrimaryButtonEnabled)
        assertThat(contentV1.sendingFooter).isEqualTo(contentV2.sendingFooter)
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
            currencySymbol = "TST",
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

    private fun createTestFeeUM(): FeeUM.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
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
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.001",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "TST",
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

    private fun createTestFeeSelectorUMV2(): FeeSelectorUMV2.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Single(fee)
        return FeeSelectorUMV2.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Market(fee),
            ),
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

    private fun createFeeTooLowUM(): FeeUM.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.0001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
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
                        symbol = "TST",
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
            isCustomSelected = true,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooLowUMV2(): FeeSelectorUMV2.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.0001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = fee,
            priority = fee,
        )
        return FeeSelectorUMV2.Content(
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
                            symbol = "TST",
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
                        symbol = "TST",
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

    private fun createFeeTooHighUM(): FeeUM.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
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
                        symbol = "TST",
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
            isCustomSelected = true,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooHighUMV2(): FeeSelectorUMV2.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.01"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = minimumFee,
            priority = priorityFee,
        )
        return FeeSelectorUMV2.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Custom(
                    fee = customFee,
                    customValues = persistentListOf(
                        CustomFeeFieldUM(
                            value = "0.01",
                            onValueChange = {},
                            keyboardOptions = mockk(relaxed = true),
                            keyboardActions = mockk(relaxed = true),
                            symbol = "TST",
                            decimals = 8,
                            title = mockk(relaxed = true),
                            footer = mockk(relaxed = true),
                        ),
                    ),
                ),
            ),
            selectedFeeItem = FeeItem.Custom(
                fee = customFee,
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.01",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "TST",
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

    private fun createFeeTooHighAndTooLowUM(): FeeUM.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.01"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
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
                        symbol = "TST",
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
            isCustomSelected = true,
            notifications = persistentListOf(),
        )
    }

    private fun createFeeTooHighAndTooLowUMV2(): FeeSelectorUMV2.Content {
        val priorityFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val minimumFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.01"),
                decimals = 8,
            ),
        )
        val customFee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.008"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Choosable(
            minimum = minimumFee,
            normal = minimumFee,
            priority = priorityFee,
        )
        return FeeSelectorUMV2.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Custom(
                    fee = customFee,
                    customValues = persistentListOf(
                        CustomFeeFieldUM(
                            value = "0.008",
                            onValueChange = {},
                            keyboardOptions = mockk(relaxed = true),
                            keyboardActions = mockk(relaxed = true),
                            symbol = "TST",
                            decimals = 8,
                            title = mockk(relaxed = true),
                            footer = mockk(relaxed = true),
                        ),
                    ),
                ),
            ),
            selectedFeeItem = FeeItem.Custom(
                fee = customFee,
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.008",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "TST",
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

    private fun createNormalFeeUM(): FeeUM.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
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
                customValues = persistentListOf(
                    CustomFeeFieldUM(
                        value = "0.001",
                        onValueChange = {},
                        keyboardOptions = mockk(relaxed = true),
                        keyboardActions = mockk(relaxed = true),
                        symbol = "TST",
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

    private fun createNormalFeeSelectorUMV2(): FeeSelectorUMV2.Content {
        val fee = Fee.Common(
            amount = Amount(
                currencySymbol = "TST",
                value = BigDecimal("0.001"),
                decimals = 8,
            ),
        )
        val transactionFee = TransactionFee.Single(fee)
        return FeeSelectorUMV2.Content(
            isPrimaryButtonEnabled = true,
            fees = transactionFee,
            feeItems = persistentListOf(
                FeeItem.Market(fee),
            ),
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

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpLocale() {
            Locale.setDefault(Locale.US)
        }
    }
}