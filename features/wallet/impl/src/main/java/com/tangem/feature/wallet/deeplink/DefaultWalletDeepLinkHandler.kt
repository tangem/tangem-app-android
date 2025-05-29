package com.tangem.feature.wallet.deeplink

import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletDeepLinkHandler @AssistedInject constructor() : WalletDeepLinkHandler {

    @AssistedFactory
    interface Factory : WalletDeepLinkHandler.Factory {
        override fun create(): DefaultWalletDeepLinkHandler
    }
}