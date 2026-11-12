package com.tangem.features.walletconnect.deeplink

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import java.net.URLDecoder
import javax.inject.Inject

internal class WalletConnectDeepLinkHandler @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val wcPairService: WcPairService,
) : DeepLinkHandler {

    override val isOpenableFromPush: Boolean = false

    override fun handle(request: DeepLinkRequest) {
        val uri = request.uri
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

    companion object {
        const val WC_DEEPLINK_KEY = "wc:"
        const val TANGEM_DEEPLINK_KEY = "tangem://wc"

        private const val DEFAULT_CHARSET_NAME = "UTF-8"
    }
}