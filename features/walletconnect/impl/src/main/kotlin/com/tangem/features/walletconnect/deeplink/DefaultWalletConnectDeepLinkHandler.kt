package com.tangem.features.walletconnect.deeplink

import android.net.Uri
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.net.URLDecoder

internal class DefaultWalletConnectDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    wcPairService: WcPairService,
) : WalletConnectDeepLinkHandler {

    init {
        val wcUri =
            // deeplink type "tangem://wc/?uri=wc:..."
            runCatching { uri.getQueryParameter("uri") }.getOrNull()
                // direct deeplink "wc:..."
                ?: uri.toString()
        if (wcUri.isNotBlank()) {
            try {
                // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
                getSelectedWalletSyncUseCase().fold(
                    ifLeft = {
                        TangemLogger.e("Error on getting user wallet: $it")
                    },
                    ifRight = { wallet ->
                        val decodedWcUri = URLDecoder.decode(wcUri, DEFAULT_CHARSET_NAME)
                        wcPairService.pair(
                            request = WcPairRequest(
                                uri = decodedWcUri,
                                source = WcPairRequest.Source.DEEPLINK,
                                userWalletId = wallet.walletId,
                            ),
                        )
                    },
                )
            } catch (e: Exception) {
                TangemLogger.e("Error", e)
            }
        }
    }

    @AssistedFactory
    interface Factory : WalletConnectDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultWalletConnectDeepLinkHandler
    }

    private companion object {
        const val DEFAULT_CHARSET_NAME = "UTF-8"
    }
}