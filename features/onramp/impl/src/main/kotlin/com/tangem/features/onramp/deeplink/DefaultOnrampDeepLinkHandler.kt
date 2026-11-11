package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.OnrampDeeplinkTarget
import com.tangem.common.routing.deeplink.resolveOnrampDeeplinkTarget
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class DefaultOnrampDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
    private val onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger,
    private val conciergeWaitlistTrigger: ConciergeWaitlistTrigger,
) : DeepLinkHandler, OnrampDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        when (val target = resolveOnrampDeeplinkTarget(request.queryParams)) {
            is OnrampDeeplinkTarget.Transaction -> {
                // finish current onramp flow and show onramp success screen
                val replaceOnrampScreens = appRouter.stack
                    .filterNot { it is AppRoute.Onramp || it is AppRoute.OnrampSuccess }
                    .toMutableList() + AppRoute.OnrampSuccess(target.txId)

                appRouter.replaceAll(*replaceOnrampScreens.toTypedArray())
            }
            is OnrampDeeplinkTarget.Redirect -> {
                request.scope.launch { onrampSuccessScreenTrigger.triggerOnrampSuccess(target.isSuccess) }
            }
            OnrampDeeplinkTarget.ConciergeWaitlist -> {
                request.scope.launch { conciergeWaitlistTrigger.triggerOpenWaitlist() }
            }
            OnrampDeeplinkTarget.Invalid -> {
                TangemLogger.e(
                    """
                       Invalid parameters for ONRAMP deeplink
                       |- Params: ${request.queryParams}
                    """.trimIndent(),
                )
            }
        }
    }
}