package com.tangem.tap.common.pushes

import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.deeplink.PayloadToDeeplinkConverter
import com.tangem.utils.extensions.uriValidate
import javax.inject.Inject

/**
 * Routes pushes received while the app is running to the matching in-app handler.
 *
 * Converts the push payload to a deeplink (via [PayloadToDeeplinkConverter]) and routes by its
 * [host][Uri.getHost] — the same routing key [DeepLinkFactory][com.tangem.tap.routing.utils.DeepLinkFactory] uses
 * for tapped deeplinks. Handlers receive the deeplink query params (not the raw payload), so both flat-key and
 * `deeplink`-style payloads are handled uniformly. Each handler owns its own reaction; add a `when` branch per
 * push type as new in-app reactions appear.
 */
internal class PushMessageHandler @Inject constructor(
    private val tokenDetailsPushHandler: TokenDetailsPushHandler,
) {

    fun onMessageReceived(data: Map<String, String>) {
        val deeplink = PayloadToDeeplinkConverter.convert(data)?.toUri() ?: return
        val queryParams = deeplink.getQueryParams()
        when (deeplink.host) {
            DeepLinkRoute.TokenDetails.host -> tokenDetailsPushHandler.handle(queryParams)
            else -> Unit
        }
    }

    private fun Uri.getQueryParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        queryParameterNames.forEach { name ->
            val value = getQueryParameter(name)
            if (name.uriValidate() && value?.uriValidate() == true) {
                params[name] = value
            }
        }
        return params
    }
}