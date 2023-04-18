package com.tangem.tap.features.onboarding.products.wallet.redux

import android.net.Uri
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.ifNotNull
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.isSaltPay
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.backup.BackupService
import com.tangem.tap.backupService
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.primaryCardIsSaltPayVisa
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManagerFactory
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.wallet.models.toCurrencies
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userTokensRepository
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

object OnboardingWalletMiddleware {
    val handler = onboardingWalletMiddleware
}

private val onboardingWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleWalletAction(action, state, dispatch)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun handleWalletAction(action: Action, state: () -> AppState?, dispatch: DispatchFunction) {
    if (action !is OnboardingWalletAction) return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse
    val card = scanResponse?.card

    val onboardingWalletState = store.state.onboardingWalletState

    when (action) {
        OnboardingWalletAction.Init -> {
            ifNotNull(onboardingManager, card) { manager, card ->
                if (!card.isSaltPay && !manager.isActivationStarted(card.cardId)) {
                    Analytics.send(Onboarding.Started())
                }
            }
            when {
                card == null -> {
                    // it's possible when found unfinished backup
                    store.dispatch(OnboardingWalletAction.ResumeBackup)
                }
                card.wallets.isNotEmpty() && card.backupStatus == CardDTO.BackupStatus.NoBackup -> {
                    store.dispatch(OnboardingWalletAction.ResumeBackup)
                }
                card.wallets.isNotEmpty() && card.backupStatus?.isActive == true -> {
                    when {
                        // check for unfinished backup for saltPay cards. See more
                        scanResponse.cardTypesResolver.isSaltPay() && backupService.hasIncompletedBackup -> {
                            store.dispatch(OnboardingWalletAction.ResumeBackup)
                        }

                        scanResponse.cardTypesResolver.isSaltPay() -> {
                            store.dispatch(OnboardingWalletAction.GetToSaltPayStep)
                            store.dispatch(BackupAction.FinishBackup(withAnalytics = false))
                            store.dispatch(OnboardingSaltPayAction.OnSwitchedToSaltPayProcess)
                        }

                        else -> store.dispatch(BackupAction.FinishBackup())
                    }
                }
                else -> {
                    store.dispatch(OnboardingWalletAction.GetToCreateWalletStep)
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                }
            }
        }
        is OnboardingWalletAction.LoadArtwork -> {
            scope.launch {
                val cardArtwork = when (onboardingManager) {
                    null -> action.cardArtworkUriForUnfinishedBackup
                    else -> onboardingManager.loadArtworkUrl()
                        .takeIf { it != Artwork.DEFAULT_IMG_URL }
                        ?.let { Uri.parse(it) }
                }
                store.dispatchOnMain(OnboardingWalletAction.SetArtworkUrl(cardArtwork))
            }
        }
        is OnboardingWalletAction.CreateWallet -> {
            scope.launch {
                scanResponse ?: return@launch
                val result = tangemSdkManager.createProductWallet(scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            // here we must use updated scanResponse after createWallet & derivation
                            val updatedResponse = globalState.onboardingState.onboardingManager.scanResponse.copy(
                                card = result.data.card,
                                derivedKeys = result.data.derivedKeys,
                                primaryCard = result.data.primaryCard,
                            )
                            onboardingManager.scanResponse = updatedResponse
                            store.state.globalState.topUpController?.registerEmptyWallet(updatedResponse)

                            val blockchainNetworks = if (DemoHelper.isDemoCardId(result.data.card.cardId)) {
                                DemoHelper.config.demoBlockchains
                            } else {
                                listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
                            }.map { blockchain ->
                                BlockchainNetwork(blockchain, result.data.card)
                            }

                            scope.launch {
                                userTokensRepository.saveUserTokens(
                                    card = result.data.card,
                                    tokens = blockchainNetworks.toCurrencies(),
                                )
                            }
                            startCardActivation(updatedResponse)
                            store.dispatch(OnboardingWalletAction.ResumeBackup)
                        }
                        is CompletionResult.Failure -> Unit
                    }
                }
            }
        }
        OnboardingWalletAction.FinishOnboarding -> {
            store.dispatch(GlobalAction.Onboarding.Stop)

            if (scanResponse == null) {
                store.dispatch(NavigationAction.PopBackTo())
                store.dispatch(HomeAction.ReadCard())
            } else {
                val backupState = store.state.onboardingWalletState.backupState
                val updatedScanResponse = updateScanResponseAfterBackup(scanResponse, backupState)
                OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(
                    scanResponse = updatedScanResponse,
                    accessCode = backupState.accessCode,
                    backupCardsIds = backupState.backupCardIds,
                )
            }
        }
        is OnboardingWalletAction.ResumeBackup -> {
            val newAction = when (val backupState = backupService.currentState) {
                BackupService.State.FinalizingPrimaryCard -> BackupAction.PrepareToWritePrimaryCard
                is BackupService.State.FinalizingBackupCard -> BackupAction.PrepareToWriteBackupCard(backupState.index)
                else -> {
                    if (onboardingWalletState.backupState.backupStep == BackupStep.InitBackup ||
                        onboardingWalletState.backupState.backupStep == BackupStep.Finished
                    ) {
                        if (onboardingWalletState.isSaltPay) {
                            BackupAction.StartBackup
                        } else {
                            Analytics.send(Onboarding.Backup.ScreenOpened())
                            BackupAction.IntroduceBackup
                        }
                    } else {
                        null
                    }
                }
            }
            newAction?.let { store.dispatch(it) }
        }
        OnboardingWalletAction.OnBackPressed -> handleOnBackPressed(onboardingWalletState)
        else -> Unit
    }
}

