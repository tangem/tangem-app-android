package com.tangem.features.staking.impl.presentation.state.events

import com.tangem.common.ui.alerts.TransactionErrorAlertConverter
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.staking.impl.presentation.state.StakingStateController

internal class StakingEventFactory(
    private val stateController: StakingStateController,
    private val popBackStack: () -> Unit,
    private val onFailedTxEmailClick: (String) -> Unit,
) {

    fun createGenericErrorAlert(error: String) {
        val alert = StakingEvent.ShowAlert(
            StakingAlertUM.GenericError(
                onConfirmClick = { onFailedTxEmailClick(error) },
            ),
        )
        stateController.updateEvent(alert)
    }

    fun createSendTransactionErrorAlert(error: SendTransactionError?) {
        val alert = error?.let {
            TransactionErrorAlertConverter(
                popBackStack = popBackStack,
                onFailedTxEmailClick = onFailedTxEmailClick,
            ).convert(error)
        }?.let {
            StakingEvent.ShowAlert(it)
        }
        stateController.updateEvent(alert)
    }

    fun createStakingErrorAlert(error: StakingError) {
        val alert = StakingEvent.ShowAlert(
            StakingAlertUM.StakingError(
                code = error.toString(),
                onConfirmClick = { onFailedTxEmailClick(error.toString()) },
            ),
        )
        stateController.updateEvent(alert)
    }
}