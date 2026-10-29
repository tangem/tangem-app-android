package com.tangem.domain.moneyaccount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AccountState
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AddFundsMethod
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.BalanceBand
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class MoneyAccountAnalyticsEventsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CashAccountEvents {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN cash account event WHEN created THEN id and params match the spec`(model: EventModel) {
            // Act
            val event = model.event

            // Assert
            assertThat(event.id).isEqualTo(model.expectedId)
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountScreenOpened(
                    accountState = AccountState.ACTIVE,
                    balanceBand = BalanceBand.FROM_100_TO_1K,
                    currency = "USD",
                ),
                expectedId = "[Cash Account] Account Screen Opened",
                expectedParams = mapOf(
                    "account_state" to "active",
                    "balance_band" to "100-1k",
                    "yield_enabled" to "no",
                    "currency" to "USD",
                ),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountScreenOpened(
                    accountState = AccountState.CLOSED,
                    balanceBand = null,
                    currency = null,
                ),
                expectedId = "[Cash Account] Account Screen Opened",
                expectedParams = mapOf("account_state" to "closed", "yield_enabled" to "no"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AddFundsClicked(),
                expectedId = "[Cash Account] Button - Add Funds",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.SendClicked(),
                expectedId = "[Cash Account] Button - Send",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountMenuClicked(),
                expectedId = "[Cash Account] Button - Account Menu",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.SupportClicked(),
                expectedId = "[Cash Account] Button - Support",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.BalanceErrorBannerShown(),
                expectedId = "[Cash Account] Balance Error Banner Shown",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AddFundsScreenOpened(),
                expectedId = "[Cash Account] Add Funds Screen Opened",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown(),
                expectedId = "[Cash Account] Add Funds Bank Details Shown",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened(),
                expectedId = "[Cash Account] Account Details Screen Opened",
                expectedParams = mapOf("route_state" to "active"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsFieldCopied(field = "routing_number"),
                expectedId = "[Cash Account] Details Field Copied",
                expectedParams = mapOf("field" to "routing_number"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsShared(),
                expectedId = "[Cash Account] Details Shared",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsLoadFailed(),
                expectedId = "[Cash Account] Details Load Failed",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsRetryTapped(),
                expectedId = "[Cash Account] Details Retry Tapped",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsErrorSupportTapped(),
                expectedId = "[Cash Account] Details Error Support Tapped",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddFundsMethodSelected {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN add funds method WHEN event created THEN method param matches the spec`(model: MethodModel) {
            // Act
            val event = MoneyAccountAnalyticsEvents.AddFundsMethodSelected(model.method)

            // Assert
            assertThat(event.id).isEqualTo("[Cash Account] Add Funds Method Selected")
            assertThat(event.params).isEqualTo(mapOf("method" to model.expectedValue))
        }

        private fun provideTestModels() = listOf(
            MethodModel(method = AddFundsMethod.WALLET, expectedValue = "wallet"),
            MethodModel(method = AddFundsMethod.EXTERNAL_WALLET, expectedValue = "external_wallet"),
            MethodModel(method = AddFundsMethod.BANK_TRANSFER, expectedValue = "bank_transfer"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BalanceBandOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balance WHEN band picked THEN band value matches the spec`(model: BandModel) {
            // Act
            val band = BalanceBand.of(model.amount)

            // Assert
            assertThat(band.value).isEqualTo(model.expectedValue)
        }

        private fun provideTestModels() = listOf(
            BandModel(amount = BigDecimal.ZERO, expectedValue = "0"),
            BandModel(amount = BigDecimal("0.01"), expectedValue = "<100"),
            BandModel(amount = BigDecimal("99.99"), expectedValue = "<100"),
            BandModel(amount = BigDecimal("100"), expectedValue = "100-1k"),
            BandModel(amount = BigDecimal("999.99"), expectedValue = "100-1k"),
            BandModel(amount = BigDecimal("1000"), expectedValue = "1k-10k"),
            BandModel(amount = BigDecimal("10000"), expectedValue = "1k-10k"),
            BandModel(amount = BigDecimal("10000.01"), expectedValue = ">10k"),
        )
    }

    data class EventModel(
        val event: MoneyAccountAnalyticsEvents,
        val expectedId: String,
        val expectedParams: Map<String, String> = emptyMap(),
    )

    data class MethodModel(val method: AddFundsMethod, val expectedValue: String)

    data class BandModel(val amount: BigDecimal, val expectedValue: String)
}