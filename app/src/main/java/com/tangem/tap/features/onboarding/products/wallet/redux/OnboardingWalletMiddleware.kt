package com.tangem.tap.features.onboarding.products.wallet.redux

import android.net.Uri
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.ifNotNull
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.TapWorkarounds.canSkipBackup
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.Artwork
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.backup.BackupService
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.wallet.R
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

object OnboardingWalletMiddleware {
    val handler = onboardingWalletMiddleware
}

private val onboardingWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is OnboardingWallet2Action -> handleWallet2Action(action)
                else -> handleWalletAction(action)
            }
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun handleWalletAction(action: Action) {
    if (action !is OnboardingWalletAction) return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse
    val card = scanResponse?.card

    val onboardingWalletState = store.state.onboardingWalletState

    when (action) {
        OnboardingWalletAction.Init -> {
            ifNotNull(onboardingManager, card) { manager, card ->
                if (!manager.isActivationStarted(card.cardId)) {
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
                    store.dispatch(BackupAction.FinishBackup())
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
                    null -> {
                        // onboardingManager is null when backup started from previously interrupted
                        val primaryCardId = backupService.primaryCardId
                        val cardPublicKey = backupService.primaryPublicKey
                        if (primaryCardId != null && cardPublicKey != null) {
                            // uses when no scanResponse and backup state restored
                            loadArtworkForUnfinishedBackup(
                                cardId = primaryCardId,
                                cardPublicKey = cardPublicKey,
                                defaultArtwork = action.cardArtworkUriForUnfinishedBackup,
                            )
                        } else {
                            action.cardArtworkUriForUnfinishedBackup
                        }
                    }
                    else -> onboardingManager.loadArtworkUrl()
                        .takeIf { it != Artwork.DEFAULT_IMG_URL }
                        ?.let { Uri.parse(it) }
                }
                store.dispatchOnMain(OnboardingWalletAction.SetArtworkUrl(cardArtwork))
            }
        }
        is OnboardingWalletAction.CreateWallet -> {
            scanResponse ?: return
            scope.launch {
                val result = tangemSdkManager.createProductWallet(
                    scanResponse,
                    globalState.onboardingState.shouldResetOnCreate,
                )
                store.dispatchOnMain(OnboardingWalletAction.WalletWasCreated(true, result))
            }
        }
        is OnboardingWalletAction.WalletWasCreated -> {
            scanResponse ?: return
            when (val result = action.result) {
                is CompletionResult.Success -> {
                    if (action.shouldSendAnalyticsEvent) {
                        Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                    }
                    // here we must use updated scanResponse after createWallet & derivation
                    val updatedResponse = globalState.onboardingState.onboardingManager.scanResponse.copy(
                        card = result.data.card,
                        derivedKeys = result.data.derivedKeys,
                        primaryCard = result.data.primaryCard,
                    )
                    onboardingManager.scanResponse = updatedResponse

                    store.dispatch(GlobalAction.Onboarding.ShouldResetCardOnCreate(false))
                    startCardActivation(updatedResponse)
                    store.dispatch(OnboardingWalletAction.ResumeBackup)
                }
                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.WalletAlreadyCreated) {
                        handleActivationError()
                    }
                }
            }
        }
        is OnboardingWalletAction.FinishOnboarding -> {
            store.dispatch(GlobalAction.Onboarding.Stop)

            if (scanResponse == null) {
                store.dispatch(NavigationAction.PopBackTo())
                store.dispatch(HomeAction.ReadCard(scope = action.scope))
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
                        Analytics.send(Onboarding.Backup.ScreenOpened())
                        BackupAction.IntroduceBackup
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

private suspend fun loadArtworkForUnfinishedBackup(
    cardId: String,
    cardPublicKey: ByteArray,
    defaultArtwork: Uri?,
): Uri {
    return when (val cardInfo = OnlineCardVerifier().getCardInfo(cardId, cardPublicKey)) {
        is Result.Success -> {
            val artworkId = cardInfo.data.artwork?.id
            if (artworkId.isNullOrEmpty()) {
                defaultArtwork ?: Uri.EMPTY
            } else {
                Uri.parse(OnlineCardVerifier.getUrlForArtwork(cardId, cardPublicKey.toHexString(), artworkId))
            }
        }
        is Result.Failure -> defaultArtwork ?: Uri.EMPTY
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun handleWallet2Action(action: OnboardingWallet2Action) {
    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse

    when (action) {
        is OnboardingWallet2Action.Init -> {
            if (scanResponse?.cardTypesResolver?.isWallet2() == true) {
                store.dispatch(OnboardingWallet2Action.SetDependencies(action.maxProgress))
            }
        }

        is OnboardingWallet2Action.CreateWallet -> {
            scanResponse ?: return
            scope.launch {
                val mediateResult = when (
                    val result = tangemSdkManager.createProductWallet(
                        scanResponse,
                        globalState.onboardingState.shouldResetOnCreate,
                    )
                ) {
                    is CompletionResult.Success -> {
                        Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                        store.dispatch(GlobalAction.Onboarding.ShouldResetCardOnCreate(false))
                        val response = CreateWalletResponse(
                            card = result.data.card,
                            derivedKeys = result.data.derivedKeys,
                            primaryCard = result.data.primaryCard,
                        )
                        CompletionResult.Success(response)
                    }

                    is CompletionResult.Failure -> {
                        if (result.error is TangemSdkError.WalletAlreadyCreated) {
                            handleActivationError()
                        }
                        CompletionResult.Failure(result.error)
                    }
                }
                withMainContext {
                    action.callback(mediateResult)
                }
            }
        }

        is OnboardingWallet2Action.ImportWallet -> {
            scanResponse ?: return
            scope.launch {
                val mediateResult = when (
                    val result = tangemSdkManager.importWallet(
                        scanResponse = scanResponse,
                        mnemonic = action.mnemonicComponents.joinToString(" "),
                        shouldReset = globalState.onboardingState.shouldResetOnCreate,
                    )
                ) {
                    is CompletionResult.Success -> {
                        val creationType = when (action.seedPhraseSource) {
                            SeedPhraseSource.IMPORTED -> AnalyticsParam.WalletCreationType.SeedImport
                            SeedPhraseSource.GENERATED -> AnalyticsParam.WalletCreationType.NewSeed
                        }
                        Analytics.send(
                            event = Onboarding.CreateWallet.WalletCreatedSuccessfully(
                                creationType = creationType,
                                seedPhraseLength = action.mnemonicComponents.size,
                            ),
                        )
                        store.dispatch(GlobalAction.Onboarding.ShouldResetCardOnCreate(false))
                        val response = CreateWalletResponse(
                            card = result.data.card,
                            derivedKeys = result.data.derivedKeys,
                            primaryCard = result.data.primaryCard,
                        )
                        CompletionResult.Success(response)
                    }

                    is CompletionResult.Failure -> {
                        if (result.error is TangemSdkError.WalletAlreadyCreated) {
                            handleActivationError()
                        }
                        CompletionResult.Failure(result.error)
                    }
                }
                withMainContext {
                    action.callback(mediateResult)
                }
            }
        }

        is OnboardingWallet2Action.WalletWasCreated -> {
            val result = when (val mediateResult = action.result) {
                is CompletionResult.Success -> {
                    val response = CreateProductWalletTaskResponse(
                        card = mediateResult.data.card,
                        derivedKeys = mediateResult.data.derivedKeys,
                        primaryCard = mediateResult.data.primaryCard,
                    )
                    CompletionResult.Success(response)
                }

                is CompletionResult.Failure -> {
                    CompletionResult.Failure(mediateResult.error)
                }
            }
            // do not send analytics event for wallet2 flow, cause its already sent
            store.dispatchOnMain(OnboardingWalletAction.WalletWasCreated(false, result))
        }

        else -> Unit
    }
}

private fun handleActivationError() {
    store.dispatchDialogShow(
        OnboardingDialog.WalletActivationError(
            onConfirm = {
                store.dispatch(GlobalAction.Onboarding.ShouldResetCardOnCreate(true))
            },
        ),
    )
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
            val iconScanRes = if (scanResponse?.cardTypesResolver?.isRing() == true) {
                R.drawable.img_hand_scan_ring
            } else {
                null
            }
            backupService.readPrimaryCard(iconScanRes = iconScanRes, cardId = card?.cardId) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.StartAddingBackupCards)
                    }
                    is CompletionResult.Failure -> Unit
                }
            }
        }
        is BackupAction.AddBackupCard -> {
            backupService.addBackupCard { result ->
                backupService.skipCompatibilityChecks = false
                store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository).sdk
                    .config.filter.cardIdFilter = null

                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.AddBackupCard.Success)
                    }

                    is CompletionResult.Failure -> {
                        when (val error = result.error) {
                            is TangemSdkError.BackupFailedNotEmptyWallets -> {
                                if (card?.canSkipBackup == false) {
                                    store.dispatchOnMain(
                                        GlobalAction.ShowDialog(
                                            BackupDialog.ResetBackupCard(error.cardId),
                                        ),
                                    )
                                }
                            }
                            is TangemSdkError.IssuerSignatureLoadingFailed -> {
                                store.dispatchOnMain(
                                    GlobalAction.ShowDialog(BackupDialog.AttestationFailed),
                                )
                            }
                            else -> Unit
                        }
                    }
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
                store.dispatch(BackupAction.PrepareToWritePrimaryCard)
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(AccessCodeError.CodesDoNotMatch))
            }
        }
        is BackupAction.WritePrimaryCard -> {
            val iconScanRes = if (scanResponse?.cardTypesResolver?.isRing() == true) {
                R.drawable.img_hand_scan_ring
            } else {
                null
            }
            backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
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
                ),
            )
            store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
        }
        is BackupAction.SkipBackup -> {
            Analytics.send(Onboarding.Backup.Skipped())

            Analytics.send(Onboarding.Finished())
            finishCardActivation(gatherCardIds(backupState, card))
        }
        is BackupAction.FinishBackup -> {
            if (action.withAnalytics) {
                Analytics.send(Onboarding.Backup.Finished(backupState.backupCardsNumber))
            }

            if (scanResponse != null) {
                scope.launch {
                    val userWallet = UserWalletBuilder(scanResponse)
                        .backupCardsIds(backupState.backupCardIds.toSet())
                        .build()
                        .guard {
                            Timber.e("User wallet not created")
                            return@launch
                        }

                    userWalletsListManager.update(
                        userWalletId = userWallet.walletId,
                        update = { wallet ->
                            wallet.copy(
                                scanResponse = updateScanResponseAfterBackup(
                                    scanResponse = wallet.scanResponse,
                                    backupState = backupState,
                                ),
                            )
                        },
                    )
                    store.dispatchOnMain(GlobalAction.UpdateUserWalletsListManager(userWalletsListManager))
                }
            }

            val notActivatedCardIds = gatherCardIds(backupState, card)
                .mapNotNull { if (cardActivationIsFinished(it)) null else it }

            // All cardIds may already be activated if the backup was skipped before.
            if (notActivatedCardIds.isEmpty()) return

            Analytics.send(Onboarding.Finished())
            finishCardActivation(notActivatedCardIds)
        }

        is BackupAction.ResetBackupCard -> {
            scope.launch { tangemSdkManager.resetToFactorySettings(action.cardId, false) }
        }

        else -> Unit
    }
}

private fun cardActivationIsFinished(cardId: String): Boolean {
    return preferencesStorage.usedCardsPrefStorage.isActivationFinished(cardId)
}

/**
 * Standard Wallet cards start activation at OnboardingWalletAction.CreateWallet
 */
private fun startCardActivation(scanResponse: ScanResponse) {
    preferencesStorage.usedCardsPrefStorage.activationStarted(scanResponse.card.cardId)
}

/**
 * Standard Wallet cards finish activation at BackupAction.SkipBackup and BackupAction.FinishBackup
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

private fun finishCardsActivationForDiscardedUnfinishedBackup(cardId: String) {
    preferencesStorage.usedCardsPrefStorage.activationFinished(cardId)
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
            OnboardingHelper.onInterrupted()
            store.dispatch(NavigationAction.PopBackTo())
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
