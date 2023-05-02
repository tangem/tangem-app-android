package com.tangem.tap.features.details.ui.cardsettings.coderecovery

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.AccessCodeRecoveryState
import com.tangem.tap.features.details.redux.DetailsAction
import org.rekotlin.Store

class AccessCodeRecoveryViewModel(val store: Store<AppState>) {

    fun updateState(state: AccessCodeRecoveryState?): AccessCodeRecoveryScreenState {
        // We shouldn't get to this screen here when this state is null
        return if (state == null) {
            AccessCodeRecoveryScreenState(
                enabledOnCard = false,
                enabledSelection = false,
                isSaveChangesEnabled = false,
                onSaveChangesClick = {},
                onOptionClick = {},
            )
        } else {
            AccessCodeRecoveryScreenState(
                enabledOnCard = state.enabledOnCard,
                enabledSelection = state.enabledSelection,
                isSaveChangesEnabled = state.enabledOnCard != state.enabledSelection,
                onSaveChangesClick = { store.dispatch(DetailsAction.AccessCodeRecovery.SaveChanges(it)) },
                onOptionClick = { store.dispatch(DetailsAction.AccessCodeRecovery.SelectOption(it)) },
            )
        }
    }
}
