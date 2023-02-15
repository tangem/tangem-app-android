package com.tangem.tap.features.sprinklr.redux

import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import org.rekotlin.Middleware

internal class SprinklrMiddleware {
    val middleware: Middleware<AppState> = { _, appStateProvider ->
        { next ->
            { action ->
                val appState = appStateProvider()
                if (action is SprinklrAction && appState != null) {
                    handleAction(action)
                }
                next(action)
            }
        }
    }

    private fun handleAction(action: SprinklrAction) {
        when (action) {
            is SprinklrAction.SetConfig -> createUrl()
            is SprinklrAction.UpdateUrl -> Unit
        }
    }

    private fun createUrl() {
// [REDACTED_TODO_COMMENT]
        val url = "https://prod-live-chat.sprinklr.com/" +
            "page?appId=60c1d169c96beb5bf5a326f3_app_950954&device=MOBILE&enableClose=true&zoom=false"
        store.dispatchOnMain(SprinklrAction.UpdateUrl(url))
    }
}
