package com.tangem.feature.swap.mcp.model

import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/** Builds the sheet of an intent that cannot be reached: "Try again" when reading it again may help, else "Got it". */
internal class ExchangeIntentCantReachConverter @Inject constructor() :
    Converter<ExchangeIntentCantReachConverter.Input, ExchangeIntentUM.Status> {

    data class Input(val onRetry: (() -> Unit)?, val onClose: () -> Unit)

    override fun convert(value: Input): ExchangeIntentUM.Status = ExchangeIntentUM.Status(
        variant = TangemModalStatus.Variant.Warning,
        title = resourceReference(R.string.mcp_swap_intent_cant_reach_title),
        subtitle = null,
        primaryButton = value.onRetry
            ?.let { onRetry ->
                ExchangeIntentButtonUM(text = resourceReference(R.string.alert_button_try_again), onClick = onRetry)
            }
            ?: ExchangeIntentButtonUM(text = resourceReference(R.string.common_got_it), onClick = value.onClose),
    )
}