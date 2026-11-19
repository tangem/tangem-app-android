package com.tangem.domain.moneyaccount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.NoticeState
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.domain.tangempay.model.bankofframp.BankRail
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
        val additionalVerificationNotice: NoticeState,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Account Screen Opened",
        params = buildMap {
            put("account_state", accountState.value)
            balanceBand?.let { put("balance_band", it.value) }
            put("yield_enabled", "no")
            currency?.let { put("currency", it) }
            put("additional_verification_notice", additionalVerificationNotice.value)
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

    data class AddFundsMethodSelected(
        val method: AddFundsMethod,
        val currency: DepositCurrency? = null,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Add Funds Method Selected",
        params = buildMap {
            put("method", method.value)
            if (method == AddFundsMethod.BANK_TRANSFER) currency?.let { put("currency", it.value) }
        },
    )

    data class AddFundsBankDetailsShown(val currency: DepositCurrency?) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Add Funds Bank Details Shown",
        params = currencyParams(currency),
    )

    data class AccountDetailsScreenOpened(
        val currency: DepositCurrency?,
        val rail: DepositRail?,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Account Details Screen Opened",
        params = buildMap {
            put("route_state", "active")
            currency?.let { put("currency", it.value) }
            rail?.let { put("rail", it.value) }
        },
    )

    data class DetailsFieldCopied(val field: String, val currency: DepositCurrency?) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Field Copied",
        params = mapOf("field" to field) + currencyParams(currency),
    )

    data class DetailsShared(val currency: DepositCurrency?) : MoneyAccountAnalyticsEvents(
        categoryName = CASH_ACCOUNT_CATEGORY,
        event = "Details Shared",
        params = currencyParams(currency),
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

    data class OfframpRecipientSelected(val rail: BankRail?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Selected",
        params = railParams(rail),
    )

    data class OfframpAmountScreenOpened(val rail: BankRail?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Amount Screen Opened",
        params = railParams(rail),
    )

    data class OfframpConfirmationScreenOpened(val rail: BankRail?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Confirmation Screen Opened",
        params = railParams(rail),
    )

    data class OfframpRecipientTransferConfirmed(val rail: BankRail?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Transfer Confirmed",
        params = railParams(rail),
    )

    data class OfframpRecipientFormOpened(
        val rail: BankRail,
        val recipientType: RecipientType,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Form Opened",
        params = railParams(rail) + ("recipient_type" to recipientType.value),
    )

    data class OfframpRecipientSaved(
        val rail: BankRail,
        val recipientType: RecipientType,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Saved",
        params = railParams(rail) + ("recipient_type" to recipientType.value),
    )

    data class OfframpRecipientSaveFailed(val rail: BankRail, val errorCode: Int?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Recipient Save Failed",
        params = railParams(rail) + errorCodeParams(errorCode),
    )

    data class OfframpQuoteShown(val rail: BankRail, val amountBand: AmountBand) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Quote Shown",
        params = railParams(rail) + ("amount_band" to amountBand.value),
    )

    data class OfframpAmountErrorShown(
        val rail: BankRail,
        val reason: AmountErrorReason,
    ) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Offramp Amount Error Shown",
        params = railParams(rail) + ("reason" to reason.value),
    )

    data class SendConfirmed(val amountUsdc: BigDecimal, val method: SendMethod) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send Confirmed",
        params = mapOf(
            "amount_usdc" to amountUsdc.toPlainString(),
            "method" to method.value,
        ),
    )

    data class SendFailed(val errorCode: Int?, val method: SendMethod?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send failed",
        params = errorCodeParams(errorCode) + methodParams(method),
    )

    data class SendSucceeded(val method: SendMethod?) : MoneyAccountAnalyticsEvents(
        categoryName = TRANSFER_CATEGORY,
        event = "VA Send succesfull",
        params = methodParams(method),
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
        BANK_TRANSFER_SPEI("bank_transfer_spei"),
        ;

        companion object {
            fun of(rail: BankRail): SendMethod = when (rail) {
                BankRail.ACH -> BANK_TRANSFER_ACH
                BankRail.WIRE -> BANK_TRANSFER_WIRE
                BankRail.SPEI -> BANK_TRANSFER_SPEI
            }
        }
    }

    enum class DepositCurrency(val value: String) {
        USD("usd"),
        MXN("mxn"),
        ;

        companion object {
            fun of(credentials: BankCredentials): DepositCurrency = if (credentials.isSpei) MXN else USD

            fun singleOf(credentials: List<BankCredentials>): DepositCurrency? =
                credentials.map(::of).distinct().singleOrNull()
        }
    }

    enum class DepositRail(val value: String) {
        ACH_WIRE("ach_wire"),
        SPEI("spei"),
        ;

        companion object {
            fun of(credentials: BankCredentials): DepositRail = if (credentials.isSpei) SPEI else ACH_WIRE
        }
    }

    enum class RecipientType(val value: String) {
        OWN("own"),
        SOMEONE_ELSE("someone_else"),
        BUSINESS("business"),
    }

    enum class AmountErrorReason(val value: String) {
        BELOW_MINIMUM("below_minimum"),
        ABOVE_BALANCE("above_balance"),
        QUOTE_UNAVAILABLE("quote_unavailable"),
        QUOTE_EXPIRED("quote_expired"),
    }

    enum class AmountBand(val value: String) {
        UNDER_10("<10"),
        FROM_10_TO_100("10-100"),
        FROM_100_TO_1K("100-1k"),
        OVER_1K(">1k"),
        ;

        companion object {
            private val TEN = BigDecimal("10")
            private val HUNDRED = BigDecimal("100")
            private val THOUSAND = BigDecimal("1000")

            fun of(amount: BigDecimal): AmountBand = when {
                amount < TEN -> UNDER_10
                amount < HUNDRED -> FROM_10_TO_100
                amount <= THOUSAND -> FROM_100_TO_1K
                else -> OVER_1K
            }
        }
    }

    private companion object {
        const val CASH_ACCOUNT_CATEGORY = "Cash Account"
        const val TRANSFER_CATEGORY = "VA Transfer"

        fun currencyParams(currency: DepositCurrency?): Map<String, String> =
            currency?.let { mapOf("currency" to it.value) }.orEmpty()

        fun railParams(rail: BankRail?): Map<String, String> = when (rail) {
            BankRail.ACH -> mapOf("rail" to "ach")
            BankRail.WIRE -> mapOf("rail" to "wire")
            BankRail.SPEI -> mapOf("rail" to "spei")
            null -> emptyMap()
        }

        fun methodParams(method: SendMethod?): Map<String, String> =
            method?.let { mapOf("method" to it.value) }.orEmpty()

        fun errorCodeParams(errorCode: Int?): Map<String, String> =
            errorCode?.let { mapOf("error_code" to it.toString()) }.orEmpty()
    }
}