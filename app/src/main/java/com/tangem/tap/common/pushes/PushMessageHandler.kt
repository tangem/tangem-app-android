package com.tangem.tap.common.pushes

import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.features.jointaccount.deeplink.JointAccountDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.utils.extensions.uriValidate
import javax.inject.Inject

/**
 * Routes pushes received while the app is running to the matching in-app handler.
 *
 * Converts the push payload to a deeplink (via [PayloadToDeeplinkConverter]) and routes it by the deep link handler
 * that can handle it — the same handlers [DeepLinkFactory][com.tangem.tap.routing.utils.DeepLinkFactory] dispatches
 * tapped deeplinks to. Handlers receive the deeplink query params (not the raw payload), so flat-key,
 * `deeplink`- and `link`-style payloads are handled uniformly. Each handler owns its own reaction; add a `when`
 * branch per push type as new in-app reactions appear.
 *
 * Only in-app reactions live here. A payload whose destination is a web page routes nowhere in this path — a
 * background push must not open a browser — and is opened on tap instead.
 */
internal class PushMessageHandler @Inject constructor(
    private val deepLinkHandlerRegistry: DeepLinkHandlerRegistry,
    private val tokenDetailsPushHandler: TokenDetailsPushHandler,
    private val jointAccountPushHandler: JointAccountPushHandler,
) {

    fun onMessageReceived(data: Map<String, String>) {
        val deeplink = PayloadToDeeplinkConverter.convert(data)?.toUri()
            ?.takeIf(deepLinkHandlerRegistry::isOpenableFromPush)
            ?: return
        val queryParams = deeplink.getQueryParams()
        when {
            TokenDetailsDeepLinkHandler.canHandle(deeplink) -> tokenDetailsPushHandler.handle(queryParams)
            JointAccountDeepLinkHandler.canHandle(deeplink) -> jointAccountPushHandler.handle(queryParams)
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