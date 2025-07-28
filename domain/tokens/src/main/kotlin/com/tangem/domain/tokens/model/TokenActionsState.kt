package com.tangem.domain.tokens.model

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.models.wallet.UserWalletId

data class TokenActionsState(
    val walletId: UserWalletId,
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
    val states: List<ActionState>,
) {

    sealed class ActionState {

        abstract val unavailabilityReason: ScenarioUnavailabilityReason

        data class Buy(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class CopyAddress(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class Sell(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class Receive(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class Stake(
            override val unavailabilityReason: ScenarioUnavailabilityReason,
            val yield: Yield?,
        ) : ActionState()

        data class Swap(
            override val unavailabilityReason: ScenarioUnavailabilityReason,
            val showBadge: Boolean,
        ) : ActionState()

        data class Send(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class Analytics(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()

        data class HideToken(override val unavailabilityReason: ScenarioUnavailabilityReason) : ActionState()
    }
}