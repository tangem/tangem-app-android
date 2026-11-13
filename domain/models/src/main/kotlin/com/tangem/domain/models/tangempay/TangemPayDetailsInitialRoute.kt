package com.tangem.domain.models.tangempay

import kotlinx.serialization.Serializable

/**
 * Which inner screen the Tangem Pay should open on when launched.
 *
 * [ACCOUNT_DETAILS] — the default account/main page.
 * [ADD_FUNDS] — the account/main page with the "Add funds" bottom sheet expanded, used by the top-up push.
 * [VA_ONRAMP], [VA_ONRAMP_DETAILS] — the bank transfer on-ramp: its banking details when the Virtual
 *                 Account is available, otherwise its onboarding.
 * [CURRENT_PLAN] — the details of the currently active tariff plan.
 * [CHANGE_PLAN] — the tariff-plan selection screen opened to change an already active plan.
 * [CASHBACK] — the cashback screen.
 * [ORDER_CARD] — the card order flow.
 */
@Serializable
enum class TangemPayDetailsInitialRoute {
    ACCOUNT_DETAILS,
    ADD_FUNDS,
    VA_ONRAMP,
    VA_ONRAMP_DETAILS,
    CURRENT_PLAN,
    CHANGE_PLAN,
    CASHBACK,
    ORDER_CARD,
}