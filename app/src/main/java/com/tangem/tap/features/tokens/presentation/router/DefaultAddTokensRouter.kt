package com.tangem.tap.features.tokens.presentation.router

import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store

/**
 * Router implementation for add tokens feature
 *
[REDACTED_AUTHOR]
 */
internal class DefaultAddTokensRouter : AddTokensRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
        store.dispatch(TokensAction.ResetState)
    }

    override fun openAddCustomTokenScreen() {
        Analytics.send(ManageTokens.ButtonCustomToken())
        store.dispatch(TokensAction.PrepareAndNavigateToAddCustomToken)
    }
}