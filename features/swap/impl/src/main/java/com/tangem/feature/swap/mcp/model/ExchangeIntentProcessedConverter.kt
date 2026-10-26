package com.tangem.feature.swap.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Builds the sheet of an intent that has already been processed. Without both currencies of the wallet the swap
 * cannot be named, so the sheet has no subtitle.
 */
internal class ExchangeIntentProcessedConverter @Inject constructor(
    private val swapTextConverter: ExchangeIntentSwapTextConverter,
) : Converter<ExchangeIntentProcessedConverter.Input, ExchangeIntentUM.Status> {

    data class Input(
        val intent: ExchangeIntent,
        val from: CryptoCurrency?,
        val to: CryptoCurrency?,
        val onClose: () -> Unit,
    )

    override fun convert(value: Input): ExchangeIntentUM.Status = ExchangeIntentUM.Status(
        variant = TangemModalStatus.Variant.Info,
        title = Texts.TITLE,
        subtitle = value.toSubtitle(),
        primaryButton = ExchangeIntentButtonUM(text = Texts.GOT_IT, onClick = value.onClose),
    )

    private fun Input.toSubtitle(): TextReference? {
        if (from == null || to == null) return null
        val swap = swapTextConverter.convert(
            ExchangeIntentSwapTextConverter.Input(intent = intent, from = from, to = to),
        )

        return swap?.let { stringReference("Swap ${it.from} to ${it.to}") }
    }

    // TODO: [REDACTED_TASK_KEY] Replace English placeholders with Lokalise-backed resources.
    private object Texts {
        val TITLE = stringReference("This intent has already been processed")
        val GOT_IT = stringReference("Got it")
    }
}