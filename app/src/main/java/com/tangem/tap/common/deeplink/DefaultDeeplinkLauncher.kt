package com.tangem.tap.common.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.uri.ExternalUrlValidator
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
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
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : DeeplinkLauncher {

    override fun launch(link: String, shouldReportFailure: Boolean) {
        val deeplinkUri = link.toUri()
        val failure = when (deeplinkUri.scheme) {
            DeepLinkScheme.Tangem.scheme,
            DeepLinkScheme.WalletConnect.scheme,
            -> launchDeepLink(deeplinkUri)
            DeepLinkScheme.Https.scheme -> {
                if (ExternalUrlValidator.isUriTrusted(deeplinkUri)) {
                    launchDeeplinkOrOpenBrowser(deeplinkUri, link)
                    null
                } else {
                    UntrustedDeeplinkException(deeplinkUri)
                }
            }
            else -> UnsupportedDeeplinkException(deeplinkUri)
        } ?: return

        TangemLogger.i(
            """
                ${failure.message}
                |- Received URI: $deeplinkUri
            """.trimIndent(),
        )
        if (shouldReportFailure) reportFailure(deeplinkUri, failure)
    }

    private fun launchDeeplinkOrOpenBrowser(uri: Uri, link: String) {
        val intent = createDeepLinkIntent(uri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            urlOpener.openUrl(link)
        }
    }

    private fun launchDeepLink(uri: Uri): UnopenableDeeplinkException? {
        val intent = createDeepLinkIntent(uri)
        if (intent.resolveActivity(context.packageManager) == null) return UnresolvedDeeplinkException(uri)

        context.startActivity(intent)
        return null
    }

    private fun reportFailure(uri: Uri, exception: UnopenableDeeplinkException) {
        analyticsExceptionHandler.sendException(
            ExceptionAnalyticsEvent(
                exception = exception,
                params = mapOf(
                    "uri_scheme" to uri.scheme.orEmpty(),
                    "uri_host" to uri.host.orEmpty(),
                ),
            ),
        )
    }

    private fun createDeepLinkIntent(uri: Uri): Intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

internal sealed class UnopenableDeeplinkException(uri: Uri, reason: String) :
    RuntimeException("$reason: scheme=${uri.scheme}, host=${uri.host}")

internal class UnresolvedDeeplinkException(uri: Uri) :
    UnopenableDeeplinkException(uri, reason = "Deeplink has no matching activity")

internal class UntrustedDeeplinkException(uri: Uri) :
    UnopenableDeeplinkException(uri, reason = "Deeplink is not a trusted https link")

internal class UnsupportedDeeplinkException(uri: Uri) :
    UnopenableDeeplinkException(uri, reason = "Deeplink scheme is not supported")