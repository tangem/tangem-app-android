package com.tangem.tap.common.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tangem.commands.common.card.Card

object FirebaseAnalyticsHandler : AnalyticsHandler {
    override fun triggerEvent(event: AnalyticsEvent, card: Card?) {
        Firebase.analytics.logEvent(event.event, setCardData(card))
    }

    fun logException(name: String, throwable: Throwable) {
        Firebase.analytics.logEvent(name, bundleOf(
                "message" to (throwable.message ?: "none"),
                "cause_message" to (throwable.cause?.message ?: "none"),
                "stack_trace" to throwable.stackTraceToString()
        ))
    }

    private fun setCardData(card: Card?): Bundle {
        if (card == null) return bundleOf()
        return bundleOf(
                AnalyticsParam.BLOCKCHAIN.param to card.cardData?.blockchainName,
                AnalyticsParam.BATCH_ID.param to card.cardData?.batchId,
                AnalyticsParam.FIRMWARE.param to card.firmwareVersion.version
        )
    }

    private enum class AnalyticsParam(val param: String) {
        BLOCKCHAIN("blockchain"),
        BATCH_ID("batch_id"),
        FIRMWARE("firmware"),
    }
} 