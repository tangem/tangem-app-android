package com.tangem.domain.models.pay

import kotlinx.serialization.Serializable

/**
 * Which inner screen the Tangem Pay should open on when launched.
 *
 * [ACCOUNT_DETAILS] — the default account/main page.
 * [TIERS_ONBOARDING] — the tariff-plan selection screen, used by the "Select plan" entry point on the
 *                 wallet main screen (Tiers).
 */
@Serializable
enum class TangemPayDetailsInitialRoute {
    ACCOUNT_DETAILS,
    TIERS_ONBOARDING,
}