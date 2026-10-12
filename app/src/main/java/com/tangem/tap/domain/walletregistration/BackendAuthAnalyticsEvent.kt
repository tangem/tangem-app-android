package com.tangem.tap.domain.walletregistration

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Backend-authentication analytics events (category "Backend Auth").
 */
internal sealed class BackendAuthAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Backend Auth", event = event, params = params) {

    class UnexpectedWalletCurve(cardId: String) : BackendAuthAnalyticsEvent(
        event = "Unexpected Wallet Curve",
        params = mapOf("Card Id" to cardId),
    )

    /**
     * Wallet binding never surfaces to the user, and failures that don't reach the server are invisible
     * on the backend — so this event is the only production signal of how often binding fails.
     */
    class WalletRegistrationFailed(walletType: WalletType, stage: Stage, reason: String) : BackendAuthAnalyticsEvent(
        event = "Wallet Registration Failed",
        params = mapOf(
            "Wallet Type" to walletType.value,
            "Stage" to stage.value,
            "Reason" to reason,
        ),
    ) {

        enum class WalletType(val value: String) {
            Mobile("Mobile"),
            Cold("Cold"),
        }

        enum class Stage(val value: String) {
            Register("Register"),
            Nonce("Nonce"),
            Prepare("Prepare"),
            Submit("Submit"),
        }
    }
}