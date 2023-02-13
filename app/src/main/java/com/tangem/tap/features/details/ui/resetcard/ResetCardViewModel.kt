package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import org.rekotlin.Store

class ResetCardViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?): ResetCardScreenState {
        val descriptionText = state?.cardInfo
            ?.toResetCardDescriptionText()
            ?: TextReference.Str(value = "")

        return ResetCardScreenState(
            accepted = state?.resetConfirmed ?: false,
            descriptionText = descriptionText,
            onAcceptWarningToggleClick = { store.dispatch(DetailsAction.ResetToFactory.Confirm(it)) },
            onResetButtonClick = { store.dispatch(DetailsAction.ResetToFactory.Proceed) },
        )
    }
}
