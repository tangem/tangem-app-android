package com.tangem.domain.redux.global

import android.webkit.ValueCallback
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader
import com.tangem.domain.redux.BaseStoreHub
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.ReStoreReducer
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
// [REDACTED_TODO_COMMENT]
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
            else -> state
        }
    }
}
