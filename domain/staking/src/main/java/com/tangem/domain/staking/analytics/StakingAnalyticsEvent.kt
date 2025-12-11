package com.tangem.domain.staking.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.models.staking.action.StakingActionType

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

    class WhatIsStaking : StakingAnalyticsEvent(
        event = "Link - What Is Staking",
    )

    class AmountScreenOpened : StakingAnalyticsEvent(
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

    class RewardScreenOpened : StakingAnalyticsEvent(
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

    class ButtonMax : StakingAnalyticsEvent(
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

    class ButtonRewards : StakingAnalyticsEvent(
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

    class ButtonShare : StakingAnalyticsEvent(event = "Button - Share")

    class ButtonExplore : StakingAnalyticsEvent(event = "Button - Explore")

    data class StakeKitApiError(
        val stakingError: StakingError.StakeKitApiError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_MESSAGE, stakingError.message)
            addIfValueIsNotNull(AnalyticsParam.ERROR_CODE, stakingError.code)
            addIfValueIsNotNull(AnalyticsParam.METHOD_NAME, stakingError.methodName)
        },
    )

    data class StakeKitApiUnknownError(
        val stakeKitUnknownError: StakingError.StakeKitUnknownError,
    ) : StakingAnalyticsEvent(
        event = "Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_DESCRIPTION, stakeKitUnknownError.jsonString)
        },
    )

    data class DomainError(
        val stakeKitDomainError: StakingError.DomainError,
    ) : StakingAnalyticsEvent(
        event = "App Errors",
        params = buildMap {
            addIfValueIsNotNull(AnalyticsParam.ERROR_DESCRIPTION, stakeKitDomainError.message)
        },
    )

    data class TransactionError(
        val errorCode: String,
    ) : StakingAnalyticsEvent(
        event = "Error - Transaction Rejected",
        params = mapOf(
            AnalyticsParam.ERROR_CODE to errorCode,
        ),
    )

    data class UninitializedAddress(val token: String) : StakingAnalyticsEvent(
        event = "Notice - Uninitialized Address",
        params = mapOf(AnalyticsParam.TOKEN_PARAM to token),
    )

    data class UninitializedAddressScreen(val token: String) : StakingAnalyticsEvent(
        event = "Uninitialized Address Screen",
        params = mapOf(AnalyticsParam.TOKEN_PARAM to token),
    )

    data class ButtonActivate(val token: String) : StakingAnalyticsEvent(
        event = "Button - Activate",
        params = mapOf(AnalyticsParam.TOKEN_PARAM to token),
    )
}

enum class StakeScreenSource {
    Info, Amount, Confirmation, Validators,
}

@Suppress("CanBeNonNullable")
fun MutableMap<String, String>.addIfValueIsNotNull(key: String, value: Any?) {
    if (value != null) {
        put(key, value.toString())
    }
}