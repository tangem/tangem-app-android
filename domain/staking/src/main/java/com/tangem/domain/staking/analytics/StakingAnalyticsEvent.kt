package com.tangem.domain.staking.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.EventValue
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent.ButtonRewards.addIfValueIsNotNull
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionType

sealed class StakingAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
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
            "Validators Count" to validatorsCount.asStringValue(),
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
            "Validator" to validator.asStringValue(),
            "Action" to action.asAnalyticName.asStringValue(),
        ),
    )

    data class StakeInProgressScreenOpened(
        val validator: String,
        val action: StakingActionType,
    ) : StakingAnalyticsEvent(
        event = "Stake In Progress Screen Opened",
        params = mapOf(
            "Validator" to validator.asStringValue(),
            "Action" to action.asAnalyticName.asStringValue(),
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
            AnalyticsParam.TYPE to if (isAppCurrency) "App Currency".asStringValue() else "Token".asStringValue(),
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
            AnalyticsParam.SOURCE to source.name.asStringValue(),
        ),
    )

    data class ValidatorChosen(
        val validator: String,
    ) : StakingAnalyticsEvent(
        event = "Validator Chosen",
        params = mapOf(
            "Validator" to validator.asStringValue(),
        ),
    )

    data class ButtonValidator(
        val source: StakeScreenSource,
    ) : StakingAnalyticsEvent(
        event = "Button - Validator",
        params = mapOf(
            AnalyticsParam.SOURCE to source.name.asStringValue(),
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
            "Validator" to validator.asStringValue(),
        ),
    )

    data object ButtonShare : StakingAnalyticsEvent(event = "Button - Share")

    data object ButtonExplore : StakingAnalyticsEvent(event = "Button - Explore")

    data class StakeKitApiError(
        val stakingError: StakingError.StakeKitApiError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_MESSAGE, stakingError.message?.asStringValue())
            addIfValueIsNotNull(AnalyticsParam.ERROR_CODE, stakingError.code?.asStringValue())
            addIfValueIsNotNull(AnalyticsParam.METHOD_NAME, stakingError.methodName?.asStringValue())
        },
    )

    data class StakeKitApiUnknownError(
        val stakeKitUnknownError: StakingError.StakeKitUnknownError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_DESCRIPTION, stakeKitUnknownError.jsonString?.asStringValue())
        },
    )

    data class DomainError(
        val stakeKitDomainError: StakingError.DomainError,
    ) : StakingAnalyticsEvent(
        event = "App Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_DESCRIPTION, stakeKitDomainError.message?.asStringValue())
        },
    )

    fun MutableMap<String, EventValue>.addIfValueIsNotNull(key: String, value: EventValue?) {
        if (value != null) {
            put(key, value)
        }
    }

    data object TransactionError : StakingAnalyticsEvent(
        event = "Error - Transaction Rejected",
    )
}

enum class StakeScreenSource {
    Info, Amount, Confirmation, Validators,
}