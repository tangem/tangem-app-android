package com.tangem.features.staking.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionType

internal sealed class StakingAnalyticsEvent(
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
    ) : StakingAnalyticsEvent(
        event = "Staking Info Screen Opened",
        params = mapOf(
            "Validators Count" to validatorsCount.toString(),
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class WhatIsStaking(val token: String) : StakingAnalyticsEvent(
        event = "Link - What Is Staking",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class AmountScreenOpened(val token: String) : StakingAnalyticsEvent(
        event = "Amount Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ConfirmationScreenOpened(
        val token: String,
        val validator: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvent(
        event = "Confirmation Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
            "Action" to action.asAnalyticName,
        ),
    )

    data class StakeInProgressScreenOpened(
        val validator: String,
        val token: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvent(
        event = "Stake In Progress Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
            "Action" to action.asAnalyticName,
        ),
    )

    data class RewardScreenOpened(val token: String) : StakingAnalyticsEvent(
        event = "Reward Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class AmountSelectCurrency(
        val token: String,
        val isAppCurrency: Boolean,
    ) : StakingAnalyticsEvent(
        event = "Selected Currency",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.TYPE to if (isAppCurrency) "App Currency" else "Token",
        ),
    )

    data class ButtonMax(val token: String) : StakingAnalyticsEvent(
        event = "Button - Max",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ButtonCancel(
        val source: StakeScreenSource,
        val token: String,
    ) : StakingAnalyticsEvent(
        event = "Button - Cancel",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data class ValidatorChosen(
        val token: String,
        val validator: String,
    ) : StakingAnalyticsEvent(
        event = "Validator Chosen",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
        ),
    )

    data class ButtonValidator(
        val source: StakeScreenSource,
        val token: String,
    ) : StakingAnalyticsEvent(
        event = "Button - Validator",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data class ButtonRewards(
        val token: String,
    ) : StakingAnalyticsEvent(
        event = "Button - Rewards",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    data class ButtonAction(
        val action: StakingActionType,
        val token: String,
        val validator: String,
    ) : StakingAnalyticsEvent(
        event = "Button - ${action.asAnalyticName}",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            "Validator" to validator,
        ),
    )

    data object ButtonShare : StakingAnalyticsEvent(event = "Button - Share")

    data object ButtonExplore : StakingAnalyticsEvent(event = "Button - Explore")

    data class StakeKitError(
        val token: String,
        val stakingError: StakingError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            when (stakingError) {
                is StakingError.StakeKitUnknownError -> {
                    AnalyticsParam.ERROR_DESCRIPTION to (stakingError.jsonString ?: "Unknown")
                }
                is StakingError.StakeKitApiError -> {
                    AnalyticsParam.ERROR_MESSAGE to (stakingError.message ?: "Unknown")
                }
                is StakingError.UnknownError -> {
                    AnalyticsParam.ERROR_MESSAGE to (stakingError.message ?: "Unknown")
                }
            },
        ),
    )

    data class TransactionError(
        val token: String,
    ) : StakingAnalyticsEvent(
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