private fun updateScanResponseAfterBackup(scanResponse: ScanResponse, backupState: BackupState): ScanResponse {
    val card = if (backupState.backupCardsNumber > 0) {
        val cardsCount = backupState.backupCardsNumber
        scanResponse.card.copy(
            backupStatus = CardDTO.BackupStatus.Active(cardCount = cardsCount),
            isAccessCodeSet = true,
        )
    } else {
        scanResponse.card
    }
    return scanResponse.copy(card = card)
}

class BackupMiddleware {
    val backupMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                if (action is BackupAction) handleBackupAction(state, action)
                next(action)
            }
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handleBackupAction(appState: () -> AppState?, action: BackupAction) {
    if (DemoHelper.tryHandle(appState, action)) return
    val globalState = appState()?.globalState ?: return
    val onboardingWalletState = appState()?.onboardingWalletState ?: return

    val backupState = onboardingWalletState.backupState
    val scanResponse = globalState.onboardingState.onboardingManager?.scanResponse
    val card = scanResponse?.card

    when (action) {
        is BackupAction.StartBackup -> {
            Analytics.send(Onboarding.Backup.Started())
            backupService.discardSavedBackup()
            val primaryCard = scanResponse?.primaryCard
            if (primaryCard != null) {
                backupService.setPrimaryCard(primaryCard)
                store.dispatch(BackupAction.StartAddingBackupCards)
            } else {
                store.dispatch(BackupAction.StartAddingPrimaryCard)
            }
        }
        is BackupAction.ScanPrimaryCard -> {
            backupService.readPrimaryCard(cardId = card?.cardId) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.StartAddingBackupCards)
                    }
                    is CompletionResult.Failure -> Unit
                }
            }
        }
        is BackupAction.AddBackupCard -> {
            onboardingWalletState.backupCardIdFilter?.let {
                backupService.skipCompatibilityChecks = true
                tangemSdk.config.filter.cardIdFilter = it
            }

            backupService.addBackupCard { result ->
                backupService.skipCompatibilityChecks = false
                tangemSdk.config.filter.cardIdFilter = null
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.AddBackupCard.Success)
                    }
                    is CompletionResult.Failure -> Unit
                }
            }
        }
        is BackupAction.FinishAddingBackupCards -> {
            if (backupService.addedBackupCardsCount == backupState.maxBackupCards) {
                store.dispatchOnMain(BackupAction.ShowAccessCodeInfoScreen)
            } else {
                store.dispatchOnMain(GlobalAction.ShowDialog(BackupDialog.AddMoreBackupCards))
            }
        }
        is BackupAction.ShowEnterAccessCodeScreen -> {
            Analytics.send(Onboarding.Backup.SettingAccessCodeStarted())
        }
        is BackupAction.CheckAccessCode -> {
            Analytics.send(Onboarding.Backup.AccessCodeEntered())
            if (action.accessCode.length < 4) {
                store.dispatch(BackupAction.SetAccessCodeError(AccessCodeError.CodeTooShort))
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(null))
                store.dispatch(BackupAction.SaveFirstAccessCode(action.accessCode))
            }
        }
        is BackupAction.SaveAccessCodeConfirmation -> {
            Analytics.send(Onboarding.Backup.AccessCodeReEntered())
            if (action.accessCodeConfirmation == backupState.accessCode) {
                store.dispatch(BackupAction.SetAccessCodeError(null))
                backupService.setAccessCode(action.accessCodeConfirmation)
                store.dispatch(OnboardingSaltPayAction.SetAccessCode(action.accessCodeConfirmation))
                store.dispatch(BackupAction.PrepareToWritePrimaryCard)
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(AccessCodeError.CodesDoNotMatch))
            }
        }
        is BackupAction.WritePrimaryCard -> {
            backupService.proceedBackup { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(1))
                    }
                    is CompletionResult.Failure -> Unit
                }
            }
        }
        is BackupAction.WriteBackupCard -> {
            backupService.proceedBackup { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        if (backupService.currentState == BackupService.State.Finished) {
                            initSaltPayOnBackupFinishedIfNeeded(scanResponse, onboardingWalletState)
                            store.dispatchOnMain(BackupAction.FinishBackup())
                        } else {
                            store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(action.cardNumber + 1))
                        }
                    }
                    is CompletionResult.Failure -> Unit
                }
            }
        }
        is BackupAction.DiscardBackup -> {
            backupService.discardSavedBackup()
        }
        is BackupAction.DiscardSavedBackup -> {
            backupService.primaryCardId?.let {
                Analytics.send(Onboarding.Finished())
                finishCardsActivationForDiscardedUnfinishedBackup(it)
            }
            backupService.discardSavedBackup()
        }
        is BackupAction.CheckForUnfinishedBackup -> {
            if (backupService.hasIncompletedBackup) {
                store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound))
            }
        }
        is BackupAction.ResumeFoundUnfinishedBackup -> {
            store.dispatch(
                GlobalAction.Onboarding.StartForUnfinishedBackup(
                    addedBackupCardsCount = backupService.addedBackupCardsCount,
                    isSaltPayVisa = backupService.primaryCardIsSaltPayVisa(),
                ),
            )
            store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
        }
        is BackupAction.SkipBackup -> {
            Analytics.send(Onboarding.Backup.Skipped())
            if (onboardingWalletState.isSaltPay) return

            Analytics.send(Onboarding.Finished())
            finishCardActivation(gatherCardIds(backupState, card))
        }
        is BackupAction.FinishBackup -> {
            if (action.withAnalytics) {
                Analytics.send(Onboarding.Backup.Finished(backupState.backupCardsNumber))
            }
            if (onboardingWalletState.isSaltPay) return

            val notActivatedCardIds = gatherCardIds(backupState, card)
                .mapNotNull { if (cardActivationIsFinished(it)) null else it }

            // All cardIds may already be activated if the backup was skipped before.
            if (notActivatedCardIds.isEmpty()) return

            Analytics.send(Onboarding.Finished())
            finishCardActivation(notActivatedCardIds)
        }
        else -> Unit
    }
}

