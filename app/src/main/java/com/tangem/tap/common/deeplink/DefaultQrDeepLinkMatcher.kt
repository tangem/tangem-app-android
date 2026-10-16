package com.tangem.tap.common.deeplink

import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.common.routing.deeplink.PushDeeplinkPolicy
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.domain.qrscanning.QrDeepLinkMatcher
import com.tangem.tap.routing.utils.DeepLinkFeatureToggles

/**
 * A page of a trusted Tangem host counts even without a handler: [DefaultDeeplinkLauncher] opens it in-app when the
 * manifest claims it, otherwise in the browser.
 */
internal class DefaultQrDeepLinkMatcher(
    private val deepLinkHandlerRegistry: DeepLinkHandlerRegistry,
    private val deepLinkFeatureToggles: DeepLinkFeatureToggles,
) : QrDeepLinkMatcher {

    override fun isDeepLink(link: String): Boolean {
        if (!deepLinkFeatureToggles.isQrScanDeepLinksEnabled) return false

        val uri = link.toUri()

        return when (uri.scheme) {
            DeepLinkScheme.Tangem.scheme -> isScreenLink(uri)
            DeepLinkScheme.Https.scheme -> ExternalUrlValidator.isHostTrusted(uri.host)
            else -> false
        }
    }

    /**
     * A printed code is as untrusted as a push payload, so [PushDeeplinkPolicy] applies; the buy redirect is bound to a
     * no-op handler and would open nothing.
     */
    private fun isScreenLink(uri: Uri): Boolean {
        return uri.host != DeepLinkRoute.BuyRedirect.host &&
            PushDeeplinkPolicy.isOpenableFromPush(uri.scheme, uri.host) &&
            deepLinkHandlerRegistry.find(uri) != null
    }
}