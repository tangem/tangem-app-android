package com.tangem.tap.features.details.ui.resetcard.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.ResetCardUserCodeParams
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.getResetToFactoryDescription
import com.tangem.tap.features.details.ui.resetcard.ResetCardDialog
import com.tangem.tap.features.details.ui.resetcard.ResetCardScreenState
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class ResetCardModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val resetCardUseCase: ResetCardUseCase,
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val userWalletsListManager: UserWalletsListManager,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cardSettingsInteractor: CardSettingsInteractor,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<ResetCardComponent.Params>()

    // region Card-set specific data. All cards from single set have the same userWalletId and cardTypesResolver
    private val currentUserWalletId = params.userWalletId

    // Use only for card-specific data
    private val userWallet = getUserWalletUseCase(userWalletId = currentUserWalletId)
        .getOrElse { error("Failed to get user wallet: $it") }
        .requireColdWallet()

    private val currentCardTypesResolver = userWallet.cardTypesResolver
    private val currentUserCodeParams = ResetCardUserCodeParams(
        isAccessCodeSet = userWallet.scanResponse.card.isAccessCodeSet,
        isPasscodeSet = userWallet.scanResponse.card.isPasscodeSet,
    )
    // endregion

    // region Data of card that was scanned on CardSettings
    private val primaryCardId: String = params.cardId

    private val isActiveBackupPrimaryCard = params.isActiveBackupStatus

    private val primaryBackupCardsCount = params.backupCardsCount
    // endregion

    // TODO: move logic to separate domain entity
    private var resetBackupCardCount = 0

    val screenState: MutableStateFlow<ResetCardScreenState> = MutableStateFlow(
        value = getInitialState(),
    )

    private fun getInitialState(): ResetCardScreenState {
        val shouldShowResetPasswordButton = shouldShowResetPasswordButton()
        val warningsToShow = buildList {
            add(ResetCardScreenState.WarningsToReset.LOST_WALLET_ACCESS)

            if (shouldShowResetPasswordButton) {
                add(ResetCardScreenState.WarningsToReset.LOST_PASSWORD_RESTORE)
            }
        }

        return ResetCardScreenState(
            isResetButtonEnabled = false,
            descriptionText = getResetToFactoryDescription(
                isActiveBackupStatus = isActiveBackupPrimaryCard,
                typesResolver = currentCardTypesResolver,
            ),
            warningsToShow = warningsToShow,
            isResetPasswordButtonShown = shouldShowResetPasswordButton,
            isAcceptCondition1Checked = false,
            isAcceptCondition2Checked = false,
            onAcceptCondition1ToggleClick = ::toggleFirstCondition,
            onAcceptCondition2ToggleClick = ::toggleSecondCondition,
            onResetButtonClick = { showDialog(ResetCardDialog.StartResetDialog) },
            dialog = null,
        )
    }

    private fun shouldShowResetPasswordButton(): Boolean {
        val isTangemWallet = currentCardTypesResolver.isTangemWallet() || currentCardTypesResolver.isWallet2()

        return isTangemWallet && isActiveBackupPrimaryCard
    }

    private fun toggleFirstCondition(isAccepted: Boolean) {
        screenState.update { prevState ->
            val isResetButtonEnabled = if (prevState.isResetPasswordButtonShown) {
                isAccepted && prevState.isAcceptCondition2Checked
            } else {
                isAccepted
            }

            prevState.copy(
                isAcceptCondition1Checked = isAccepted,
                isResetButtonEnabled = isResetButtonEnabled,
            )
        }
    }

    private fun toggleSecondCondition(isAccepted: Boolean) {
        screenState.update { prevState ->
            val isResetButtonEnabled = prevState.isAcceptCondition1Checked && isAccepted

            prevState.copy(
                isAcceptCondition2Checked = isAccepted,
                isResetButtonEnabled = isResetButtonEnabled,
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
        modelScope.launch {
            resetCardUseCase(cardId = primaryCardId, params = currentUserCodeParams).onRight {
                deleteSavedAccessCodesUseCase(cardId = primaryCardId)
                val hasUserWallets = deleteWalletUseCase(userWalletId = currentUserWalletId).getOrElse { error ->
                    Timber.e("Unable to delete user wallet: $error")
                    return@launch
                }

                if (hasUserWallets) {
                    val newSelectedWallet = getSelectedWalletSyncUseCase().getOrElse { error ->
                        error("Failed to get selected wallet: $error")
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

        modelScope.launch {
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
        cardSettingsInteractor.clear()

        val newSelectedWallet = getSelectedWalletSyncUseCase.invoke().getOrNull()

        if (newSelectedWallet != null) {
            store.dispatchNavigationAction { popTo<AppRoute.Wallet>() }
        } else {
            if (hotWalletFeatureToggles.isHotWalletEnabled) {
                store.dispatchNavigationAction { replaceAll(AppRoute.Home()) }
            } else {
                val isLocked = runCatching { userWalletsListManager.asLockable()?.isLocked }.isSuccess
                if (isLocked && userWalletsListManager.hasUserWallets) {
                    store.dispatchNavigationAction { popTo<AppRoute.Welcome>() }
                } else {
                    store.dispatchNavigationAction { replaceAll(AppRoute.Home()) }
                }
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

        return primaryBackupCardsCount
    }
}