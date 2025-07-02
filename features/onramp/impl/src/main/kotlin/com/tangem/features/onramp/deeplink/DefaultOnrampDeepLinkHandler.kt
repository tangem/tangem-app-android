package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultOnrampDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    @Assisted queryParams: Map<String, String>,
    appRouter: AppRouter,
    onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger,
) : OnrampDeepLinkHandler {

    init {
        val txId = queryParams[TX_ID_KEY]
        val result = OnrampRedirectResult.getResult(queryParams[RESULT_KEY])

        when {
            !txId.isNullOrEmpty() -> {
                // finish current onramp flow and show onramp success screen
                val replaceOnrampScreens = appRouter.stack
                    .filterNot { it is AppRoute.Onramp || it is AppRoute.OnrampSuccess }
                    .toMutableList() + AppRoute.OnrampSuccess(txId)

                appRouter.replaceAll(*replaceOnrampScreens.toTypedArray())
            }
            result != OnrampRedirectResult.Unknown -> {
                scope.launch {
                    onrampSuccessScreenTrigger.triggerOnrampSuccess(result == OnrampRedirectResult.Success)
                }
            }
            else -> {
                Timber.e(
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

    private companion object {
        const val TX_ID_KEY = "tx_id"
        const val RESULT_KEY = "result"
    }
}