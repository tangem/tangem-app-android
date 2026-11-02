package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.OnrampDeeplinkTarget
import com.tangem.common.routing.deeplink.resolveOnrampDeeplinkTarget
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger

internal class DefaultOnrampDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted queryParams: Map<String, String>,
    appRouter: AppRouter,
    onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger,
    conciergeWaitlistTrigger: ConciergeWaitlistTrigger,
) : OnrampDeepLinkHandler {

    init {
        when (val target = resolveOnrampDeeplinkTarget(queryParams)) {
            is OnrampDeeplinkTarget.Transaction -> {
                // finish current onramp flow and show onramp success screen
                val replaceOnrampScreens = appRouter.stack
                    .filterNot { it is AppRoute.Onramp || it is AppRoute.OnrampSuccess }
                    .toMutableList() + AppRoute.OnrampSuccess(target.txId)

                appRouter.replaceAll(*replaceOnrampScreens.toTypedArray())
            }
            is OnrampDeeplinkTarget.Redirect -> {
                scope.launch { onrampSuccessScreenTrigger.triggerOnrampSuccess(target.isSuccess) }
            }
            OnrampDeeplinkTarget.ConciergeWaitlist -> {
                scope.launch { conciergeWaitlistTrigger.triggerOpenWaitlist() }
            }
            OnrampDeeplinkTarget.Invalid -> {
                TangemLogger.e(
                    """
                       Invalid parameters for ONRAMP deeplink
                       |- Params: $queryParams
                    """.trimIndent(),
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : OnrampDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultOnrampDeepLinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://onramp"
    }
}