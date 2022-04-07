package com.tangem.domain.features.global.redux

import android.webkit.ValueCallback
import com.tangem.domain.restore.BaseStoreHub
import com.tangem.domain.restore.DomainState
import org.rekotlin.Action
import org.rekotlin.DispatchFunction

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalMiddleware and the GlobalReducer
internal class DomainGlobalHub : BaseStoreHub<DomainGlobalState>("DomainGlobalHub") {

    override fun getHubState(storeState: DomainState): DomainGlobalState {
        return storeState.globalState
    }

    override fun updateStoreState(storeState: DomainState, newState: DomainGlobalState): DomainState {
        return storeState.copy(globalState = newState)
    }

    override suspend fun handleAction(
        state: DomainState,
        action: Action,
        dispatch: DispatchFunction,
        cancel: ValueCallback<Action>
    ) {
        if (action !is DomainGlobalAction) return

        val state = state.globalState

        when (action) {
        }
    }

    override fun reduceAction(action: Action, state: DomainGlobalState): DomainGlobalState = when (action) {
        is DomainGlobalAction.SetScanResponse -> {
            state.copy(scanResponse = action.scanResponse)
        }
        else -> state
    }
}