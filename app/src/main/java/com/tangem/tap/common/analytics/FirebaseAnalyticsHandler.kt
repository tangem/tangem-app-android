package com.tangem.tap.common.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.extensions.filterNotNull

object FirebaseAnalyticsHandler : AnalyticsHandler {
    override fun triggerEvent(event: AnalyticsEvent, card: Card?, blockchain: String?) {
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

        params.forEach {
            FirebaseCrashlytics.getInstance().setCustomKey(it.key.param, it.value)
        }
        val cardError = TangemSdk.map(error)
        FirebaseCrashlytics.getInstance().recordException(cardError)
    }

    private fun getParamsFromCard(card: Card): Map<AnalyticsParam, String> {
        return mapOf(
            AnalyticsParam.FIRMWARE to card.firmwareVersion.stringValue,
            AnalyticsParam.BATCH_ID to card.batchId
        ).filterNotNull()
    }

    private fun setData(card: Card?, blockchain: String?): Bundle {
        if (card == null) return bundleOf()
        return bundleOf(
            AnalyticsParam.BLOCKCHAIN.param to (blockchain),
            AnalyticsParam.BATCH_ID.param to card.batchId,
            AnalyticsParam.FIRMWARE.param to card.firmwareVersion.stringValue
        )
    }

    fun logWcEvent(event: WcAnalyticsEvent) {
        when (event) {
            is WcAnalyticsEvent.Action -> {

                Firebase.analytics.logEvent(
                    AnalyticsEvent.WC_SUCCESS_RESPONSE.event, bundleOf(
                        AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action.name
                    )
                )
            }
            is WcAnalyticsEvent.Error -> {
                mapOf(
                    AnalyticsParam.WALLET_CONNECT_ACTION to event.action?.name,
                    AnalyticsParam.ERROR_DESCRIPTION to event.error.message
                )
                    .filterNotNull()
                    .forEach {
                        FirebaseCrashlytics.getInstance().setCustomKey(it.key.param, it.value)
                    }
                FirebaseCrashlytics.getInstance().recordException(event.error)
            }
            is WcAnalyticsEvent.InvalidRequest ->
                Firebase.analytics.logEvent(
                    AnalyticsEvent.WC_INVALID_REQUEST.event, bundleOf(
                        AnalyticsParam.WALLET_CONNECT_REQUEST.param to event.json
                    )
                )
            is WcAnalyticsEvent.Session -> {
                val analyticsEvent = when (event.event) {
                    WcSessionEvent.Disconnect -> AnalyticsEvent.WC_SESSION_DISCONNECTED
                    WcSessionEvent.Connect -> AnalyticsEvent.WC_NEW_SESSION
                }
                Firebase.analytics.logEvent(
                    analyticsEvent.event, bundleOf(
                        AnalyticsParam.WALLET_CONNECT_DAPP_URL.param to event.url
                    )
                )
            }
        }

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
        WALLET_CONNECT_ACTION("wallet_connect_action"),
        WALLET_CONNECT_REQUEST("wallet_connect_request"),
        WALLET_CONNECT_DAPP_URL("wallet_connect_dapp_url"),
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

    enum class WcSessionEvent { Disconnect, Connect }

    sealed class WcAnalyticsEvent {
        data class Error(val error: Throwable, val action: WcAction?) :
            WcAnalyticsEvent()

        data class Session(val event: WcSessionEvent, val url: String?) : WcAnalyticsEvent()
        data class Action(val action: WcAction) : WcAnalyticsEvent()
        data class InvalidRequest(val json: String?) : WcAnalyticsEvent()
    }

    enum class WcAction { PersonalSign, SignTransaction, SendTransaction }
}