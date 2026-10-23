package com.tangem.features.send.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/** Builds the sheet of an intent that cannot be reached: "Try again" when reading it again may help, else "Got it". */
internal class SendIntentCantReachConverter @Inject constructor() :
    Converter<SendIntentCantReachConverter.Input, SendIntentUM.Status> {

    data class Input(val onRetry: (() -> Unit)?, val onClose: () -> Unit)

    override fun convert(value: Input): SendIntentUM.Status = SendIntentUM.Status(
        variant = TangemModalStatus.Variant.Warning,
        title = Texts.TITLE,
        subtitle = null,
        primaryButton = value.onRetry
            ?.let { onRetry -> SendIntentButtonUM(text = Texts.TRY_AGAIN, onClick = onRetry) }
            ?: SendIntentButtonUM(text = Texts.GOT_IT, onClick = value.onClose),
    )

    // TODO: [REDACTED_TASK_KEY] Replace English placeholders with Lokalise-backed resources.
    private object Texts {
        val TITLE = stringReference("Can’t reach intent")
        val GOT_IT = stringReference("Got it")
        val TRY_AGAIN = stringReference("Try again")
    }
}