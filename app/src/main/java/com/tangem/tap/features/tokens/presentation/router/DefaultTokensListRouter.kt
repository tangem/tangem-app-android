package com.tangem.tap.features.tokens.presentation.router

import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store

/**
 * Default implementation of tokens list router
 *
[REDACTED_AUTHOR]
 */
internal class DefaultTokensListRouter : TokensListRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
        store.dispatch(TokensAction.ResetState)
    }

    override fun openAddCustomTokenScreen() {
        store.dispatch(TokensAction.PrepareAndNavigateToAddCustomToken)
    }
}