package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.wallet.R
import org.rekotlin.Store

class ResetCardViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?): ResetCardScreenState {
        return ResetCardScreenState(
            descriptionResId = if (state?.card?.backupStatus?.isActive == true) {
                R.string.reset_card_with_backup_to_factory_message
            } else {
                R.string.reset_card_without_backup_to_factory_message
            },
            accepted = state?.resetConfirmed ?: false,
            onAcceptWarningToggleClick = { store.dispatch(DetailsAction.ResetToFactory.Confirm(it)) },
            onResetButtonClick = { store.dispatch(DetailsAction.ResetToFactory.Proceed) },
        )
    }
}
