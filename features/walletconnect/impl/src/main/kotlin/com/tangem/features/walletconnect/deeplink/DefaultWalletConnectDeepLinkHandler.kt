package com.tangem.features.walletconnect.deeplink

import android.net.Uri
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.net.URLDecoder

internal class DefaultWalletConnectDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    walletConnectFeatureToggles: WalletConnectFeatureToggles,
    wcPairService: WcPairService,
) : WalletConnectDeepLinkHandler {

    init {
        val wcUri = uri.toString()
        if (uri.toString().isNotBlank()) {
            try {
                // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
                getSelectedWalletSyncUseCase().fold(
                    ifLeft = {
                        Timber.e("Error on getting user wallet: $it")
                    },
                    ifRight = { wallet ->
                        val decodedWcUri = URLDecoder.decode(wcUri, DEFAULT_CHARSET_NAME)
                        if (walletConnectFeatureToggles.isRedesignedWalletConnectEnabled) {
                            wcPairService.pair(
                                request = WcPairRequest(
                                    uri = decodedWcUri,
                                    source = WcPairRequest.Source.DEEPLINK,
                                    userWalletId = wallet.walletId,
                                ),
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                Timber.e(e)
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