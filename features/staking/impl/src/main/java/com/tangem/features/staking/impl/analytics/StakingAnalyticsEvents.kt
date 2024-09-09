package com.tangem.features.staking.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.model.stakekit.action.StakingActionType

internal sealed class StakingAnalyticsEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "Staking",
    event = event,
    params = params,
) {

    data class StakingInfoScreenOpened(
        val validatorsCount: Int,
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Staking Info Screen Opened",
        params = mapOf(
            "Validators Count" to validatorsCount.toString(),
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class WhatIsStaking(val token: String) : StakingAnalyticsEvents(
        event = "Link - What Is Staking",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class AmountScreenOpened(val token: String) : StakingAnalyticsEvents(
        event = "Amount Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ConfirmationScreenOpened(
        val token: String,
        val validator: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvents(
        event = "Confirmation Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
            "Action" to action.name,
        ),
    )

    data class StakeInProgressScreenOpened(
        val validator: String,
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Stake In Progress Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
        ),
    )

    data class RewardScreenOpened(val token: String) : StakingAnalyticsEvents(
        event = "Reward Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class AmountSelectCurrency(
        val token: String,
        val isAppCurrency: Boolean,
    ) : StakingAnalyticsEvents(
        event = "Selected Currency",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.TYPE to if (isAppCurrency) "App Currency" else "Token",
        ),
    )

    data class ButtonMax(val token: String) : StakingAnalyticsEvents(
        event = "Button - Max",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ButtonCancel(
        val source: StakeScreenSource,
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Button - Cancel",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data class ButtonValidator(
        val source: StakeScreenSource,
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Button - Validator",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data class ButtonRewards(
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Button - Rewards",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ButtonAction(
        val action: String,
        val token: String,
        val validator: String,
    ) : StakingAnalyticsEvents(
        event = "Button - $action",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
        ),
    )

    data object ButtonShare : StakingAnalyticsEvents(event = "Button - Share")

    data object ButtonExplore : StakingAnalyticsEvents(event = "Button - Explore")

    data class StakingError(
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Errors",
        params = mapOf(AnalyticsParam.TOKEN_PARAM to token),
    )

    data class TransactionError(
        val token: String,
    ) : StakingAnalyticsEvents(
        event = "Error - Transaction Rejected",
        params = mapOf(AnalyticsParam.TOKEN_PARAM to token),
    )
}

enum class StakeScreenSource {
    Info,
    Amount,
    Confirmation,
    Validators,
}