private fun cardActivationIsFinished(cardId: String): Boolean {
    return preferencesStorage.usedCardsPrefStorage.isActivationFinished(cardId)
}

/**
 * Standard Wallet cards start activation at OnboardingWalletAction.CreateWallet
 * SaltPay cards start activation at OnboardingWalletAction.StartSaltPayCardActivation
 */
private fun startCardActivation(scanResponse: ScanResponse) {
    preferencesStorage.usedCardsPrefStorage.activationStarted(scanResponse.card.cardId)
}

/**
 * Standard Wallet cards finish activation at BackupAction.SkipBackup and BackupAction.FinishBackup
 * SaltPay cards finish activation at OnboardingWalletAction.FinishOnboarding
 */
internal fun finishCardActivation(cardIds: List<String>) {
    cardIds.forEach { cardId ->
        preferencesStorage.usedCardsPrefStorage.activationFinished(cardId)
    }
}

internal fun gatherCardIds(backupState: BackupState, card: CardDTO?): List<String> {
    return (listOf(backupState.primaryCardId, card?.cardId) + backupState.backupCardIds)
        .filterNotNull()
        .distinct()
}

/**
 * Not used for SaltPay cards, because discarding unfinished backup is not possible
 */
private fun finishCardsActivationForDiscardedUnfinishedBackup(cardId: String) {
    preferencesStorage.usedCardsPrefStorage.activationFinished(cardId)
}

/**
 * SaltPay state maybe not initialized if backup resumed from ResumeFoundUnfinishedBackup action
 */
private fun initSaltPayOnBackupFinishedIfNeeded(scanResponse: ScanResponse?, state: OnboardingWalletState) {
    if (state.backupState.isInterruptedBackup) return
    if (scanResponse == null) return

    if (state.isSaltPay && state.onboardingSaltPayState == null) {
        val manager = SaltPayActivationManagerFactory(
            blockchain = scanResponse.cardTypesResolver.getBlockchain(),
            card = scanResponse.card,
        ).create()
        store.dispatchOnMain(OnboardingSaltPayAction.SetDependencies(manager))
        store.dispatchOnMain(OnboardingSaltPayAction.Update(withAnalytics = false))
        store.dispatchOnMain(OnboardingSaltPayAction.OnSwitchedToSaltPayProcess)
    }
}

private fun handleOnBackPressed(state: OnboardingWalletState) {
    when (state.backupState.backupStep) {
        BackupStep.InitBackup, BackupStep.ScanOriginCard, BackupStep.AddBackupCards, BackupStep.EnterAccessCode,
        BackupStep.ReenterAccessCode, BackupStep.SetAccessCode, BackupStep.WritePrimaryCard,
        -> {
            showInterruptOnboardingDialog()
        }
        is BackupStep.WriteBackupCard -> {
            store.dispatch(GlobalAction.ShowDialog(BackupDialog.BackupInProgress))
        }
        BackupStep.Finished -> {
            if (state.isSaltPay) {
                showInterruptOnboardingDialog()
            } else {
                OnboardingHelper.onInterrupted()
                store.dispatch(NavigationAction.PopBackTo())
            }
        }
    }
}

private fun showInterruptOnboardingDialog() {
    store.dispatchDialogShow(
        OnboardingDialog.InterruptOnboarding(
            onOk = {
                OnboardingHelper.onInterrupted()
                store.dispatch(BackupAction.DiscardBackup)
                store.dispatch(NavigationAction.PopBackTo())
            },
        ),
    )
}
