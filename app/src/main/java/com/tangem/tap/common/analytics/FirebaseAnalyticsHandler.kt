package com.tangem.tap.common.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.tangem.TangemSdkError
import com.tangem.blockchain.common.Blockchain
import com.tangem.commands.common.card.Card
import com.tangem.tap.common.extensions.filterNotNull

object FirebaseAnalyticsHandler : AnalyticsHandler {
    override fun triggerEvent(event: AnalyticsEvent, card: Card?, blockchain: Blockchain?) {
        Firebase.analytics.logEvent(event.event, setData(card, blockchain))
    }

    fun logException(name: String, throwable: Throwable) {
        Firebase.analytics.logEvent(name, bundleOf(
            "message" to (throwable.message ?: "none"),
            "cause_message" to (throwable.cause?.message ?: "none"),
            "stack_trace" to throwable.stackTraceToString()
        ))
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?,
    ) {
        if (error is TangemSdkError.UserCancelled) return

        val params = parameters?.toMutableMap() ?: mutableMapOf()
        if (card != null) params + getParamsFromCard(card)
        params[AnalyticsParam.ACTION] = actionToLog.key
        params[AnalyticsParam.ERROR_CODE] = error.code.toString()
        params[AnalyticsParam.ERROR_DESCRIPTION] = error.javaClass.simpleName
        params[AnalyticsParam.ERROR_KEY] = "TangemSdkError"

        params.forEach { (key, value) ->
            FirebaseCrashlytics.getInstance().setCustomKey(key.param, value)
        }
        val cardError = TangemSdk.map(error)
        FirebaseCrashlytics.getInstance().recordException(cardError)
    }

    private fun getParamsFromCard(card: Card): Map<AnalyticsParam, String> {
        return mapOf(
            AnalyticsParam.FIRMWARE to card.firmwareVersion.version,
            AnalyticsParam.BATCH_ID to card.cardData?.batchId
        ).filterNotNull()
    }

    private fun setData(card: Card?, blockchain: Blockchain?): Bundle {
        if (card == null) return bundleOf()
        return bundleOf(
            AnalyticsParam.BLOCKCHAIN.param to (blockchain?.currency ?: card.cardData?.blockchainName),
            AnalyticsParam.BATCH_ID.param to card.cardData?.batchId,
            AnalyticsParam.FIRMWARE.param to card.firmwareVersion.version
        )
    }

    enum class AnalyticsParam(val param: String) {
        BLOCKCHAIN("blockchain"),
        BATCH_ID("batch_id"),
        FIRMWARE("firmware"),
        ACTION("action"),
        ERROR_DESCRIPTION("error_description"),
        ERROR_CODE("error_code"),
        NEW_SECURITY_OPTION("new_security_option"),
        ERROR_KEY("Tangem SDK error key"),
    }

    enum class ActionToLog(val key: String) {
        Scan("tap_scan_task"),
        SendTransaction("send_transaction"),
        WalletConnectSign("wallet_connect_personal_sign"),
        WalletConnectTransaction("wallet_connect_tx_sign"),
        ReadPinSettings("read_pin_settings"),
        ChangeSecOptions("change_sec_options"),
        CreateWallet("create_wallet"),
        PurgeWallet("purge_wallet"),
        WriteIssuerData("write_issuer_data"),
    }
} 