package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.removePrefixOrNull
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.features.intentHandler.AffectsNavigation
import com.tangem.tap.store
import timber.log.Timber
import java.net.URLDecoder

/**
[REDACTED_AUTHOR]
 */
class WalletConnectLinkIntentHandler : IntentHandler, AffectsNavigation {

    override fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean {
        val intentData = intent?.data ?: return false
        val scheme = intent.scheme ?: return false

        val wcUri = when (scheme) {
            WC_SCHEME -> intentData.toString()
            TANGEM_SCHEME -> intentData.toString().removePrefixOrNull(TANGEM_WC_PREFIX)
            else -> null
        }

        return if (wcUri.isNullOrBlank()) {
            false
        } else {
            val decodedWcUri = try {
                URLDecoder.decode(wcUri, DEFAULT_CHARSET_NAME)
            } catch (e: Exception) {
                Timber.e(e)
                return false
            }
            store.dispatchOnMain(WalletConnectAction.HandleDeepLink(decodedWcUri))
            true
        }
    }

    private companion object {
        const val TANGEM_SCHEME = "tangem"
        const val TANGEM_WC_PREFIX = "tangem://wc?uri="
        const val DEFAULT_CHARSET_NAME = "UTF-8"
        const val WC_SCHEME = "wc"
    }
}