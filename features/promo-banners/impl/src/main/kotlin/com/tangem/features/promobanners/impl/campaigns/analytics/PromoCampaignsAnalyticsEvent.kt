package com.tangem.features.promobanners.impl.campaigns.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.CAMPAIGN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.REASON
import com.tangem.core.analytics.models.AnalyticsParam.Key.SCREEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.WALLETS_COUNT
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType

/**
 * Hardcoded [CampaignType]s keep their legacy values; a backend [PromoCampaign] is identified by its [PromoCampaign.name],
 * which, unlike the id, is the same on every environment.
 */
internal sealed class PromoCampaignsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {

    class PromotionScreenOpened private constructor(
        params: Map<String, String>,
    ) : PromoCampaignsAnalyticsEvent(event = "Promotion Screen Opened", params = params) {

        constructor(campaignType: CampaignType) : this(mapOf(SCREEN to campaignType.analyticsName))

        constructor(campaign: PromoCampaign) : this(
            mapOf(
                SCREEN to campaign.name,
                MATCH_MODE to campaign.matchMode.analyticsName,
                PAYOUT to if (campaign.payout.isNeeded) "Yes" else "No",
            ),
        )
    }

    class EnrollButtonClicked private constructor(
        params: Map<String, String>,
    ) : PromoCampaignsAnalyticsEvent(event = "Enroll Button Clicked", params = params) {

        constructor(campaignType: CampaignType, token: String, blockchain: String) : this(
            mapOf(
                CAMPAIGN to campaignType.analyticsName,
                TOKEN_PARAM to token,
                BLOCKCHAIN to blockchain,
            ),
        )

        constructor(campaign: PromoCampaign, payoutToken: PayoutToken?, walletsCount: Int) : this(
            buildMap {
                put(CAMPAIGN, campaign.name)
                putPayoutToken(payoutToken)
                put(MATCH_MODE, campaign.matchMode.analyticsName)
                put(WALLETS_COUNT, walletsCount.toString())
            },
        )
    }

    class AlreadyEnrolledScreenOpened(campaign: String, source: Source) : PromoCampaignsAnalyticsEvent(
        event = "Already Enrolled Screen Opened",
        params = mapOf(
            CAMPAIGN to campaign,
            SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            /** The compact sheet shown when a banner or link leads to a campaign the wallet is already in. */
            Entry("Entry"),
            EnrollResponse("Enroll Response"),
        }
    }

    class EnrollSuccessful(
        campaign: PromoCampaign,
        payoutToken: PayoutToken?,
        walletsEnrolled: Int,
        walletsTotal: Int,
    ) : PromoCampaignsAnalyticsEvent(
        event = "Enroll Successful",
        params = buildMap {
            put(CAMPAIGN, campaign.name)
            put(MATCH_MODE, campaign.matchMode.analyticsName)
            putPayoutToken(payoutToken)
            put(WALLETS_ENROLLED, walletsEnrolled.toString())
            put(WALLETS_TOTAL, walletsTotal.toString())
        },
    )

    /** [errorCode] is the HTTP status; `null` when there was no HTTP error response, e.g. a network failure. */
    class EnrollError(campaign: PromoCampaign, reason: Reason, errorCode: Int?) : PromoCampaignsAnalyticsEvent(
        event = "Enroll Error",
        params = buildMap {
            put(CAMPAIGN, campaign.name)
            put(REASON, reason.value)
            errorCode?.let { put(ERROR_CODE, it.toString()) }
        },
    ) {
        enum class Reason(val value: String) {
            Unavailable("Unavailable"),
            NotActiveYet("Not Active Yet"),
            Technical("Technical"),
        }
    }

    /** The payout token the user chose; the wallet and the address never reach analytics. */
    data class PayoutToken(val symbol: String, val blockchain: String)

    private companion object {
        const val MATCH_MODE = "Match Mode"
        const val PAYOUT = "Payout"
        const val WALLETS_ENROLLED = "Wallets Enrolled"
        const val WALLETS_TOTAL = "Wallets Total"

        fun MutableMap<String, String>.putPayoutToken(payoutToken: PayoutToken?) {
            payoutToken ?: return
            put(TOKEN_PARAM, payoutToken.symbol)
            put(BLOCKCHAIN, payoutToken.blockchain)
        }
    }
}

internal val CampaignType.analyticsName: String
    get() = when (this) {
        is CampaignType.WhaleSwapCashback -> "Cashback"
        is CampaignType.ReactivationCashback -> "Reactivation"
    }

private val PromoMatchMode.analyticsName: String
    get() = when (this) {
        PromoMatchMode.Strict -> "Strict"
        PromoMatchMode.LinkedWallet -> "Linked Wallet"
    }