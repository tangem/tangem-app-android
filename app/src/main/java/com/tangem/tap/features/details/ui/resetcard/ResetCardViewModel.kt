package com.tangem.tap.features.details.ui.resetcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.analytics.events.Settings
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

@Suppress("LongParameterList")
@HiltViewModel
internal class ResetCardViewModel @Inject constructor(
    private val resetCardFeatureToggles: ResetCardFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val resetCardUseCase: ResetCardUseCase,
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    private val firstCardScanResponse = store.state.detailsState.cardSettingsState?.scanResponse
        ?: error("ScanResponse can't be null")

    private val currentUserWalletId = createUserWalletId(firstCardScanResponse)

    // TODO: move logic to separate domain entity
    private var resetBackupCardCount = 0

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
                    onDismiss = ::onInterruptedResetDialogDismiss,
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
            resetCardUseCase(card = firstCardScanResponse.card).onRight {
                deleteSavedAccessCodesUseCase(firstCardScanResponse.card.cardId)
                deleteWalletUseCase(currentUserWalletId)

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
                cardNumber = resetBackupCardCount + 1,
                card = firstCardScanResponse.card,
                userWalletId = currentUserWalletId,
            )
                .onRight {
                    resetBackupCardCount++

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

    private fun onInterruptedResetDialogDismiss() {
        analyticsEventHandler.send(Settings.CardSettings.FactoryResetCanceled(cardsCount = resetBackupCardCount + 1))

        dismissAndFinishFullReset()
    }

    private fun checkRemainingBackupCards() {
        val backupCardsCount = firstCardScanResponse.getBackupCardsCount()

        when {
            backupCardsCount > resetBackupCardCount -> showDialog(CardSettingsDialog.ContinueResetDialog)
            backupCardsCount == resetBackupCardCount -> {
                analyticsEventHandler.send(
                    event = Settings.CardSettings.FactoryResetFinished(cardsCount = resetBackupCardCount + 1),
                )
                showDialog(CardSettingsDialog.CompletedResetDialog)
            }
            else -> finishFullReset()
        }
    }

    private fun dismissAndFinishFullReset() {
        dismissDialog()

        finishFullReset()
    }

    private fun finishFullReset() {
        val newSelectedWallet = userWalletsListManager.selectedUserWalletSync

        if (newSelectedWallet != null) {
            store.dispatchNavigationAction { popTo<AppRoute.Wallet>() }
        } else {
            val isLocked = runCatching { userWalletsListManager.asLockable()?.isLockedSync }.isSuccess
            if (isLocked && userWalletsListManager.hasUserWallets) {
                store.dispatchNavigationAction { popTo<AppRoute.Welcome>() }
            } else {
                store.dispatchNavigationAction { popTo<AppRoute.Home>() }
            }
        }
    }

    private fun showDialog(dialog: CardSettingsDialog) {
        store.dispatch(ResetToFactory.ShowDialog(dialog))
    }

    private fun dismissDialog() {
        store.dispatch(ResetToFactory.DismissDialog)
    }

    private fun ScanResponse.getBackupCardsCount(): Int {
        if (!cardTypesResolver.isMultiwalletAllowed()) return 0

        return when (val status = card.backupStatus) {
            is CardDTO.BackupStatus.Active -> status.cardCount
            is CardDTO.BackupStatus.CardLinked,
            is CardDTO.BackupStatus.NoBackup,
            null,
            -> 0
        }
    }

    private fun createUserWalletId(scanResponse: ScanResponse): UserWalletId {
        return UserWalletIdBuilder.scanResponse(scanResponse).build()
            ?: error("UserWalletId can't be null")
    }
}