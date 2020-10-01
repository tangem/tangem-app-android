package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import org.rekotlin.Middleware

val globalMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is GlobalAction.RestoreAppCurrency -> {
                    store.dispatch(GlobalAction.RestoreAppCurrency.Success(
                            preferencesStorage.getAppCurrency()
                    ))
                }
            }
            nextDispatch(action)
        }
    }
}