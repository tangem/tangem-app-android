package com.tangem.tap.features.details.ui.securitymode

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.ManageSecurityState
import com.tangem.tap.features.details.redux.SecurityOption
import org.rekotlin.Store

class SecurityModeViewModel(val store: Store<AppState>) {

    fun updateState(state: ManageSecurityState?): SecurityModeScreenState {
        if (state == null) {
            return SecurityModeScreenState(
                availableOptions = emptyList(),
                selectedSecurityMode = SecurityOption.LongTap,
                isSaveChangesEnabled = false,
                onNewModeSelected = {},
                onSaveChangesClicked = {},
            )
        }
        return SecurityModeScreenState(
            availableOptions = state.allowedOptions.toList(),
            selectedSecurityMode = state.selectedOption,
            isSaveChangesEnabled = state.selectedOption != state.currentOption,
            onNewModeSelected = { store.dispatch(DetailsAction.ManageSecurity.SelectOption(it)) },
            onSaveChangesClicked = { store.dispatch(DetailsAction.ManageSecurity.SaveChanges) },
        )
    }
}
