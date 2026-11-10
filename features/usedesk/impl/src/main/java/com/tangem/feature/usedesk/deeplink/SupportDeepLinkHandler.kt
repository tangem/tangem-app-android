package com.tangem.feature.usedesk.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.utils.popToIfCovered
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SupportDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        request.scope.launch {
            // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
            val userWallet = getSelectedWalletSyncUseCase().getOrElse { error ->
                TangemLogger.e("Error on getting user wallet: $error")
                return@launch
            }

            val walletMetaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrElse { error ->
                TangemLogger.e("Error on getting wallet meta info", error)
                return@launch
            }

            val openedChat = appRouter.stack.lastOrNull { it is AppRoute.Usedesk }
            if (openedChat != null) {
                appRouter.popToIfCovered(openedChat)
                return@launch
            }

            appRouter.push(
                AppRoute.Usedesk(
                    walletMetaInfo = walletMetaInfo,
                    source = AnalyticsParam.ScreensSources.Deeplink,
                ),
            )
        }
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://support"
    }
}