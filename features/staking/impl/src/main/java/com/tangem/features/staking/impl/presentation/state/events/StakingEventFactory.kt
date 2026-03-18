package com.tangem.features.staking.impl.presentation.state.events

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.SendTransactionError

internal class StakingEventFactory(
    private val messageSender: UiMessageSender,
    private val popBackStack: () -> Unit,
    private val onFailedTxEmailClick: (String) -> Unit,
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory = TransactionErrorDialogFactory(),
) {

    fun createGenericErrorAlert(error: String) {
        messageSender.send(
            StakingAlertUM.genericError(
                onConfirmClick = { onFailedTxEmailClick(error) },
            ),
        )
    }

    fun createSendTransactionErrorAlert(error: SendTransactionError?) {
        val alert = error?.let {
            transactionErrorDialogFactory.create(
                error = error,
                popBackStack = popBackStack,
                onFailedTxEmailClick = onFailedTxEmailClick,
            )
        }
        alert?.let { messageSender.send(it) }
    }

    fun createStakingErrorAlert(error: StakingError) {
        messageSender.send(
            StakingAlertUM.stakingError(
                code = error.toString(),
                onConfirmClick = { onFailedTxEmailClick(error.toString()) },
            ),
        )
    }

    fun createStakingValidatorsUnavailableAlert() {
        messageSender.send(StakingAlertUM.validatorsUnavailable())
    }

    fun createStakingRewardsMinimumRequirementsErrorAlert(cryptoCurrencyName: String, cryptoAmountValue: String) {
        messageSender.send(
            StakingAlertUM.rewardsMinimumRequirementsError(
                cryptoCurrencyName = cryptoCurrencyName,
                cryptoAmountValue = cryptoAmountValue,
            ),
        )
    }

    fun createNetworkFeeUpdatedAlert(onConfirm: () -> Unit) {
        messageSender.send(
            StakingAlertUM.networkFeeUpdated(
                onConfirmClick = onConfirm,
            ),
        )
    }
}