package com.tangem.domain.staking.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionType

sealed class StakingAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "Staking",
    event = event,
    params = params,
) {

    data class StakingInfoScreenOpened(
        val validatorsCount: Int,
    ) : StakingAnalyticsEvent(
        event = "Staking Info Screen Opened",
        params = mapOf(
            "Validators Count" to validatorsCount.toString(),
        ),
    )

    data object WhatIsStaking : StakingAnalyticsEvent(
        event = "Link - What Is Staking",
    )

    data object AmountScreenOpened : StakingAnalyticsEvent(
        event = "Amount Screen Opened",
    )

    data class ConfirmationScreenOpened(
        val validator: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvent(
        event = "Confirmation Screen Opened",
        params = mapOf(
            "Validator" to validator,
            "Action" to action.asAnalyticName,
        ),
    )

    data class StakeInProgressScreenOpened(
        val validator: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvent(
        event = "Stake In Progress Screen Opened",
        params = mapOf(
            "Validator" to validator,
            "Action" to action.asAnalyticName,
        ),
    )

    data object RewardScreenOpened : StakingAnalyticsEvent(
        event = "Reward Screen Opened",
    )

    data class AmountSelectCurrency(
        val isAppCurrency: Boolean,
    ) : StakingAnalyticsEvent(
        event = "Selected Currency",
        params = mapOf(
            AnalyticsParam.TYPE to if (isAppCurrency) "App Currency" else "Token",
        ),
    )

    data object ButtonMax : StakingAnalyticsEvent(
        event = "Button - Max",
    )

    data class ButtonCancel(
        val source: StakeScreenSource,
    ) : StakingAnalyticsEvent(
        event = "Button - Cancel",
        params = mapOf(
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data class ValidatorChosen(
        val validator: String,
    ) : StakingAnalyticsEvent(
        event = "Validator Chosen",
        params = mapOf(
            "Validator" to validator,
        ),
    )

    data class ButtonValidator(
        val source: StakeScreenSource,
    ) : StakingAnalyticsEvent(
        event = "Button - Validator",
        params = mapOf(
            AnalyticsParam.SOURCE to source.name,
        ),
    )

    data object ButtonRewards : StakingAnalyticsEvent(
        event = "Button - Rewards",
    )

    data class ButtonAction(
        val action: StakingActionType,
        val validator: String,
    ) : StakingAnalyticsEvent(
        event = "Button - ${action.asAnalyticName}",
        params = mapOf(
            "Validator" to validator,
        ),
    )

    data object ButtonShare : StakingAnalyticsEvent(event = "Button - Share")

    data object ButtonExplore : StakingAnalyticsEvent(event = "Button - Explore")

    data class StakeKitError(
        val stakingError: StakingError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = mapOf(
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

    data object TransactionError : StakingAnalyticsEvent(
        event = "Error - Transaction Rejected",
    )
}

enum class StakeScreenSource {
    Info,
    Amount,
    Confirmation,
    Validators,
}