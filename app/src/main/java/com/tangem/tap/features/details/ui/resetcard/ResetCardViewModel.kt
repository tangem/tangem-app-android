package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import org.rekotlin.Store

internal class ResetCardViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?): ResetCardScreenState {
        val descriptionText = state?.cardInfo
            ?.toResetCardDescriptionText()
            ?: TextReference.Str(value = "")

        return ResetCardScreenState(
            accepted = state?.resetButtonEnabled ?: false,
            descriptionText = descriptionText,
            acceptCondition1Checked = state?.condition1Checked ?: false,
            acceptCondition2Checked = state?.condition2Checked ?: false,
            onAcceptCondition1ToggleClick = { store.dispatch(DetailsAction.ResetToFactory.AcceptCondition1(it)) },
            onAcceptCondition2ToggleClick = { store.dispatch(DetailsAction.ResetToFactory.AcceptCondition2(it)) },
            onResetButtonClick = { store.dispatch(DetailsAction.ResetToFactory.Proceed) },
        )
    }
}