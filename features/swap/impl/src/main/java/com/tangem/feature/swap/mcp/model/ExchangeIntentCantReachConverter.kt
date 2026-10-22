package com.tangem.feature.swap.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/** Builds the sheet of an intent that cannot be reached: "Try again" when reading it again may help, else "Got it". */
internal class ExchangeIntentCantReachConverter @Inject constructor() :
    Converter<ExchangeIntentCantReachConverter.Input, ExchangeIntentUM.Status> {

    data class Input(val onRetry: (() -> Unit)?, val onClose: () -> Unit)

    override fun convert(value: Input): ExchangeIntentUM.Status = ExchangeIntentUM.Status(
        variant = TangemModalStatus.Variant.Warning,
        title = Texts.TITLE,
        subtitle = null,
        primaryButton = value.onRetry
            ?.let { onRetry -> ExchangeIntentButtonUM(text = Texts.TRY_AGAIN, onClick = onRetry) }
            ?: ExchangeIntentButtonUM(text = Texts.GOT_IT, onClick = value.onClose),
    )

    // TODO: [REDACTED_TASK_KEY] Replace English placeholders with Lokalise-backed resources.
    private object Texts {
        val TITLE = stringReference("Can’t reach intent")
        val GOT_IT = stringReference("Got it")
        val TRY_AGAIN = stringReference("Try again")
    }
}