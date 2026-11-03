package com.tangem.domain.moneyaccount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import java.math.BigDecimal

sealed class MoneyAccountAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    data class AccountScreenOpened(
        val accountState: AccountState,
        val balanceBand: BalanceBand?,
        val currency: String?,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Account Screen Opened",
        params = buildMap {
            put("account_state", accountState.value)
            balanceBand?.let { put("balance_band", it.value) }
            put("yield_enabled", "no")
            currency?.let { put("currency", it) }
        },
    )

    class AddFundsClicked : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Button - Add Funds",
    )

    class SendClicked : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Button - Send",
    )

    class AccountMenuClicked : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Button - Account Menu",
    )

    class SupportClicked : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Button - Support",
    )

    class BalanceErrorBannerShown : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Balance Error Banner Shown",
    )

    class AddFundsScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Add Funds Screen Opened",
    )

    data class AddFundsMethodSelected(val method: AddFundsMethod) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Add Funds Method Selected",
        params = mapOf("method" to method.value),
    )

    class AddFundsBankDetailsShown : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Add Funds Bank Details Shown",
    )

    class AccountDetailsScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Account Details Screen Opened",
        params = mapOf("route_state" to "active"),
    )

    data class DetailsFieldCopied(val field: String) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Field Copied",
        params = mapOf("field" to field),
    )

    class DetailsShared : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Shared",
    )

    class DetailsLoadFailed : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Load Failed",
    )

    class DetailsRetryTapped : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Retry Tapped",
    )

    class DetailsErrorSupportTapped : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Error Support Tapped",
    )

    data class WithdrawOptionSelected(val method: WithdrawMethod) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Withdraw Option Selected",
        params = mapOf("method" to method.value),
    )

    class OfframpRecipientListOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient List Opened",
    )

    class OfframpRecipientSelected : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Selected",
    )

    class OfframpAmountScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Amount Screen Opened",
    )

    class OfframpConfirmationScreenOpened : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Confirmation Screen Opened",
    )

    class OfframpRecipientTransferConfirmed : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Transfer Confirmed",
    )

    data class SendConfirmed(val amountUsdc: BigDecimal, val method: SendMethod) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send Confirmed",
        params = mapOf(
            "amount_usdc" to amountUsdc.toPlainString(),
            "method" to method.value,
        ),
    )

    data class SendFailed(val errorCode: Int?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send failed",
        params = buildMap { if (errorCode != null) put("error_code", errorCode.toString()) },
    )

    class SendSucceeded : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send succesfull",
    )

    enum class AccountState(val value: String) {
        ACTIVE("active"),
        CLOSED("closed"),
    }

    enum class BalanceBand(val value: String) {
        ZERO("0"),
        UNDER_100("<100"),
        FROM_100_TO_1K("100-1k"),
        FROM_1K_TO_10K("1k-10k"),
        OVER_10K(">10k"),
        ;

        companion object {
            private val HUNDRED = BigDecimal("100")
            private val THOUSAND = BigDecimal("1000")
            private val TEN_THOUSAND = BigDecimal("10000")

            fun of(amount: BigDecimal): BalanceBand = when {
                amount.signum() <= 0 -> ZERO
                amount < HUNDRED -> UNDER_100
                amount < THOUSAND -> FROM_100_TO_1K
                amount <= TEN_THOUSAND -> FROM_1K_TO_10K
                else -> OVER_10K
            }
        }
    }

    enum class AddFundsMethod(val value: String) {
        WALLET("wallet"),
        EXTERNAL_WALLET("external_wallet"),
        BANK_TRANSFER("bank_transfer"),
    }

    enum class WithdrawMethod(val value: String) {
        ON_CHAIN("on_chain"),
        BANK_TRANSFER("bank_transfer"),
    }

    enum class SendMethod(val value: String) {
        BANK_TRANSFER_ACH("bank_transfer_ach"),
        BANK_TRANSFER_WIRE("bank_transfer_wire"),
    }

    private companion object {
        const val CASH_ACCOUNT_CATEGORY = "Cash Account"
        const val TRANSFER_CATEGORY = "VA Transfer"
    }
}