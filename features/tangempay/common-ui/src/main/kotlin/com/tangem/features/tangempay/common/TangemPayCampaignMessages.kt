package com.tangem.features.tangempay.common

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage

object TangemPayCampaignMessages {

    fun unavailableDialog(onDismiss: () -> Unit): DialogMessage {
        return DialogMessage(
            title = resourceReference(R.string.tangempay_campaign_unavailable_title),
            message = resourceReference(R.string.tangempay_campaign_unavailable_description),
            onDismissRequest = onDismiss,
            firstActionBuilder = { okAction(onClick = {}) },
        )
    }

    fun retryableErrorSnackbar(): SnackbarMessage {
        return SnackbarMessage(
            message = resourceReference(R.string.common_unknown_error),
            startIconId = R.drawable.ic_alert_circle_24,
        )
    }
}