package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.features.txhistory.impl.R

internal fun FinishedJointOperation.Status.toUiStatus(): Status = when (this) {
    FinishedJointOperation.Status.EXECUTED -> Status.Confirmed
    FinishedJointOperation.Status.FAILED,
    FinishedJointOperation.Status.CANCELLED,
    -> Status.Failed
}

internal fun FinishedJointOperation.Status.toTitle(): TextReference = when (this) {
    FinishedJointOperation.Status.EXECUTED -> resourceReference(R.string.common_sent)
    FinishedJointOperation.Status.FAILED -> resourceReference(R.string.transaction_history_status_send_failed)
    FinishedJointOperation.Status.CANCELLED -> resourceReference(R.string.express_exchange_status_canceled)
}