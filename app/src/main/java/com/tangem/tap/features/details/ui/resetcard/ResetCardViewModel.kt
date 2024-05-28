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
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.details.ui.resetcard.featuretoggles.ResetCardFeatureToggles
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

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

        if (resetCardFeatureToggles.isFullResetEnabled) {
            makeFullReset()
        } else {
            store.dispatch(DetailsAction.ResetToFactory.Proceed)
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

    private suspend fun resetCurrentCard(userWallet: UserWallet) = either {
        resetCardUseCase(userWallet.scanResponse.card).bind()
        deleteSavedAccessCodesUseCase(userWallet.cardId).bind()
        deleteWalletUseCase(userWallet.walletId).bind()
    }

    private fun onLastWarningDialogDismiss() {
        store.dispatch(DetailsAction.ResetToFactory.LastWarningDialogVisibility(isShown = false))
    }
}
