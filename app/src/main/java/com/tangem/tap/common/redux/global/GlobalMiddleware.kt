package com.tangem.tap.common.redux.global

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

object GlobalMiddleware {
    val handler = globalMiddlewareHandler
}

private val globalMiddlewareHandler: Middleware<AppState> = { _, _ ->
    { nextDispatch ->
        { action ->
            handleAction(action)
            nextDispatch(action)
        }
    }
}

private fun handleAction(action: Action) {
    when (action) {
        is GlobalAction.RestoreAppCurrency -> restoreAppCurrency()
    }
}

private fun restoreAppCurrency() {
    scope.launch {
        val currency = store.inject(DaggerGraphState::appCurrencyRepository)
            .getSelectedAppCurrency()
            .firstOrNull()
            ?: AppCurrency.Default

        store.dispatchWithMain(GlobalAction.RestoreAppCurrency.Success(currency))
    }
}