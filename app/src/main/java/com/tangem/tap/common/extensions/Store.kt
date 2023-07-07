package com.tangem.tap.common.extensions

import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.scope
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Store

/**
 * Dispatch action with creating the new coroutine with the Main dispatcher
 *
 * @see dispatchWithMain
 */
fun Store<*>.dispatchOnMain(action: Action) {
    scope.launch(Dispatchers.Main) {
        dispatch(action)
    }
}

/**
 * Dispatch action on the Main coroutine context
 *
 * @param action [Action] to be dispatched
 *
 * @see dispatchOnMain
 * */
suspend fun Store<*>.dispatchWithMain(action: Action) {
    withMainContext {
        dispatch(action)
    }
}

fun Store<*>.dispatchNotification(resId: Int) {
    dispatchOnMain(GlobalAction.ShowNotification(resId))
}

suspend fun Store<AppState>.onUserWalletSelected(
    userWallet: UserWallet,
    refresh: Boolean = false,
    sendAnalyticsEvent: Boolean = false,
) {
    state.globalState.tapWalletManager.onWalletSelected(userWallet, refresh, sendAnalyticsEvent)
}

fun Store<*>.dispatchToastNotification(resId: Int) {
    dispatchOnMain(GlobalAction.ShowToastNotification(resId))
}

fun Store<*>.dispatchErrorNotification(error: TapError) {
    dispatchOnMain(GlobalAction.ShowErrorNotification(error))
}

/**
 * @param fatal used to indicate errors that should not normally occur
 */
fun Store<*>.dispatchDebugErrorNotification(message: String, fatal: Boolean = false) {
    val prefix = if (fatal) "FATAL ERROR: " else "DEBUG ERROR: "
    dispatchDebugErrorNotification(TapError.CustomError("$prefix $message"))
}

fun Store<*>.dispatchDebugErrorNotification(error: TapError) {
    dispatchOnMain(GlobalAction.DebugShowErrorNotification(error))
}

fun Store<*>.dispatchDialogShow(dialog: StateDialog) {
    dispatchOnMain(GlobalAction.ShowDialog(dialog))
}

fun Store<*>.dispatchDialogHide() {
    dispatchOnMain(GlobalAction.HideDialog)
}

/**
 * Dispatch action inside a coroutine with the Main dispatcher
 */
@Deprecated(
    message = "Use dispatchWithMain instead",
    replaceWith = ReplaceWith(expression = "dispatchWithMain"),
)
suspend fun dispatchOnMain(vararg actions: Action) {
    withMainContext { actions.forEach { store.dispatch(it) } }
}

fun Store<*>.dispatchOpenUrl(url: String) {
    dispatch(NavigationAction.OpenUrl(url))
}

fun Store<*>.dispatchShare(url: String) {
    dispatch(NavigationAction.Share(url))
}