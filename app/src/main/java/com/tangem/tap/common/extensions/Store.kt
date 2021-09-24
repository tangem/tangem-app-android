package com.tangem.tap.common.extensions

import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Store

fun Store<*>.dispatchOnMain(action: Action) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(action)
    }
}

fun Store<*>.dispatchOpenUrl(url: String) {
    store.dispatch(NavigationAction.OpenUrl(url))
}

fun Store<*>.dispatchNotification(resId: Int) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.ShowNotification(resId))
    }
}

fun Store<*>.dispatchToastNotification(resId: Int) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.ShowToastNotification(resId))
    }
}

fun Store<*>.dispatchDialogShow(dialog: StateDialog) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.ShowDialog(dialog))
    }
}

fun Store<*>.dispatchDialogHide() {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.HideDialog)
    }
}
