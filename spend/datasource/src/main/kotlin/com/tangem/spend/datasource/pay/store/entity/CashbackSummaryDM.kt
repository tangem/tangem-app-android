package com.tangem.spend.datasource.pay.store.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.serialization.SerializedDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cached cashback summary of a Tangem Pay customer. Holds only the states shown before the network answers: a summary
 * the backend reports as disabled or unrecognized is removed from the cache instead of being stored.
 */
@Serializable
sealed class CashbackSummaryDM {

    @Serializable
    @SerialName("enabled")
    data class Enabled(
        @SerialName("display_mode") val displayMode: DisplayMode,
        @SerialName("confirmed_amount") val confirmedAmount: SerializedBigDecimal,
        @SerialName("total_earned_amount") val totalEarnedAmount: SerializedBigDecimal? = null,
        @SerialName("currency") val currency: String,
        @SerialName("payout_currency") val payoutCurrency: String? = null,
        @SerialName("period") val period: Period,
        @SerialName("previous_payout") val previousPayout: PreviousPayout? = null,
    ) : CashbackSummaryDM()

    @Serializable
    @SerialName("deactivated")
    data object Deactivated : CashbackSummaryDM()

    @Serializable
    enum class DisplayMode {
        @SerialName("full")
        FULL,

        @SerialName("alt_block")
        ALT_BLOCK,
    }

    @Serializable
    data class Period(
        @SerialName("year") val year: Int,
        @SerialName("month") val month: Int,
        @SerialName("payout_start") val payoutStart: SerializedDateTime? = null,
        @SerialName("payout_end") val payoutEnd: SerializedDateTime? = null,
    )

    @Serializable
    data class PreviousPayout(
        @SerialName("end_date") val endDate: SerializedDateTime,
        @SerialName("amount") val amount: SerializedBigDecimal,
    )
}