package com.tangem.common.routing.deeplink

import android.os.Bundle
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DEEPLINK_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NAME_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TRANSACTION_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WEBLINK_KEY
import com.tangem.domain.visa.model.TangemPayPushNotificationType
import com.tangem.utils.converter.Converter

/**
 * Converts a push payload to a deeplink, in this precedence order:
 * 1. [DEEPLINK_KEY] — a ready-made deeplink, passed through verbatim.
 * 2. Tangem Pay flat keys ([CUSTOMER_WALLET_ID_KEY] + a known [TangemPayPushNotificationType]).
 * 3. Token flat keys ([TYPE_KEY] + [NETWORK_ID_KEY] + [TOKEN_ID_KEY] + [WALLET_ID_KEY]).
 * 4. [WEBLINK_KEY] — the destination of a marketing / Customer.io push (`link` is also Customer.io's own
 *    deeplink key). It is deliberately last, so it can never preempt a backend-generated transactional push.
 *
 * The result is not validated here — the caller applies [PushDeeplinkPolicy] and decides how to dispatch it.
 * A value that no handler routes falls back to a browser only for trusted web hosts, which is why an
 * arbitrary [WEBLINK_KEY] value may be returned from here.
 */
object PayloadToDeeplinkConverter : Converter<Map<String, String>, String?> {

    override fun convert(value: Map<String, String>): String? {
        return when {
            value[DEEPLINK_KEY] != null -> value[DEEPLINK_KEY]
            isTangemPayPushNotificationPayload(value) -> buildTangemPayNotificationDeeplink(value)
            isTangemPushNotificationPayload(value) -> buildNotificationDeeplink(value)
            else -> value[WEBLINK_KEY]
        }
    }

    fun convertBundle(bundle: Bundle?): String? {
        if (bundle == null) return null
        val bundleDataMap = mutableMapOf<String, String>()
        for (key in bundle.keySet()) {
            val value = bundle.getString(key)
            if (value != null) {
                bundleDataMap[key] = value
            }
        }
        return convert(bundleDataMap)
    }

    @Suppress("ReturnCount")
    private fun buildNotificationDeeplink(payload: Map<String, String>): String? {
        val type = payload[TYPE_KEY] ?: return null
        val networkId = payload[NETWORK_ID_KEY] ?: return null
        val tokenId = payload[TOKEN_ID_KEY] ?: return null
        val walletId = payload[WALLET_ID_KEY] ?: return null
        val derivationPath = payload[DERIVATION_PATH_KEY].orEmpty()
        val transactionId = payload[TRANSACTION_ID_KEY]
        val name = payload[NAME_KEY]

        return DeepLinkBuilder().setScheme(DeepLinkScheme.Tangem.scheme).apply {
            setAction(DeepLinkRoute.TokenDetails.host)
            addQueryParam(NETWORK_ID_KEY, networkId)
            addQueryParam(TOKEN_ID_KEY, tokenId)
            addQueryParam(TYPE_KEY, type)
            addQueryParam(WALLET_ID_KEY, walletId)
            if (derivationPath.isNotBlank()) {
                addQueryParam(DERIVATION_PATH_KEY, derivationPath)
            }

            if (transactionId != null) {
                addQueryParam(TRANSACTION_ID_KEY, transactionId)
            }
            if (name != null) {
                addQueryParam(NAME_KEY, name)
            }
        }.build()
    }

    private fun isTangemPushNotificationPayload(payload: Map<String, String>): Boolean {
        return payload.containsKey(TYPE_KEY) &&
            payload.containsKey(NETWORK_ID_KEY) &&
            payload.containsKey(TOKEN_ID_KEY) &&
            payload.containsKey(WALLET_ID_KEY)
    }

    private fun isTangemPayPushNotificationPayload(payload: Map<String, String>): Boolean {
        return payload.containsKey(CUSTOMER_WALLET_ID_KEY) && payload[TYPE_KEY] in TangemPayPushNotificationType.all
    }

    private fun buildTangemPayNotificationDeeplink(payload: Map<String, String>): String? {
        val walletId = payload[CUSTOMER_WALLET_ID_KEY]
        val type = payload[TYPE_KEY]
        if (walletId.isNullOrEmpty() || type.isNullOrEmpty()) return null

        return DeepLinkBuilder().setScheme(DeepLinkScheme.Tangem.scheme).apply {
            setAction(DeepLinkRoute.PayAppMain.host)
            payload.forEach { (key, value) -> addQueryParam(key, value) }
        }.build()
    }
}