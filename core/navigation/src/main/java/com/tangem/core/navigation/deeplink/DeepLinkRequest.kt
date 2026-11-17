package com.tangem.core.navigation.deeplink

import android.net.Uri
import com.tangem.core.decompose.navigation.Router
import kotlinx.coroutines.CoroutineScope

/**
 * A deep link dispatched to a [DeepLinkHandler].
 *
 * @property uri the received deep link
 * @property queryParams validated query parameters of [uri]
 * @property scope scope of the screen that received the deep link
 * @property isFromOnNewIntent whether the deep link arrived to an already running activity
 * @property router router of the root stack, ready to route when the request is dispatched
 */
data class DeepLinkRequest(
    val uri: Uri,
    val queryParams: Map<String, String>,
    val scope: CoroutineScope,
    val isFromOnNewIntent: Boolean,
    val router: Router,
)