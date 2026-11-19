package com.tangem.domain.moneyaccount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AccountState
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AddFundsMethod
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.BalanceBand
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.DepositCurrency
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.DepositRail
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeState
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.tangempay.MockBankCredentials
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
                    additionalVerificationNotice = NoticeState.NOT_SHOWN,
                ),
                expectedId = "[Cash Account] Account Screen Opened",
                expectedParams = mapOf(
                    "account_state" to "active",
                    "balance_band" to "100-1k",
                    "yield_enabled" to "no",
                    "currency" to "USD",
                    "additional_verification_notice" to "not_shown",
                ),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountScreenOpened(
                    accountState = AccountState.CLOSED,
                    balanceBand = null,
                    currency = null,
                    additionalVerificationNotice = NoticeState.NOT_SHOWN,
                ),
                expectedId = "[Cash Account] Account Screen Opened",
                expectedParams = mapOf(
                    "account_state" to "closed",
                    "yield_enabled" to "no",
                    "additional_verification_notice" to "not_shown",
                ),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountScreenOpened(
                    accountState = AccountState.ACTIVE,
                    balanceBand = BalanceBand.FROM_100_TO_1K,
                    currency = "USD",
                    additionalVerificationNotice = NoticeState.SHOWN,
                ),
                expectedId = "[Cash Account] Account Screen Opened",
                expectedParams = mapOf(
                    "account_state" to "active",
                    "balance_band" to "100-1k",
                    "yield_enabled" to "no",
                    "currency" to "USD",
                    "additional_verification_notice" to "shown",
                ),
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
                event = MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown(currency = DepositCurrency.USD),
                expectedId = "[Cash Account] Add Funds Bank Details Shown",
                expectedParams = mapOf("currency" to "usd"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown(currency = DepositCurrency.MXN),
                expectedId = "[Cash Account] Add Funds Bank Details Shown",
                expectedParams = mapOf("currency" to "mxn"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown(currency = null),
                expectedId = "[Cash Account] Add Funds Bank Details Shown",
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened(
                    currency = DepositCurrency.USD,
                    rail = DepositRail.ACH_WIRE,
                ),
                expectedId = "[Cash Account] Account Details Screen Opened",
                expectedParams = mapOf("route_state" to "active", "currency" to "usd", "rail" to "ach_wire"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened(
                    currency = DepositCurrency.MXN,
                    rail = DepositRail.SPEI,
                ),
                expectedId = "[Cash Account] Account Details Screen Opened",
                expectedParams = mapOf("route_state" to "active", "currency" to "mxn", "rail" to "spei"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsFieldCopied(
                    field = "routing_number",
                    currency = DepositCurrency.USD,
                ),
                expectedId = "[Cash Account] Details Field Copied",
                expectedParams = mapOf("field" to "routing_number", "currency" to "usd"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsFieldCopied(field = "clabe", currency = DepositCurrency.MXN),
                expectedId = "[Cash Account] Details Field Copied",
                expectedParams = mapOf("field" to "clabe", "currency" to "mxn"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsShared(currency = DepositCurrency.MXN),
                expectedId = "[Cash Account] Details Shared",
                expectedParams = mapOf("currency" to "mxn"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.AccountDetailsScreenOpened(currency = null, rail = null),
                expectedId = "[Cash Account] Account Details Screen Opened",
                expectedParams = mapOf("route_state" to "active"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsFieldCopied(field = "routing_number", currency = null),
                expectedId = "[Cash Account] Details Field Copied",
                expectedParams = mapOf("field" to "routing_number"),
            ),
            EventModel(
                event = MoneyAccountAnalyticsEvents.DetailsShared(currency = null),
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
        fun `GIVEN add funds method WHEN event created THEN method and currency params match the spec`(
            model: MethodModel,
        ) {
            // Act
            val event = MoneyAccountAnalyticsEvents.AddFundsMethodSelected(
                method = model.method,
                currency = model.currency,
            )

            // Assert
            assertThat(event.id).isEqualTo("[Cash Account] Add Funds Method Selected")
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            MethodModel(
                method = AddFundsMethod.WALLET,
                currency = null,
                expectedParams = mapOf("method" to "wallet"),
            ),
            MethodModel(
                method = AddFundsMethod.EXTERNAL_WALLET,
                currency = null,
                expectedParams = mapOf("method" to "external_wallet"),
            ),
            MethodModel(
                method = AddFundsMethod.BANK_TRANSFER,
                currency = null,
                expectedParams = mapOf("method" to "bank_transfer"),
            ),
            MethodModel(
                method = AddFundsMethod.BANK_TRANSFER,
                currency = DepositCurrency.USD,
                expectedParams = mapOf("method" to "bank_transfer", "currency" to "usd"),
            ),
            MethodModel(
                method = AddFundsMethod.BANK_TRANSFER,
                currency = DepositCurrency.MXN,
                expectedParams = mapOf("method" to "bank_transfer", "currency" to "mxn"),
            ),
            MethodModel(
                method = AddFundsMethod.WALLET,
                currency = DepositCurrency.USD,
                expectedParams = mapOf("method" to "wallet"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DepositRouteOf {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN bank route WHEN currency and rail picked THEN values match the spec`(model: RouteModel) {
            // Act
            val currency = DepositCurrency.of(model.credentials)
            val rail = DepositRail.of(model.credentials)

            // Assert
            assertThat(currency.value to rail.value).isEqualTo(model.expectedCurrency to model.expectedRail)
        }

        @ParameterizedTest
        @MethodSource("provideRouteListModels")
        fun `GIVEN bank routes WHEN single currency picked THEN it is the one currency or none`(model: RouteListModel) {
            // Act
            val currency = DepositCurrency.singleOf(model.credentials)

            // Assert
            assertThat(currency?.value).isEqualTo(model.expectedCurrency)
        }

        private fun provideTestModels() = listOf(
            RouteModel(credentials = ACH, expectedCurrency = "usd", expectedRail = "ach_wire"),
            RouteModel(credentials = WIRE, expectedCurrency = "usd", expectedRail = "ach_wire"),
            RouteModel(credentials = SPEI, expectedCurrency = "mxn", expectedRail = "spei"),
        )

        private fun provideRouteListModels() = listOf(
            RouteListModel(credentials = listOf(ACH), expectedCurrency = "usd"),
            RouteListModel(credentials = listOf(ACH, WIRE), expectedCurrency = "usd"),
            RouteListModel(credentials = listOf(SPEI), expectedCurrency = "mxn"),
            RouteListModel(credentials = listOf(ACH, SPEI), expectedCurrency = null),
            RouteListModel(credentials = emptyList(), expectedCurrency = null),
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

    data class MethodModel(
        val method: AddFundsMethod,
        val currency: DepositCurrency?,
        val expectedParams: Map<String, String>,
    )

    data class RouteModel(val credentials: BankCredentials, val expectedCurrency: String, val expectedRail: String)

    data class RouteListModel(val credentials: List<BankCredentials>, val expectedCurrency: String?)

    private companion object {
        val ACH = MockBankCredentials.bankCredentials(rail = "ACH")
        val WIRE = MockBankCredentials.bankCredentials(rail = "WIRE")
        val SPEI = MockBankCredentials.speiBankCredentials()
    }

    data class BandModel(val amount: BigDecimal, val expectedValue: String)
}