package com.tangem.common.routing.deeplink

import android.net.Uri
import kotlinx.coroutines.CoroutineScope

/**
 * A deep link dispatched to a [DeepLinkHandler].
 *
 * @property uri the received deep link
 * @property queryParams validated query parameters of [uri]
 * @property scope scope of the screen that received the deep link
 * @property isFromOnNewIntent whether the deep link arrived to an already running activity
 */
data class DeepLinkRequest(
    val uri: Uri,
    val queryParams: Map<String, String>,
    val scope: CoroutineScope,
    val isFromOnNewIntent: Boolean,
)