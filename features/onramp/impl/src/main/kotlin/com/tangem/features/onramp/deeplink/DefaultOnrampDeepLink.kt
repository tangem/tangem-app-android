package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DefaultOnrampDeepLink @AssistedInject constructor(
    private val appRouter: AppRouter,
    private val onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger,
    @Assisted private val scope: CoroutineScope,
) : OnrampDeepLink() {

    override fun onReceive(params: Map<String, String>) {
        val txId = params[TX_ID_KEY]
        val result = OnrampRedirectResult.getResult(params[RESULT_KEY])

        when {
            !txId.isNullOrEmpty() -> {
                // finish current onramp flow and show onramp success screen
                val replaceOnrampScreens = appRouter.stack
                    .filterNot { it is AppRoute.Onramp }
                    .toMutableList()
                replaceOnrampScreens.add(AppRoute.OnrampSuccess(txId))
                appRouter.replaceAll(*replaceOnrampScreens.toTypedArray())
            }
            result != OnrampRedirectResult.Unknown -> {
                scope.launch {
                    onrampSuccessScreenTrigger.triggerOnrampSuccess(result == OnrampRedirectResult.Success)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : OnrampDeepLink.Factory {
        override fun create(coroutineScope: CoroutineScope): DefaultOnrampDeepLink
    }

    private companion object {
        const val TX_ID_KEY = "tx_id"
        const val RESULT_KEY = "result"
    }
}