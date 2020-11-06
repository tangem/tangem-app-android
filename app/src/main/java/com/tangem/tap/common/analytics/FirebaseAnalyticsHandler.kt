package com.tangem.tap.common.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.tangem.commands.Card

class FirebaseAnalyticsHandler(private val context: Context): AnalyticsHandler {
    override fun triggerEvent(event: AnalyticsEvent, card: Card?) {
        FirebaseAnalytics.getInstance(context)
                .logEvent(AnalyticsEvent.CARD_IS_SCANNED.event, setCardData(card))
    }

    private fun setCardData(card: Card?): Bundle {
        if (card == null) return bundleOf()
        return bundleOf(
                AnalyticsParam.BLOCKCHAIN.param to card.cardData?.blockchainName,
                AnalyticsParam.BATCH_ID.param to card.cardData?.batchId,
                AnalyticsParam.FIRMWARE.param to card.firmwareVersion
        )
    }

    private enum class AnalyticsParam(val param: String) {
        BLOCKCHAIN("blockchain"),
        BATCH_ID("batch_id"),
        FIRMWARE("firmware"),
    }
}