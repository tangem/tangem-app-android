package com.tangem.domain.models.pay

import kotlinx.serialization.Serializable

/**
 * Which inner screen the Tangem Pay should open on when launched.
 *
 * [ACCOUNT_DETAILS] — the default account/main page.
 * [TIERS_ONBOARDING] — the tariff-plan selection screen, used by the "Select plan" entry point on the
 *                 wallet main screen (Tiers).
 * [ADD_FUNDS] — the account/main page with the "Add funds" bottom sheet expanded, used by the top-up push.
 */
@Serializable
enum class TangemPayDetailsInitialRoute {
    ACCOUNT_DETAILS,
    TIERS_ONBOARDING,
    ADD_FUNDS,
}