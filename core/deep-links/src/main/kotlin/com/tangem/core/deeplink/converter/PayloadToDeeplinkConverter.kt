package com.tangem.core.deeplink.converter

import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.core.deeplink.DEEPLINK_KEY
import com.tangem.core.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.PAYLOAD_WALLET_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.utils.converter.Converter

object PayloadToDeeplinkConverter : Converter<Map<String, String>, String?> {

    override fun convert(value: Map<String, String>): String? {
        return when {
            value[DEEPLINK_KEY] != null -> value[DEEPLINK_KEY]
            isTangemPushNotificationPayload(value) -> buildNotificationDeeplink(value)
            else -> null
        }
    }

    @Suppress("ReturnCount")
    private fun buildNotificationDeeplink(payload: Map<String, String>): String? {
        val type = payload[TYPE_KEY] ?: return null
        val networkId = payload[NETWORK_ID_KEY] ?: return null
        val tokenId = payload[TOKEN_ID_KEY] ?: return null
        val walletId = payload[PAYLOAD_WALLET_ID_KEY] ?: return null

        return DeepLinkBuilder().setScheme(DeepLinkScheme.Tangem.scheme)
            .setAction(DeepLinkRoute.TokenDetails.host)
            .addQueryParam(NETWORK_ID_KEY, networkId)
            .addQueryParam(TOKEN_ID_KEY, tokenId)
            .addQueryParam(TYPE_KEY, type)
            .addQueryParam(WALLET_ID_KEY, walletId)
            .build()
    }

    private fun isTangemPushNotificationPayload(payload: Map<String, String>): Boolean {
        return payload.containsKey(TYPE_KEY) &&
            payload.containsKey(NETWORK_ID_KEY) &&
            payload.containsKey(TOKEN_ID_KEY) &&
            payload.containsKey(PAYLOAD_WALLET_ID_KEY)
    }
}