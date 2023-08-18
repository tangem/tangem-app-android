package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.removePrefixOrNull
import com.tangem.tap.domain.walletconnect.WalletConnectManager
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.store
import timber.log.Timber
import java.net.URLDecoder

/**
[REDACTED_AUTHOR]
 */
class WalletConnectLinkIntentHandler : IntentHandler {

    override suspend fun handleIntent(intent: Intent?): Boolean {
        val intentData = intent?.data ?: return false
        val scheme = intent.scheme ?: return false

        val wcUri = when (scheme) {
            WalletConnectManager.WC_SCHEME -> intentData.toString()
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
            store.dispatchWithMain(WalletConnectAction.HandleDeepLink(decodedWcUri))
            true
        }
    }

    private companion object {
        private const val TANGEM_SCHEME = "tangem"
        private const val TANGEM_WC_PREFIX = "tangem://wc?uri="
        private const val DEFAULT_CHARSET_NAME = "UTF-8"
    }
}