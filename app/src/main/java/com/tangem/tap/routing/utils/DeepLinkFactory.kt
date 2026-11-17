package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.features.tangempay.deeplink.TangemPayMainDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Receives deep links and keeps the last one until [RootDeepLinkDispatcher] takes it. */
@ActivityScoped
internal class DeepLinkFactory @Inject constructor() {

    val parkedDeepLink: StateFlow<ParkedDeepLink?>
        field = MutableStateFlow<ParkedDeepLink?>(null)

    /**
     * Parks [deeplinkUri] in place of the previously parked one.
     *
     * [source] decides the outcome when no handler matches — see [DeeplinkSource]. It defaults to
     * [DeeplinkSource.External], the conservative option, which drops an unroutable URI.
     */
    fun handleDeeplink(
        deeplinkUri: Uri,
        coroutineScope: CoroutineScope,
        isFromOnNewIntent: Boolean,
        source: DeeplinkSource = DeeplinkSource.External,
    ) {
        if (source == DeeplinkSource.External && TangemPayMainDeepLinkHandler.canHandle(deeplinkUri)) {
            TangemLogger.i("Tangem Pay main deep link is openable only from push")
            return
        }

        TangemLogger.i(
            """
                Received deep link intent
                |- Received URI: $deeplinkUri
                |- Source: $source
            """.trimIndent(),
        )
        parkedDeepLink.value = ParkedDeepLink(
            uri = deeplinkUri,
            source = source,
            scope = coroutineScope,
            isFromOnNewIntent = isFromOnNewIntent,
        )
    }

    /** Takes [deepLink] out of the box. Returns `false` when it has already been taken or replaced. */
    fun consume(deepLink: ParkedDeepLink): Boolean = parkedDeepLink.compareAndSet(expect = deepLink, update = null)
}

/** A deep link waiting for the app to become routable, and where it came from. */
internal data class ParkedDeepLink(
    val uri: Uri,
    val source: DeeplinkSource,
    val scope: CoroutineScope,
    val isFromOnNewIntent: Boolean,
)