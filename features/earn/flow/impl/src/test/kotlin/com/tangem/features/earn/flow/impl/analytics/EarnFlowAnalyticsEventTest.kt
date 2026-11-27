package com.tangem.features.earn.flow.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.flow.impl.model.TestClaim
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnFlowAnalyticsEventTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EventContract {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN flow event WHEN built THEN id and params match the spec`(model: EventModel) {
            // Act
            val event = model.event

            // Assert
            assertThat(event.id).isEqualTo(model.expectedId)
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            EventModel(
                event = EarnFlowAnalyticsEvent.DepositScreenOpened(
                    category = CATEGORY,
                    source = "Token Page",
                    currentApy = "5.50",
                ),
                expectedId = "[ETH Lending] Deposit Screen Opened",
                expectedParams = mapOf("Source" to "Token Page", "Current APY" to "5.50"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.DepositScreenOpened(
                    category = CATEGORY,
                    source = "Token Page",
                    currentApy = null,
                ),
                expectedId = "[ETH Lending] Deposit Screen Opened",
                expectedParams = mapOf("Source" to "Token Page"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                    category = CATEGORY,
                    positionBucket = "1K-5K",
                    isLiquidityCapped = true,
                ),
                expectedId = "[ETH Lending] Withdraw Screen Opened",
                expectedParams = mapOf("Position Bucket" to "1K-5K", "Liquidity Capped" to "true"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                    category = CATEGORY,
                    positionBucket = "<50",
                    isLiquidityCapped = false,
                ),
                expectedId = "[ETH Lending] Withdraw Screen Opened",
                expectedParams = mapOf("Position Bucket" to "<50", "Liquidity Capped" to "false"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                    category = CATEGORY,
                    positionBucket = "1K-5K",
                    isLiquidityCapped = null,
                ),
                expectedId = "[ETH Lending] Withdraw Screen Opened",
                expectedParams = mapOf("Position Bucket" to "1K-5K"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.WithdrawScreenOpened(
                    category = CATEGORY,
                    positionBucket = null,
                    isLiquidityCapped = null,
                ),
                expectedId = "[ETH Lending] Withdraw Screen Opened",
                expectedParams = emptyMap(),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.HowItWorksOpened(
                    category = CATEGORY,
                    source = EarnEntrySource.EarnBanner,
                ),
                expectedId = "[ETH Lending] How It Works Sheet Opened",
                expectedParams = mapOf("Source" to "Earn Banner"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.LearnMore(
                    category = CATEGORY,
                    source = EarnEntrySource.Markets,
                ),
                expectedId = "[ETH Lending] Button - Learn More",
                expectedParams = mapOf("Source" to "Markets"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.CurrencyToggle(
                    category = CATEGORY,
                    currency = EarnDisplayCurrency.Fiat,
                    screen = EarnFlowScreen.Deposit,
                ),
                expectedId = "[ETH Lending] Button - Currency Toggle",
                expectedParams = mapOf("Display Currency" to "Fiat", "Screen" to "Deposit"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.Max(category = CATEGORY, screen = EarnFlowScreen.Withdraw),
                expectedId = "[ETH Lending] Button - Max",
                expectedParams = mapOf("Screen" to "Withdraw"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.Percent(
                    category = CATEGORY,
                    percent = 25,
                    screen = EarnFlowScreen.Deposit,
                ),
                expectedId = "[ETH Lending] Button - Percent",
                expectedParams = mapOf("Percent" to "25", "Screen" to "Deposit"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.FeeSpeed(
                    category = CATEGORY,
                    feeType = AnalyticsParam.FeeType.Max,
                    screen = EarnFlowScreen.Confirm,
                ),
                expectedId = "[ETH Lending] Button - Fee Speed",
                expectedParams = mapOf("Fee Speed" to "Max", "Screen" to "Confirm"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.InsufficientBalance(
                    category = CATEGORY,
                    screen = EarnFlowScreen.Deposit,
                ),
                expectedId = "[ETH Lending] Notice - Insufficient Balance",
                expectedParams = mapOf("Screen" to "Deposit"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.InsufficientFee(
                    category = CATEGORY,
                    screen = EarnFlowScreen.Confirm,
                ),
                expectedId = "[ETH Lending] Notice - Insufficient Fee",
                expectedParams = mapOf("Screen" to "Confirm"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.ConfirmScreenOpened(
                    category = CATEGORY,
                    type = EarnTxType.Deposit,
                    amountBucket = "1K-5K",
                    currentApy = "5.50",
                    performanceFee = "15.00",
                ),
                expectedId = "[ETH Lending] Confirm Screen Opened",
                expectedParams = mapOf(
                    "Transaction Type" to "Deposit",
                    "Amount Bucket" to "1K-5K",
                    "Current APY" to "5.50",
                    "Performance Fee" to "15.00",
                ),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.ConfirmScreenOpened(
                    category = CATEGORY,
                    type = EarnTxType.Withdraw,
                    amountBucket = null,
                    currentApy = null,
                    performanceFee = null,
                ),
                expectedId = "[ETH Lending] Confirm Screen Opened",
                expectedParams = mapOf("Transaction Type" to "Withdraw"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.ErrorPopup(
                    category = CATEGORY,
                    type = EarnTxType.Withdraw,
                    errorCode = "100",
                ),
                expectedId = "[ETH Lending] Notice - Error Popup",
                expectedParams = mapOf("Transaction Type" to "Withdraw", "Error Code" to "100"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.ErrorPopupButton(
                    category = CATEGORY,
                    type = EarnTxType.Deposit,
                    action = EarnErrorPopupAction.Support,
                ),
                expectedId = "[ETH Lending] Button - Error Popup",
                expectedParams = mapOf("Transaction Type" to "Deposit", "Action" to "Support"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.SuccessScreenOpened(
                    category = CATEGORY,
                    type = EarnTxType.Deposit,
                    currentApy = "5.50",
                ),
                expectedId = "[ETH Lending] Success Screen Opened",
                expectedParams = mapOf("Transaction Type" to "Deposit", "Current APY" to "5.50"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.SuccessScreenOpened(
                    category = CATEGORY,
                    type = EarnTxType.Withdraw,
                    currentApy = null,
                ),
                expectedId = "[ETH Lending] Success Screen Opened",
                expectedParams = mapOf("Transaction Type" to "Withdraw"),
            ),
            EventModel(
                event = EarnFlowAnalyticsEvent.ResultAction(
                    category = CATEGORY,
                    destination = EarnResultDestination.Yield,
                ),
                expectedId = "[ETH Lending] Button - Result Action",
                expectedParams = mapOf("Destination" to "Yield"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PercentValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN percent button WHEN built THEN percent is the plain number`(model: PercentModel) {
            // Act
            val event = EarnFlowAnalyticsEvent.Percent(
                category = CATEGORY,
                percent = model.percent,
                screen = EarnFlowScreen.Deposit,
            )

            // Assert
            assertThat(event.params).isEqualTo(mapOf("Percent" to model.expected, "Screen" to "Deposit"))
        }

        private fun provideTestModels() = listOf(
            PercentModel(percent = 25, expected = "25"),
            PercentModel(percent = 50, expected = "50"),
            PercentModel(percent = 75, expected = "75"),
            PercentModel(percent = 100, expected = "100"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeSpeedValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee type WHEN fee speed built THEN spec string`(model: FeeSpeedModel) {
            // Act
            val event = EarnFlowAnalyticsEvent.FeeSpeed(
                category = CATEGORY,
                feeType = model.feeType,
                screen = EarnFlowScreen.Confirm,
            )

            // Assert
            assertThat(event.params).isEqualTo(mapOf("Fee Speed" to model.expected, "Screen" to "Confirm"))
        }

        private fun provideTestModels() = listOf(
            FeeSpeedModel(AnalyticsParam.FeeType.Min, expected = "Min"),
            FeeSpeedModel(AnalyticsParam.FeeType.Normal, expected = "Normal"),
            FeeSpeedModel(AnalyticsParam.FeeType.Max, expected = "Max"),
            FeeSpeedModel(AnalyticsParam.FeeType.Custom, expected = "Custom"),
            FeeSpeedModel(AnalyticsParam.FeeType.Fixed, expected = "Fixed"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EnumValues {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN analytics enum constant WHEN value read THEN spec string`(model: EnumValueModel) {
            // Act
            val actual = model.value

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            EnumValueModel(EarnFlowScreen.Deposit, EarnFlowScreen.Deposit.value, expected = "Deposit"),
            EnumValueModel(EarnFlowScreen.Withdraw, EarnFlowScreen.Withdraw.value, expected = "Withdraw"),
            EnumValueModel(EarnFlowScreen.Confirm, EarnFlowScreen.Confirm.value, expected = "Confirm"),
            EnumValueModel(EarnTxType.Deposit, EarnTxType.Deposit.value, expected = "Deposit"),
            EnumValueModel(EarnTxType.Withdraw, EarnTxType.Withdraw.value, expected = "Withdraw"),
            EnumValueModel(EarnDisplayCurrency.Crypto, EarnDisplayCurrency.Crypto.value, expected = "Crypto"),
            EnumValueModel(EarnDisplayCurrency.Fiat, EarnDisplayCurrency.Fiat.value, expected = "Fiat"),
            EnumValueModel(EarnErrorPopupAction.Close, EarnErrorPopupAction.Close.value, expected = "Close"),
            EnumValueModel(EarnErrorPopupAction.Support, EarnErrorPopupAction.Support.value, expected = "Support"),
            EnumValueModel(EarnResultDestination.Token, EarnResultDestination.Token.value, expected = "Token"),
            EnumValueModel(EarnResultDestination.Yield, EarnResultDestination.Yield.value, expected = "Yield"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TxTypeOfAction {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN flow action WHEN toTxType THEN transaction type or null`(model: ActionModel) {
            // Act
            val actual = model.action.toTxType()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ActionModel(Deposit(), expected = EarnTxType.Deposit),
            ActionModel(Deposit(isGaslessAllowed = true), expected = EarnTxType.Deposit),
            ActionModel(Withdraw(), expected = EarnTxType.Withdraw),
            ActionModel(Withdraw(isGaslessAllowed = true), expected = EarnTxType.Withdraw),
            ActionModel(TestClaim, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AmountScreenOfType {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN transaction type WHEN amountScreen THEN matching screen`(model: AmountScreenModel) {
            // Act
            val actual = model.type.amountScreen()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AmountScreenModel(EarnTxType.Deposit, expected = EarnFlowScreen.Deposit),
            AmountScreenModel(EarnTxType.Withdraw, expected = EarnFlowScreen.Withdraw),
        )
    }

    internal data class EventModel(
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = "$expectedId $expectedParams"
    }

    internal data class PercentModel(val percent: Int, val expected: String) {
        override fun toString(): String = "$percent -> $expected"
    }

    internal data class FeeSpeedModel(val feeType: AnalyticsParam.FeeType, val expected: String) {
        override fun toString(): String = "${feeType.value} -> $expected"
    }

    internal data class EnumValueModel(val constant: Enum<*>, val value: String, val expected: String) {
        override fun toString(): String = "${constant.javaClass.simpleName}.${constant.name} -> $expected"
    }

    internal data class ActionModel(val action: EarnFlowAction, val expected: EarnTxType?) {
        override fun toString(): String = "${action.id} -> $expected"
    }

    internal data class AmountScreenModel(val type: EarnTxType, val expected: EarnFlowScreen) {
        override fun toString(): String = "$type -> $expected"
    }

    private companion object {
        const val CATEGORY = "ETH Lending"
    }
}