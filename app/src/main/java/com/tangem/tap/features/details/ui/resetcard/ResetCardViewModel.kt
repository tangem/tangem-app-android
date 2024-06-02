package com.tangem.tap.features.details.ui.resetcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.raise.either
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction.ResetToFactory
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.resetcard.featuretoggles.ResetCardFeatureToggles
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.tap.features.details.redux.CardSettingsState.Dialog as CardSettingsDialog

@HiltViewModel
internal class ResetCardViewModel @Inject constructor(
    private val resetCardFeatureToggles: ResetCardFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val resetCardUseCase: ResetCardUseCase,
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
) : ViewModel() {

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
            onAcceptCondition1ToggleClick = { store.dispatch(ResetToFactory.AcceptCondition1(it)) },
            onAcceptCondition2ToggleClick = { store.dispatch(ResetToFactory.AcceptCondition2(it)) },
            onResetButtonClick = { showDialog(CardSettingsDialog.StartResetDialog) },
            dialog = state?.dialog?.let(::createDialog),
        )
    }

    private fun createDialog(dialog: CardSettingsDialog): ResetCardScreenState.ResetCardScreenContent.Dialog {
        return when (dialog) {
            CardSettingsDialog.StartResetDialog -> {
                ResetCardScreenState.ResetCardScreenContent.Dialog.StartReset(
                    onConfirmClick = ::onStartResetClick,
                    onDismiss = ::dismissDialog,
                )
            }
            CardSettingsDialog.ContinueResetDialog -> {
                ResetCardScreenState.ResetCardScreenContent.Dialog.ContinueReset(
                    onConfirmClick = ::onContinueResetClick,
                    onDismiss = ::onContinueResetDialogDismiss,
                )
            }
            CardSettingsDialog.InterruptedResetDialog -> {
                ResetCardScreenState.ResetCardScreenContent.Dialog.InterruptedReset(
                    onConfirmClick = ::onContinueResetClick,
                    onDismiss = ::dismissAndFinishFullReset,
                )
            }
            CardSettingsDialog.CompletedResetDialog -> {
                ResetCardScreenState.ResetCardScreenContent.Dialog.CompletedReset(
                    onConfirmClick = ::dismissAndFinishFullReset,
                )
            }
        }
    }

    private fun onStartResetClick() {
        dismissDialog()

        if (resetCardFeatureToggles.isFullResetEnabled) {
            makeFullReset()
        } else {
            store.dispatch(ResetToFactory.Proceed)
        }
    }

    private fun makeFullReset() {
        val currentUserWallet = getSelectedWalletSyncUseCase().getOrNull() ?: return

        viewModelScope.launch {
            resetCurrentCard(userWallet = currentUserWallet).onRight {
                val newSelectedWallet = getSelectedWalletSyncUseCase().getOrNull()
                if (newSelectedWallet != null) {
                    store.onUserWalletSelected(newSelectedWallet)
                }
            }
        }
    }

    private fun onContinueResetClick() {
        dismissDialog()

        // TODO: [REDACTED_TASK_KEY]
    }

    private fun onContinueResetDialogDismiss() {
        dismissDialog()

        showDialog(CardSettingsDialog.InterruptedResetDialog)
    }

    private suspend fun resetCurrentCard(userWallet: UserWallet) = either {
        resetCardUseCase(userWallet.scanResponse.card).bind()
        deleteSavedAccessCodesUseCase(userWallet.cardId).bind()
        deleteWalletUseCase(userWallet.walletId).bind()
    }

    private fun dismissAndFinishFullReset() {
        dismissDialog()

        // TODO: [REDACTED_TASK_KEY]
    }

    private fun showDialog(dialog: CardSettingsDialog) {
        store.dispatch(ResetToFactory.ShowDialog(dialog))
    }

    private fun dismissDialog() {
        store.dispatch(ResetToFactory.DismissDialog)
    }
}