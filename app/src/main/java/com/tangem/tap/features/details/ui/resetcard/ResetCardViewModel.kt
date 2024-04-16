package com.tangem.tap.features.details.ui.resetcard

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import org.rekotlin.Store

internal class ResetCardViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?): ResetCardScreenState.ResetCardScreenContent {
        val descriptionText = state?.cardInfo
            ?.toResetCardDescriptionText()
            ?: TextReference.Str(value = "")

        val warningsToShow = buildList {
            add(ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS)

            if (state?.isShowPasswordResetRadioButton == true) {
                add(ResetCardScreenState.WarningsToReset.LOST_PASSWORD_RESTORE)
            }
        }

        return ResetCardScreenState.ResetCardScreenContent(
            accepted = state?.resetButtonEnabled ?: false,
            descriptionText = descriptionText,
            warningsToShow = warningsToShow,
            acceptCondition1Checked = state?.condition1Checked ?: false,
            acceptCondition2Checked = state?.condition2Checked ?: false,
            onAcceptCondition1ToggleClick = { store.dispatch(DetailsAction.ResetToFactory.AcceptCondition1(it)) },
            onAcceptCondition2ToggleClick = { store.dispatch(DetailsAction.ResetToFactory.AcceptCondition2(it)) },
            onResetButtonClick = { showLastWarningDialog() },
            lastWarningDialog = ResetCardScreenState.ResetCardScreenContent.LastWarningDialog(
                isShown = state?.isLastWarningDialogShown ?: false,
                onResetButtonClick = ::onLastWarningDialogResetClicked,
                onDismiss = ::onLastWarningDialogDismiss,
            ),
        )
    }

    private fun showLastWarningDialog() {
        store.dispatch(DetailsAction.ResetToFactory.LastWarningDialogVisibility(isShown = true))
    }

    private fun onLastWarningDialogResetClicked() {
        store.dispatch(DetailsAction.ResetToFactory.LastWarningDialogVisibility(isShown = false))
        store.dispatch(DetailsAction.ResetToFactory.Proceed)
    }

    private fun onLastWarningDialogDismiss() {
        store.dispatch(DetailsAction.ResetToFactory.LastWarningDialogVisibility(isShown = false))
    }
}
