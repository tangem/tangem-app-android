package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.utils.converter.Converter

/**
 * Collapses the typed swap status into a UI [Status] bucket: the single success state
 * ([Finished][ExpressExchangeStatus.Finished]), the failure/return states ([Failed][ExpressExchangeStatus.Failed]/
 * [TxFailed][ExpressExchangeStatus.TxFailed]/[Refunded][ExpressExchangeStatus.Refunded]/
 * [Expired][ExpressExchangeStatus.Expired]/[Unknown][ExpressExchangeStatus.Unknown]) → [Failed][Status.Failed],
 * everything in flight (incl. [Verifying][ExpressExchangeStatus.Verifying] and [Paused][ExpressExchangeStatus.Paused])
 * → [Unconfirmed][Status.Unconfirmed].
 */
internal class ExpressExchangeStatusToUiStatusConverter : Converter<ExpressExchangeStatus, Status> {

    override fun convert(value: ExpressExchangeStatus): Status = when (value) {
        ExpressExchangeStatus.Finished -> Status.Confirmed
        ExpressExchangeStatus.Failed,
        ExpressExchangeStatus.TxFailed,
        ExpressExchangeStatus.Refunded,
        ExpressExchangeStatus.Expired,
        ExpressExchangeStatus.Unknown,
        -> Status.Failed
        ExpressExchangeStatus.Preview,
        ExpressExchangeStatus.Created,
        ExpressExchangeStatus.ExchangeTxSent,
        ExpressExchangeStatus.Waiting,
        ExpressExchangeStatus.WaitingTxHash,
        ExpressExchangeStatus.Confirming,
        ExpressExchangeStatus.Exchanging,
        ExpressExchangeStatus.Sending,
        ExpressExchangeStatus.Verifying,
        ExpressExchangeStatus.Paused,
        -> Status.Unconfirmed
    }
}