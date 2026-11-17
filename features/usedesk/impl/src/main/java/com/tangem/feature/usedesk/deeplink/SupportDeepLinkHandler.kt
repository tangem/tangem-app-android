package com.tangem.feature.usedesk.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SupportDeepLinkHandler @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        request.scope.launch {
            val userWallet = getSelectedWalletSyncUseCase().getOrElse { error ->
                TangemLogger.e("Error on getting user wallet: $error")
                return@launch
            }

            val walletMetaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrElse { error ->
                TangemLogger.e("Error on getting wallet meta info", error)
                return@launch
            }

            val chat = AppRoute.Usedesk(
                walletMetaInfo = walletMetaInfo,
                source = AnalyticsParam.ScreensSources.Deeplink,
            )
            // An opened chat is brought back rather than opened twice
            router.popToMatching(AppRoute.Usedesk::class, predicate = { true }) { isChatOpened ->
                if (!isChatOpened) router.push(chat)
            }
        }
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://support"
    }
}