package com.tangem.tap.common.redux.global

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import org.rekotlin.Middleware

val globalMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            when (action) {
                is GlobalAction.RestoreAppCurrency -> {
                    store.dispatch(GlobalAction.RestoreAppCurrency.Success(
                            preferencesStorage.getAppCurrency()
                    ))
                }
                is GlobalAction.HideWarningMessage -> {
                    store.state.globalState.warningManager?.let {
                        if (it.hideWarning(action.warning)) {
                            if (WarningMessagesManager.isAlreadySignedHashesWarning(action.warning)) {
                                //TODO: No appropriate warningMessage identification. Make it better later
                                store.dispatch(WalletAction.SaveCardId)
                            }

                            store.dispatch(WalletAction.SetWarnings(it.getWarnings(WarningMessage.Location.MainScreen)))
                            store.dispatch(SendAction.SetWarnings(it.getWarnings(WarningMessage.Location.SendScreen)))
                        }
                    }
                }
            }
            nextDispatch(action)
        }
    }
}