package com.tangem.tap.features.details.ui.resetcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction.ResetToFactory
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.resetcard.featuretoggles.ResetCardFeatureToggles
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import com.tangem.tap.store
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

    private val currentUserWallet = getSelectedWalletSyncUseCase().getOrNull()
        ?: error("Selected user wallet can't be null")

    // TODO: move logic to separate domain entity
    private val backupCardsCount = (currentUserWallet.getCardsCount() ?: 0) - 1
    private var resetCardsCount = 0

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
        viewModelScope.launch {
            resetCardUseCase(card = currentUserWallet.scanResponse.card).onRight {
                deleteSavedAccessCodesUseCase(currentUserWallet.cardId)
                deleteWalletUseCase(currentUserWallet.walletId)

                val newSelectedWallet = getSelectedWalletSyncUseCase().getOrNull()
                if (newSelectedWallet != null) {
                    store.onUserWalletSelected(newSelectedWallet)
                }

                delay(DELAY_SDK_DIALOG_CLOSE)

                checkRemainingBackupCards()
            }
        }
    }

    private fun onContinueResetClick() {
        dismissDialog()

        viewModelScope.launch {
            resetCardUseCase(
                cardNumber = resetCardsCount + 1,
                card = currentUserWallet.scanResponse.card,
                userWalletId = currentUserWallet.walletId,
            )
                .onRight {
                    resetCardsCount++

                    delay(DELAY_SDK_DIALOG_CLOSE)

                    checkRemainingBackupCards()
                }
                .onLeft { showDialog(CardSettingsDialog.InterruptedResetDialog) }
        }
    }

    private fun onContinueResetDialogDismiss() {
        dismissDialog()

        showDialog(CardSettingsDialog.InterruptedResetDialog)
    }

    private fun checkRemainingBackupCards() {
        when {
            backupCardsCount > resetCardsCount -> showDialog(CardSettingsDialog.ContinueResetDialog)
            backupCardsCount == resetCardsCount -> showDialog(CardSettingsDialog.CompletedResetDialog)
            else -> finishFullReset()
        }
    }

    private fun dismissAndFinishFullReset() {
        dismissDialog()

        finishFullReset()
    }

    private fun finishFullReset() {
        val newSelectedWallet = getSelectedWalletSyncUseCase().getOrNull()

        if (newSelectedWallet != null) {
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Wallet))
        } else {
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        }
    }

    private fun showDialog(dialog: CardSettingsDialog) {
        store.dispatch(ResetToFactory.ShowDialog(dialog))
    }

    private fun dismissDialog() {
        store.dispatch(ResetToFactory.DismissDialog)
    }
}