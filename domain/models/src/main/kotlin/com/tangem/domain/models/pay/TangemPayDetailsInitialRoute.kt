package com.tangem.domain.models.pay

import kotlinx.serialization.Serializable

/**
 * Which inner screen the Tangem Pay should open on when launched.
 *
 * [ACCOUNT_DETAILS] — the default account/main page.
 * [TIERS_ONBOARDING] — the tariff-plan selection screen, used by the "Select plan" entry point on the
 *                 wallet main screen (Tiers) and by the `select_plan` deeplink.
 * [ADD_FUNDS] — the account/main page with the "Add funds" bottom sheet expanded, used by the top-up push.
 * [VA_ONRAMP] — the Virtual Account on-ramp bottom sheet (conditions and fees).
 * [VA_ONRAMP_DETAILS] — the Virtual Account on-ramp banking details (bank requisites) bottom sheet.
 * [CURRENT_PLAN] — the details of the currently active tariff plan.
 * [CHANGE_PLAN] — the tariff-plan selection screen opened to change an already active plan.
 * [CASHBACK] — the cashback screen.
 * [ORDER_CARD] — the card order flow.
 */
@Serializable
enum class TangemPayDetailsInitialRoute {
    ACCOUNT_DETAILS,
    TIERS_ONBOARDING,
    ADD_FUNDS,
    VA_ONRAMP,
    VA_ONRAMP_DETAILS,
    CURRENT_PLAN,
    CHANGE_PLAN,
    CASHBACK,
    ORDER_CARD,
}