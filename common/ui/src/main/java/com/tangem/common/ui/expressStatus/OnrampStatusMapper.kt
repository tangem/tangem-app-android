package com.tangem.common.ui.expressStatus

import com.tangem.common.ui.R
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.onramp.model.OnrampStatus

fun OnrampStatus.Status.toActiveStatusText(currencyName: String): TextReference = when (this) {
    OnrampStatus.Status.Created,
    OnrampStatus.Status.WaitingForPayment,
    -> resourceReference(R.string.express_exchange_status_receiving_active)
    OnrampStatus.Status.PaymentProcessing -> resourceReference(R.string.express_exchange_status_confirming_active)
    OnrampStatus.Status.Verifying -> resourceReference(R.string.express_exchange_status_verifying)
    OnrampStatus.Status.Paid -> resourceReference(R.string.express_status_buying_active, wrappedList(currencyName))
    OnrampStatus.Status.Sending -> resourceReference(
        R.string.express_exchange_status_sending_active,
        wrappedList(currencyName),
    )
    OnrampStatus.Status.Finished -> resourceReference(R.string.express_status_bought, wrappedList(currencyName))
    OnrampStatus.Status.RefundInProgress -> resourceReference(R.string.express_exchange_status_refunding)
    OnrampStatus.Status.Refunded -> resourceReference(R.string.express_exchange_status_refunded)
    OnrampStatus.Status.Paused -> resourceReference(R.string.express_exchange_status_paused)
    OnrampStatus.Status.Expired,
    OnrampStatus.Status.Failed,
    -> resourceReference(R.string.express_exchange_status_failed)
}

fun OnrampStatus.Status.toIconState(): ExpressTransactionStateIconUM = when (this) {
    OnrampStatus.Status.Verifying,
    OnrampStatus.Status.RefundInProgress,
    -> ExpressTransactionStateIconUM.Warning
    OnrampStatus.Status.Refunded,
    OnrampStatus.Status.Failed,
    -> ExpressTransactionStateIconUM.Error
    else -> ExpressTransactionStateIconUM.None
}