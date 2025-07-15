package com.tangem.common.routing.deeplink

import android.os.Bundle
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeeplinkConst.DEEPLINK_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NAME_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TRANSACTION_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.utils.converter.Converter

object PayloadToDeeplinkConverter : Converter<Map<String, String>, String?> {

    override fun convert(value: Map<String, String>): String? {
        return when {
            value[DEEPLINK_KEY] != null -> value[DEEPLINK_KEY]
            isTangemPushNotificationPayload(value) -> buildNotificationDeeplink(value)
            else -> null
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

            transactionId?.let { addQueryParam(TRANSACTION_ID_KEY, it) }
            name?.let { addQueryParam(NAME_KEY, it) }
        }.build()
    }

    private fun isTangemPushNotificationPayload(payload: Map<String, String>): Boolean {
        return payload.containsKey(TYPE_KEY) &&
            payload.containsKey(NETWORK_ID_KEY) &&
            payload.containsKey(TOKEN_ID_KEY) &&
            payload.containsKey(WALLET_ID_KEY)
    }
}