package com.tangem.tap.features.sprinklr.redux

import com.tangem.datasource.config.models.SprinklrConfig
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.sprinklr.redux.model.SprinklrUrl
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
            is SprinklrAction.Init -> updateUrl(action.userId, action.config)
            is SprinklrAction.UpdateUrl,
            is SprinklrAction.UpdateSprinklrDomains,
            -> Unit
        }
    }

    private fun updateUrl(userId: String, config: SprinklrConfig) {
        updateSprinklrDomains(config.baseUrl)
        val url = SprinklrUrl.Prod(userId, config.baseUrl, config.appId).url
        store.dispatchOnMain(SprinklrAction.UpdateUrl(url))
    }

    private fun updateSprinklrDomains(baseUrl: String) {
        val domains = listOf(baseUrl, SprinklrUrl.Static.url)
        store.dispatchOnMain(SprinklrAction.UpdateSprinklrDomains(domains))
    }
}
