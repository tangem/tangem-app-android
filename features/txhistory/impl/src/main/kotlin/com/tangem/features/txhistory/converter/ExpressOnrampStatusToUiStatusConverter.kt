package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.utils.converter.Converter

/**
 * Collapses the typed onramp status into a UI [Status] bucket: the single success state
 * ([Finished][ExpressOnrampStatus.Finished]) → [Confirmed][Status.Confirmed]; the failure terminals
 * ([Failed][ExpressOnrampStatus.Failed]/[Expired][ExpressOnrampStatus.Expired]/[Refunded][ExpressOnrampStatus.Refunded])
 * and the terminal client fallback ([Unknown][ExpressOnrampStatus.Unknown]) → [Failed][Status.Failed]; everything in
 * flight — including a running refund ([RefundInProgress][ExpressOnrampStatus.RefundInProgress]) — → [Unconfirmed][Status.Unconfirmed].
 */
internal class ExpressOnrampStatusToUiStatusConverter : Converter<ExpressOnrampStatus, Status> {

    override fun convert(value: ExpressOnrampStatus): Status = when (value) {
        ExpressOnrampStatus.Finished -> Status.Confirmed
        ExpressOnrampStatus.Failed,
        ExpressOnrampStatus.Expired,
        ExpressOnrampStatus.Refunded,
        ExpressOnrampStatus.Unknown,
        -> Status.Failed
        ExpressOnrampStatus.Created,
        ExpressOnrampStatus.WaitingForPayment,
        ExpressOnrampStatus.PaymentProcessing,
        ExpressOnrampStatus.Verifying,
        ExpressOnrampStatus.Paid,
        ExpressOnrampStatus.Sending,
        ExpressOnrampStatus.Paused,
        ExpressOnrampStatus.RefundInProgress,
        -> Status.Unconfirmed
    }
}