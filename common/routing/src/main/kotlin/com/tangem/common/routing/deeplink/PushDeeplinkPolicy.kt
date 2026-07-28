package com.tangem.common.routing.deeplink

import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme

/**
 * Security policy for deep links that originate from a push-notification payload.
 *
 * A push payload can carry a ready-made deep link that is routed verbatim when the notification is tapped. Applied at
 * the push boundary (not inside the converter), it blocks the critical routes a notification must never reach:
 * WalletConnect pairing (both the raw `wc:` scheme and `tangem://wc`, which pairs with an attacker-controlled session)
 * and the sell redirect (which prefills the Send screen with a payload-supplied address). Every other route is allowed.
 */
object PushDeeplinkPolicy {

    fun isOpenableFromPush(scheme: String?, host: String?): Boolean = when {
        // A raw "wc:" link is paired straight from the scheme, without a host.
        scheme?.lowercase() == DeepLinkScheme.WalletConnect.scheme -> false
        host == DeepLinkRoute.WalletConnect.host -> false
        host == DeepLinkRoute.SellRedirect.host -> false
        else -> true
    }
}