package com.tangem.feature.swap.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.feature.swap.presentation.R
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
        title = resourceReference(R.string.mcp_intent_processed_title),
        subtitle = value.toSubtitle(),
        primaryButton = ExchangeIntentButtonUM(
            text = resourceReference(R.string.common_got_it),
            onClick = value.onClose,
        ),
    )

    private fun Input.toSubtitle(): TextReference? {
        if (from == null || to == null) return null
        val swap = swapTextConverter.convert(
            ExchangeIntentSwapTextConverter.Input(intent = intent, from = from, to = to),
        )

        return swap?.let { text ->
            resourceReference(
                id = R.string.mcp_swap_intent_processed_subtitle,
                formatArgs = wrappedList(text.from, text.to),
            )
        }
    }
}