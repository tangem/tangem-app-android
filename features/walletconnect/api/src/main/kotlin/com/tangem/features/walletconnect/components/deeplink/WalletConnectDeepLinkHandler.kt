package com.tangem.features.walletconnect.components.deeplink

import android.net.Uri

interface WalletConnectDeepLinkHandler {

    interface Factory {
        fun create(uri: Uri): WalletConnectDeepLinkHandler
    }
}