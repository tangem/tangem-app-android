package com.tangem.feature.wallet.deeplink.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class PromoBannerAnalytics(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "Promo Banner", event = event, params = params) {

    class BannerOpen(
        walletId: String?,
        promoCode: String,
    ) : PromoBannerAnalytics(
        event = "Banner Open",
        params = buildMap {
            walletId?.let { put(WALLET_ID, it) }
            put(PROMO_CODE, promoCode)
        },
    )

    class ActivationAttempt(
        walletId: String,
        promoCode: String,
        network: String,
        token: String,
    ) : PromoBannerAnalytics(
        event = "Activation Attempt",
        params = mapOf(
            WALLET_ID to walletId,
            PROMO_CODE to promoCode,
            NETWORK to network,
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to "banner",
        ),
    )

    class ActivationSuccess(
        walletId: String,
        promoCode: String,
        network: String,
        token: String,
        campaignId: String?,
        activationTime: String,
    ) : PromoBannerAnalytics(
        event = "Activation Success",
        params = buildMap {
            put(WALLET_ID, walletId)
            campaignId?.let { put(CAMPAIGN_ID, it) }
            put(PROMO_CODE, promoCode)
            put(NETWORK, network)
            put(AnalyticsParam.TOKEN_PARAM, token)
            put(ACTIVATION_TIME, activationTime)
        },
    )

    class ActivationFailed(
        walletId: String?,
        promoCode: String,
        network: String,
        token: String,
        error: PromoActivationError,
    ) : PromoBannerAnalytics(
        event = "Activation Failed",
        params = buildMap {
            walletId?.let { put(WALLET_ID, it) }
            put(PROMO_CODE, promoCode)
            put(NETWORK, network)
            put(AnalyticsParam.TOKEN_PARAM, token)
            put(AnalyticsParam.ERROR_CODE, error.analyticsCode)
        },
    )

    private companion object {
        const val WALLET_ID = "Wallet Id"
        const val CAMPAIGN_ID = "Campaign Id"
        const val PROMO_CODE = "Promo Code"
        const val NETWORK = "Network"
        const val ACTIVATION_TIME = "Activation Time"
    }
}

internal enum class PromoActivationError(val analyticsCode: String) {
    InvalidCode("invalid_code"),
    AlreadyUsed("already_used"),
    CampaignExpired("campaign_expired"),
    AddressMissing("address_missing"),
    InvalidAddress("invalid_address"),
    NetworkError("network_error"),
    InternalError("internal_error"),
}