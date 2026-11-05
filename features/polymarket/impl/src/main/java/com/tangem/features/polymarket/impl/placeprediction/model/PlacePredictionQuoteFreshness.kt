package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.time.CurrentTimeProvider
import javax.inject.Inject

internal class PlacePredictionQuoteFreshness @Inject constructor(
    private val currentTimeProvider: CurrentTimeProvider,
    private val messageSender: UiMessageSender,
) {

    fun refuseIfStale(quote: SignableQuoteUM): Boolean {
        val ageMillis = currentTimeProvider.nowMillis() - quote.receivedAtMillis
        if (ageMillis <= MAX_QUOTE_AGE_MILLIS) return false

        logger.i("Refused a quote $ageMillis ms old at the tap")
        messageSender.send(
            DialogMessage(message = resourceReference(R.string.prediction_place_price_changed_description)),
        )
        return true
    }

    private companion object {
        const val MAX_QUOTE_AGE_MILLIS = 30_000L

        val logger = TangemLogger.withTag(tag = "PlacePredictionQuoteFreshness")
    }
}