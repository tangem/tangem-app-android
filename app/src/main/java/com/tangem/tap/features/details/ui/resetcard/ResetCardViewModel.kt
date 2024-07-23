package com.tangem.tap.features.details.ui.resetcard

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.ResetCardUserCodeParams
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.details.redux.ResetCardDialog
import com.tangem.tap.features.details.ui.common.utils.getResetToFactoryDescription
import com.tangem.tap.store
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class ResetCardViewModel @Inject constructor(
    getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val resetCardUseCase: ResetCardUseCase,
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // region Card-set specific data. All cards from single set have the same userWalletId and cardTypesResolver
    private val currentUserWalletId = savedStateHandle.get<Bundle>(AppRoute.ResetToFactory.USER_WALLET_ID)
        ?.unbundle(UserWalletId.serializer())
        ?: error("UserWalletId must be provided for ResetCardViewModel")

    // Use only for card-specific data
    private val userWallet = getUserWalletUseCase(userWalletId = currentUserWalletId)
        .getOrElse { error("Failed to get user wallet: $it") }

    private val currentCardTypesResolver = userWallet.cardTypesResolver
    private val currentUserCodeParams = ResetCardUserCodeParams(
        isAccessCodeSet = userWallet.scanResponse.card.isAccessCodeSet,
        isPasscodeSet = userWallet.scanResponse.card.isPasscodeSet,
    )
    // endregion

    // region Data of card that was scanned on CardSettings
    private val primaryCardId: String
    private val primaryBackupStatus: CardDTO.BackupStatus?
    // endregion

    // TODO: move logic to separate domain entity
    private var resetBackupCardCount = 0

    val screenState: MutableStateFlow<ResetCardScreenState> = MutableStateFlow(
        value = getInitialState(),
    )

    init {
        val cardSpecificInfo = savedStateHandle.get<Bundle>(AppRoute.ResetToFactory.CARD_SPECIFIC_DATA)
            ?.unbundle(AppRoute.ResetToFactory.CardSpecificInfo.serializer())
            ?: error("CardSpecificData must be provided for ResetCardViewModel")

        primaryCardId = cardSpecificInfo.cardId
        primaryBackupStatus = cardSpecificInfo.backupStatus
    }

    private fun getInitialState(): ResetCardScreenState {
        val shouldShowResetPasswordButton = shouldShowResetPasswordButton()
        val warningsToShow = buildList {
            add(ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS)

            if (shouldShowResetPasswordButton) {
                add(ResetCardScreenState.WarningsToReset.LOST_PASSWORD_RESTORE)
            }
        }

        return ResetCardScreenState(
            resetButtonEnabled = false,
            descriptionText = getResetToFactoryDescription(
                backupStatus = primaryBackupStatus,
                typesResolver = currentCardTypesResolver,
            ),
            warningsToShow = warningsToShow,
            showResetPasswordButton = shouldShowResetPasswordButton,
            acceptCondition1Checked = false,
            acceptCondition2Checked = false,
            onAcceptCondition1ToggleClick = ::toggleFirstCondition,
            onAcceptCondition2ToggleClick = ::toggleSecondCondition,
            onResetButtonClick = { showDialog(ResetCardDialog.StartResetDialog) },
            dialog = null,
        )
    }

    private fun shouldShowResetPasswordButton(): Boolean {
        val isTangemWallet = currentCardTypesResolver.isTangemWallet() || currentCardTypesResolver.isWallet2()

        return isTangemWallet && primaryBackupStatus is CardDTO.BackupStatus.Active
    }

    private fun toggleFirstCondition(isAccepted: Boolean) {
        screenState.update { prevState ->
            val resetButtonEnabled = if (prevState.showResetPasswordButton) {
                isAccepted && prevState.acceptCondition2Checked
            } else {
                isAccepted
            }

            prevState.copy(
                acceptCondition1Checked = isAccepted,
                resetButtonEnabled = resetButtonEnabled,
            )
        }
    }

    private fun toggleSecondCondition(isAccepted: Boolean) {
        screenState.update { prevState ->
            val resetButtonEnabled = prevState.acceptCondition1Checked && isAccepted

            prevState.copy(
                acceptCondition2Checked = isAccepted,
                resetButtonEnabled = resetButtonEnabled,
            )
        }
    }

    private fun createDialog(dialog: ResetCardDialog): ResetCardScreenState.Dialog {
        return when (dialog) {
            ResetCardDialog.StartResetDialog -> {
                ResetCardScreenState.Dialog.StartReset(
                    onConfirmClick = ::onStartResetClick,
                    onDismiss = ::dismissDialog,
                )
            }
            ResetCardDialog.ContinueResetDialog -> {
                ResetCardScreenState.Dialog.ContinueReset(
                    onConfirmClick = ::onContinueResetClick,
                    onDismiss = ::onContinueResetDialogDismiss,
                )
            }
            ResetCardDialog.InterruptedResetDialog -> {
                ResetCardScreenState.Dialog.InterruptedReset(
                    onConfirmClick = ::onContinueResetClick,
                    onDismiss = ::onInterruptedResetDialogDismiss,
                )
            }
            ResetCardDialog.CompletedResetDialog -> {
                ResetCardScreenState.Dialog.CompletedReset(
                    onConfirmClick = ::dismissAndFinishFullReset,
                )
            }
        }
    }

    private fun onStartResetClick() {
        dismissDialog()

        makeFullReset()
    }

    private fun makeFullReset() {
        viewModelScope.launch {
            resetCardUseCase(cardId = primaryCardId, params = currentUserCodeParams).onRight {
                deleteSavedAccessCodesUseCase(cardId = primaryCardId)
                val hasUserWallets = deleteWalletUseCase(userWalletId = currentUserWalletId).getOrElse {
                    Timber.e("Unable to delete user wallet: $it")
                    return@launch
                }

                if (hasUserWallets) {
                    val newSelectedWallet = getSelectedWalletSyncUseCase().getOrElse {
                        error("Failed to get selected wallet: $it")
                    }

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
                params = currentUserCodeParams,
                userWalletId = currentUserWalletId,
            )
                .onRight { isResetCompleted ->
                    if (isResetCompleted) {
                        resetBackupCardCount++
                    }

                    delay(DELAY_SDK_DIALOG_CLOSE)

                    checkRemainingBackupCards()
                }
                .onLeft { showDialog(ResetCardDialog.InterruptedResetDialog) }
        }
    }

    private fun onContinueResetDialogDismiss() {
        dismissDialog()

        showDialog(ResetCardDialog.InterruptedResetDialog)
    }

    private fun onInterruptedResetDialogDismiss() {
        analyticsEventHandler.send(Settings.CardSettings.FactoryResetCanceled(cardsCount = resetBackupCardCount + 1))

        dismissAndFinishFullReset()
    }

    private fun checkRemainingBackupCards() {
        val backupCardsCount = getBackupCardsCount()

        when {
            backupCardsCount > resetBackupCardCount -> showDialog(ResetCardDialog.ContinueResetDialog)
            backupCardsCount == resetBackupCardCount -> {
                analyticsEventHandler.send(
                    event = Settings.CardSettings.FactoryResetFinished(cardsCount = resetBackupCardCount + 1),
                )
                showDialog(ResetCardDialog.CompletedResetDialog)
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
                store.dispatchNavigationAction { replaceAll(AppRoute.Home) }
            }
        }
    }

    private fun showDialog(dialog: ResetCardDialog) {
        screenState.update { it.copy(dialog = createDialog(dialog)) }
    }

    private fun dismissDialog() {
        screenState.update { it.copy(dialog = null) }
    }

    private fun getBackupCardsCount(): Int {
        if (!currentCardTypesResolver.isMultiwalletAllowed()) return 0

        return when (val status = primaryBackupStatus) {
            is CardDTO.BackupStatus.Active -> status.cardCount
            is CardDTO.BackupStatus.CardLinked,
            is CardDTO.BackupStatus.NoBackup,
            null,
            -> 0
        }
    }
}