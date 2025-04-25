package com.tangem.features.send.v2.subcomponents.destination

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.send.v2.impl.R
import javax.inject.Inject

@ModelScoped
internal class SendDestinationAlertFactory @Inject constructor(
    val messageSender: UiMessageSender,
) {

    fun getGenericErrorState(onFailedTxEmailClick: () -> Unit) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                message = resourceReference(id = R.string.common_unknown_error),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_support),
                    onClick = onFailedTxEmailClick,
                ),
            ),
        )
    }
}