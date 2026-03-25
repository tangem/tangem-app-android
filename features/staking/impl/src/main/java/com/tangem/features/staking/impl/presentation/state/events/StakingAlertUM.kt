package com.tangem.features.staking.impl.presentation.state.events

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.staking.impl.R

internal object StakingAlertUM {

    fun genericError(onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_error),
        message = resourceReference(R.string.common_unknown_error),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_support),
            onClick = onConfirmClick,
        ),
    )

    fun stakingError(code: String, onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_error),
        message = resourceReference(R.string.generic_error_code, wrappedList(code)),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_support),
            onClick = onConfirmClick,
        ),
    )

    fun noAvailableValidators(): DialogMessage = DialogMessage(
        title = resourceReference(R.string.common_error),
        message = resourceReference(R.string.staking_no_validators_error_message),
    )

    fun feeIncreased(onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = null,
        message = resourceReference(id = R.string.send_notification_high_fee_title),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_ok),
            onClick = onConfirmClick,
        ),
    )

    fun validatorsUnavailable(): DialogMessage = DialogMessage(
        title = resourceReference(id = R.string.staking_error_no_validators_title),
        message = resourceReference(id = R.string.staking_error_no_validators_message),
    )

    fun stakeMoreClickUnavailable(cryptoCurrency: CryptoCurrency): DialogMessage = DialogMessage(
        title = null,
        message = resourceReference(
            id = R.string.staking_stake_more_button_unavailability_reason,
            wrappedList(cryptoCurrency.name, cryptoCurrency.symbol),
        ),
    )

    fun rewardsMinimumRequirementsError(cryptoCurrencyName: String, cryptoAmountValue: String): DialogMessage =
        DialogMessage(
            title = null,
            message = resourceReference(
                id = R.string.staking_details_min_rewards_notification,
                formatArgs = wrappedList(cryptoCurrencyName, cryptoAmountValue),
            ),
        )

    fun networkFeeUpdated(onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(R.string.staking_alert_network_fee_updated_title),
        message = resourceReference(R.string.staking_alert_network_fee_updated_message),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_ok),
            onClick = onConfirmClick,
        ),
    )
}