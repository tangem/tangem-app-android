package com.tangem.features.send.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.features.send.impl.R
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Builds the sheet of an intent that has already been processed. Without the intent or the currency it is sent from
 * the transfer cannot be named, so the sheet has no subtitle.
 */
internal class SendIntentProcessedConverter @Inject constructor(
    private val amountTextConverter: SendIntentAmountTextConverter,
) : Converter<SendIntentProcessedConverter.Input, SendIntentUM.Status> {

    data class Input(val intent: SendIntent?, val currency: CryptoCurrency?, val onClose: () -> Unit)

    override fun convert(value: Input): SendIntentUM.Status = SendIntentUM.Status(
        variant = TangemModalStatus.Variant.Info,
        title = resourceReference(R.string.mcp_intent_processed_title),
        subtitle = value.toSubtitle(),
        primaryButton = SendIntentButtonUM(text = resourceReference(R.string.common_got_it), onClick = value.onClose),
    )

    private fun Input.toSubtitle(): TextReference? {
        if (intent == null || currency == null) return null
        val amountText = amountTextConverter.convert(
            SendIntentAmountTextConverter.Input(amount = intent.amount, currency = currency),
        )

        return amountText?.let { text ->
            resourceReference(id = R.string.mcp_send_intent_processed_subtitle, formatArgs = wrappedList(text))
        }
    }
}