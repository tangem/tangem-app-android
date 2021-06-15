package com.tangem.tap.common.extensions

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Store

fun Store<AppState>.dispatchOnMain(action: Action) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(action)
    }
}