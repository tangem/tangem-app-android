package com.tangem.tap.common.extensions

import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.proxy.redux.DaggerGraphState
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

suspend fun Store<AppState>.onUserWalletSelected(userWallet: UserWallet) {
    state.globalState.tapWalletManager.onWalletSelected(userWallet)
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

fun Store<AppState>.dispatchNavigationAction(action: AppRouter.() -> Unit) {
    inject(DaggerGraphState::appRouter).action()
}

inline fun <reified T> Store<AppState>.inject(getDependency: DaggerGraphState.() -> T?): T {
    return requireNotNull(state.daggerGraphState.getDependency()) {
        "${T::class.simpleName.orEmpty()} isn't initialized "
    }
}