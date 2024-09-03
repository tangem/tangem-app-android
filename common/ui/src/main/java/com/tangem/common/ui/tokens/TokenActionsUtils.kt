package com.tangem.common.ui.tokens

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

fun ScenarioUnavailabilityReason.getUnavailabilityReasonText(): TextReference {
    return when (val unavailabilityReason = this) {
        is ScenarioUnavailabilityReason.StakingUnavailable -> {
            resourceReference(
                id = R.string.token_button_unavailability_reason_staking_unavailable,
                formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
            )
        }
        is ScenarioUnavailabilityReason.PendingTransaction -> {
            when (unavailabilityReason.withdrawalScenario) {
                ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                    id = R.string.token_button_unavailability_reason_pending_transaction_send,
                    formatArgs = wrappedList(unavailabilityReason.networkName),
                )
                ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                    id = R.string.token_button_unavailability_reason_pending_transaction_sell,
                    formatArgs = wrappedList(unavailabilityReason.networkName),
                )
            }
        }
        is ScenarioUnavailabilityReason.EmptyBalance -> {
            when (unavailabilityReason.withdrawalScenario) {
                ScenarioUnavailabilityReason.WithdrawalScenario.SEND -> resourceReference(
                    id = R.string.token_button_unavailability_reason_empty_balance_send,
                )
                ScenarioUnavailabilityReason.WithdrawalScenario.SELL -> resourceReference(
                    id = R.string.token_button_unavailability_reason_empty_balance_sell,
                )
            }
        }
        is ScenarioUnavailabilityReason.BuyUnavailable -> {
            resourceReference(
                id = R.string.token_button_unavailability_reason_buy_unavailable,
                formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
            )
        }
        is ScenarioUnavailabilityReason.NotExchangeable -> {
            resourceReference(
                id = R.string.token_button_unavailability_reason_not_exchangeable,
                formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
            )
        }
        is ScenarioUnavailabilityReason.NotSupportedBySellService -> {
            resourceReference(
                id = R.string.token_button_unavailability_reason_sell_unavailable,
                formatArgs = wrappedList(unavailabilityReason.cryptoCurrencyName),
            )
        }
        ScenarioUnavailabilityReason.Unreachable -> {
            resourceReference(
                id = R.string.token_button_unavailability_generic_description,
            )
        }
        ScenarioUnavailabilityReason.UnassociatedAsset -> resourceReference(
            id = R.string.warning_receive_blocked_hedera_token_association_required_message,
        )
        ScenarioUnavailabilityReason.None -> {
            throw IllegalArgumentException("The unavailability reason must be other than None")
        }
    }
}