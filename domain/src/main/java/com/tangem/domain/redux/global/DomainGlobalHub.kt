package com.tangem.domain.redux.global

import android.webkit.ValueCallback
import com.tangem.domain.redux.BaseStoreHub
import com.tangem.domain.redux.DomainState
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalMiddleware and the GlobalReducer
internal class DomainGlobalHub : BaseStoreHub<DomainGlobalState>("DomainGlobalHub") {

    override fun getHubState(storeState: DomainState): DomainGlobalState {
        return storeState.globalState
    }

    override fun updateStoreState(storeState: DomainState, newHubState: DomainGlobalState): DomainState {
        return storeState.copy(globalState = newHubState)
    }

    override suspend fun handleAction(
        action: Action,
        storeState: DomainState,
        cancel: ValueCallback<Action>
    ) {
        if (action !is DomainGlobalAction) return

        val state = storeState.globalState

        when (action) {
        }
    }

    override fun reduceAction(action: Action, state: DomainGlobalState): DomainGlobalState = when (action) {
        is DomainGlobalAction.SetScanResponse -> {
            state.copy(scanResponse = action.scanResponse)
        }
        is DomainGlobalAction.ShowDialog -> {
            state.copy(dialog = action.stateDialog)
        }
        else -> state
    }
}