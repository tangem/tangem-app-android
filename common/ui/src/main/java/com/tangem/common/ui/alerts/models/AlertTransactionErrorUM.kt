package com.tangem.common.ui.alerts.models

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList

data class AlertTransactionErrorUM(
    val code: String,
    val cause: String?,
    val causeTextReference: TextReference? = null,
    override val onConfirmClick: () -> Unit,
) : AlertUM {
    override val title: TextReference = resourceReference(id = R.string.send_alert_transaction_failed_title)
    override val message: TextReference = resourceReference(
        id = R.string.send_alert_transaction_failed_text,
        formatArgs = wrappedList(causeTextReference ?: cause.orEmpty(), code),
    )
    override val confirmButtonText: TextReference =
        resourceReference(id = R.string.common_support)
}