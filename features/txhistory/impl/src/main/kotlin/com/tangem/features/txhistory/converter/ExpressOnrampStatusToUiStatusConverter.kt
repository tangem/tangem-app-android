package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.utils.converter.Converter

/**
 * Collapses the typed onramp status into a UI [Status] bucket: the single success state
 * ([Finished][ExpressOnrampStatus.Finished]) → [Confirmed][Status.Confirmed]; the failure terminals
 * ([Failed][ExpressOnrampStatus.Failed]/[Expired][ExpressOnrampStatus.Expired]/[Refunded][ExpressOnrampStatus.Refunded])
 * → [Failed][Status.Failed]; everything in flight — including a running refund
 * ([RefundInProgress][ExpressOnrampStatus.RefundInProgress]) — → [Unconfirmed][Status.Unconfirmed].
 *
 * [Unknown][ExpressOnrampStatus.Unknown] is the client fallback for a status string this build cannot parse — a legacy
 * or newly added value, not a failure. Reporting it as [Failed][Status.Failed] would strike the amount through and
 * title a healthy top-up "Top-up failed", so it buckets in flight instead, which is also what the detail screen's
 * hidden plaque implies.
 */
internal class ExpressOnrampStatusToUiStatusConverter : Converter<ExpressOnrampStatus, Status> {

    override fun convert(value: ExpressOnrampStatus): Status = when (value) {
        ExpressOnrampStatus.Finished -> Status.Confirmed
        ExpressOnrampStatus.Failed,
        ExpressOnrampStatus.Expired,
        ExpressOnrampStatus.Refunded,
        -> Status.Failed
        ExpressOnrampStatus.Created,
        ExpressOnrampStatus.WaitingForPayment,
        ExpressOnrampStatus.PaymentProcessing,
        ExpressOnrampStatus.Verifying,
        ExpressOnrampStatus.Paid,
        ExpressOnrampStatus.Sending,
        ExpressOnrampStatus.Paused,
        ExpressOnrampStatus.RefundInProgress,
        ExpressOnrampStatus.Unknown,
        -> Status.Unconfirmed
    }
}