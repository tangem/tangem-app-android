package com.tangem.domain.redux.global

import android.webkit.ValueCallback
import com.tangem.common.extensions.toHexString
import com.tangem.domain.redux.BaseStoreHub
import com.tangem.domain.redux.DomainState
import com.tangem.network.common.CardPublicKeyHttpInterceptor
import org.rekotlin.Action

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
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
        is DomainGlobalAction.SaveScanNoteResponse -> {
            val cardPublicKeyHex = action.scanResponse.card.cardPublicKey.toHexString()
            state.networkServices.tangemTechService.addHeaderInterceptors(
                listOf(CardPublicKeyHttpInterceptor(cardPublicKeyHex))
            )
            state.copy(scanResponse = action.scanResponse)
        }
        is DomainGlobalAction.ShowDialog -> {
            state.copy(dialog = action.stateDialog)
        }
        else -> state
    }
}