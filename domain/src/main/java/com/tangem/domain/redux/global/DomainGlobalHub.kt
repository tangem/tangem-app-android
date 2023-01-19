package com.tangem.domain.redux.global

import android.webkit.ValueCallback
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.utils.RequestHeader
import com.tangem.domain.redux.BaseStoreHub
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.ReStoreReducer
import com.tangem.lib.auth.AuthProvider
import org.rekotlin.Action

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
// TODO: refactoring: is alias for the GlobalMiddleware and the GlobalReducer
internal class DomainGlobalHub : BaseStoreHub<DomainGlobalState>("DomainGlobalHub") {

    override fun getHubState(storeState: DomainState): DomainGlobalState {
        return storeState.globalState
    }

    override fun updateStoreState(storeState: DomainState, newHubState: DomainGlobalState): DomainState {
        return storeState.copy(globalState = newHubState)
    }

    override suspend fun handleAction(action: Action, storeState: DomainState, cancel: ValueCallback<Action>) {
        if (action !is DomainGlobalAction) return
    }

    override fun getReducer(): ReStoreReducer<DomainGlobalState> = DomainGlobalReducer()
}

private class DomainGlobalReducer : ReStoreReducer<DomainGlobalState> {

    override fun reduceAction(action: Action, state: DomainGlobalState): DomainGlobalState {
        return when (action) {
            is DomainGlobalAction.SaveScanNoteResponse -> {
                val card = action.scanResponse.card
                state.networkServices.tangemTechService.addAuthenticationHeader(
                    RequestHeader.AuthenticationHeader(
                        object : AuthProvider {
                            override fun getCardPublicKey(): String = card.cardPublicKey.toHexString()
                            override fun getCardId(): String = card.cardId
                        },
                    ),
                )
                state.copy(scanResponse = action.scanResponse)
            }
            is DomainGlobalAction.ShowDialog -> {
                state.copy(dialog = action.stateDialog)
            }
            else -> state
        }
    }
}
