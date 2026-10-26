package com.tangem.features.send.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.impl.R
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/** Builds the sheet of an intent that cannot be reached: "Try again" when reading it again may help, else "Got it". */
internal class SendIntentCantReachConverter @Inject constructor() :
    Converter<SendIntentCantReachConverter.Input, SendIntentUM.Status> {

    data class Input(val onRetry: (() -> Unit)?, val onClose: () -> Unit)

    override fun convert(value: Input): SendIntentUM.Status = SendIntentUM.Status(
        variant = TangemModalStatus.Variant.Warning,
        title = resourceReference(R.string.mcp_send_intent_cant_reach_title),
        subtitle = null,
        primaryButton = value.onRetry
            ?.let { onRetry ->
                SendIntentButtonUM(text = resourceReference(R.string.alert_button_try_again), onClick = onRetry)
            }
            ?: SendIntentButtonUM(text = resourceReference(R.string.common_got_it), onClick = value.onClose),
    )
}