package com.tangem.tap.common.extensions

import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.tasks.product.ScanResponse
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

suspend fun Store<*>.onCardScanned(scanResponse: ScanResponse) {
    store.state.globalState.tapWalletManager.onCardScanned(scanResponse)
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


fun Store<*>.dispatchErrorNotification(error: TapError) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.ShowErrorNotification(error))
    }
}

/**
 * @param fatal used to indicate errors that should not normally have occurred
 */
fun Store<*>.dispatchDebugErrorNotification(message: String, fatal: Boolean = false) {
    val prefix = if (fatal) "FATAL ERROR: " else "DEBUG ERROR: "
    dispatchDebugErrorNotification(TapError.CustomError("$prefix $message"))
}

fun Store<*>.dispatchDebugErrorNotification(error: TapError) {
    scope.launch(Dispatchers.Main) {
        store.dispatch(GlobalAction.DebugShowErrorNotification(error))
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
