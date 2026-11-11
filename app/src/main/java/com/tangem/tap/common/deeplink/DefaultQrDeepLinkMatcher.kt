package com.tangem.tap.common.deeplink

import androidx.core.net.toUri
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.domain.qrscanning.QrDeepLinkMatcher

/**
 * A page of a trusted Tangem host counts even without a handler: [DefaultDeeplinkLauncher] opens it in-app when the
 * manifest claims it, otherwise in the browser.
 */
internal class DefaultQrDeepLinkMatcher(
    private val deepLinkHandlerRegistry: DeepLinkHandlerRegistry,
) : QrDeepLinkMatcher {

    override fun isDeepLink(link: String): Boolean {
        val uri = link.toUri()

        return when (uri.scheme) {
            // A printed code is as untrusted as a push payload
            DeepLinkScheme.Tangem.scheme -> deepLinkHandlerRegistry.isOpenableFromPush(uri)
            DeepLinkScheme.Https.scheme -> ExternalUrlValidator.isUriTrusted(uri)
            else -> false
        }
    }
}