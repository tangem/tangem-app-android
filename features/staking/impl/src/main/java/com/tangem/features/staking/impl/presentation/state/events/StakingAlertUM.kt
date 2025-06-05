package com.tangem.features.staking.impl.presentation.state.events

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.staking.impl.R

@Immutable
internal sealed class StakingAlertUM : AlertUM {

    data class GenericError(
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertUM() {
        override val title: TextReference = resourceReference(R.string.common_error)
        override val message: TextReference = resourceReference(R.string.common_unknown_error)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_support)
    }

    data class StakingError(
        val code: String,
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertUM() {
        override val title: TextReference = resourceReference(R.string.common_error)
        override val message: TextReference = resourceReference(R.string.generic_error_code, wrappedList(code))
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_support)
    }

    data object NoAvailableValidators : StakingAlertUM() {
        override val title = resourceReference(R.string.common_error)
        override val message = resourceReference(R.string.staking_no_validators_error_message)
        override val confirmButtonText = resourceReference(R.string.common_ok)
        override val onConfirmClick = null
    }

    data class FeeIncreased(
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertUM() {
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(id = R.string.send_notification_high_fee_title)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }

    data object ValidatorsUnavailable : StakingAlertUM() {
        override val onConfirmClick: (() -> Unit)? = null
        override val title: TextReference = resourceReference(id = R.string.staking_error_no_validators_title)
        override val message: TextReference = resourceReference(id = R.string.staking_error_no_validators_message)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }

    data class StakeMoreClickUnavailable(
        val cryptoCurrency: CryptoCurrency,
    ) : StakingAlertUM() {
        override val onConfirmClick: (() -> Unit)? = null
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(
            id = R.string.staking_stake_more_button_unavailability_reason,
            wrappedList(cryptoCurrency.name, cryptoCurrency.symbol),
        )
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }

    data object InitializeAccount : StakingAlertUM() {
        override val onConfirmClick: (() -> Unit)? = null
        override val title: TextReference = resourceReference(id = R.string.staking_error_no_validators_title)
        override val message: TextReference = resourceReference(id = R.string.staking_notification_ton_activate_account)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }

    data class RewardsMinimumRequirementsError(
        val cryptoCurrencyName: String,
        val cryptoAmountValue: String,
    ) : StakingAlertUM() {
        override val onConfirmClick: (() -> Unit)? = null
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(
            id = R.string.staking_details_min_rewards_notification,
            formatArgs = wrappedList(cryptoCurrencyName, cryptoAmountValue),
        )
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    }
}