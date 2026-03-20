package com.tangem.tap.common.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.utils.logging.TangemLogger

/**
 * [DeeplinkLauncher] implementation that launches deep links as intents to the current Activity
 * and opens web URLs in the browser via [UrlOpener].
 */
internal class DefaultDeeplinkLauncher(
    private val context: Context,
    private val urlOpener: UrlOpener,
) : DeeplinkLauncher {

    override fun launch(link: String) {
        val deeplinkUri = link.toUri()
        when (deeplinkUri.scheme) {
            DeepLinkScheme.Tangem.scheme,
            DeepLinkScheme.WalletConnect.scheme,
            -> launchDeepLink(deeplinkUri)
            DeepLinkScheme.Https.scheme -> launchDeeplinkOrOpenBrowser(deeplinkUri, link)
            else -> {
                TangemLogger.i(
                    """
                        No match found for deep link
                        |- Received URI: $deeplinkUri
                    """.trimIndent(),
                )
            }
        }
    }

    private fun launchDeeplinkOrOpenBrowser(uri: Uri, link: String) {
        val intent = createDeepLinkIntent(uri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            urlOpener.openUrl(link)
        }
    }

    private fun launchDeepLink(uri: Uri) {
        context.startActivity(createDeepLinkIntent(uri))
    }

    private fun createDeepLinkIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}