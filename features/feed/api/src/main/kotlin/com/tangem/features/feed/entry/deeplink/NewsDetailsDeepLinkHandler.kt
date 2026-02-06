package com.tangem.features.feed.entry.deeplink

import android.net.Uri
import kotlinx.coroutines.CoroutineScope

interface NewsDetailsDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): NewsDetailsDeepLinkHandler
    }
}