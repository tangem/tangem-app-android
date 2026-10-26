package com.tangem.feature.usedesk.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.popToIfCovered
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.usedesk.api.deeplink.SupportDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DefaultSupportDeepLinkHandler @AssistedInject constructor(
    @Assisted coroutineScope: CoroutineScope,
    appRouter: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
) : SupportDeepLinkHandler {

    init {
        coroutineScope.launch {
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

    @AssistedFactory
    interface Factory : SupportDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope): DefaultSupportDeepLinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://support"
    }
}