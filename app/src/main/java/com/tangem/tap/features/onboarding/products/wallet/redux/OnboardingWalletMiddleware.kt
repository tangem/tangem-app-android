package com.tangem.tap.features.onboarding.products.wallet.redux

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ifNotNull
import com.tangem.common.extensions.toHexString
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.common.extensions.withIOContext
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.CardDTO.Companion.RING_BATCH_IDS
import com.tangem.domain.models.scan.CardDTO.Companion.RING_BATCH_PREFIX
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.Artwork
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.feature.wallet.presentation.wallet.domain.BackupValidator
import com.tangem.operations.attestation.OnlineCardVerifier
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.wallet.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

object OnboardingWalletMiddleware {
    val handler = onboardingWalletMiddleware
}

private const val HIDE_PROGRESS_DELAY = 400L

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
            ifNotNull(onboardingManager, card) { manager, notNullCard ->
                mainScope.launch {
                    if (!manager.isActivationStarted(notNullCard.cardId)) {
                        Analytics.send(Onboarding.Started())
                    }
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
                            loadArtworkForCard(
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
                store.dispatchOnMain(OnboardingWalletAction.SetPrimaryCardArtworkUrl(cardArtwork))
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
                    mainScope.launch {
                        onboardingManager.startActivation(updatedResponse.card.cardId)
                        store.dispatchWithMain(OnboardingWalletAction.ResumeBackup)
                    }
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
            navigateToWalletScreen()
            store.dispatch(BackupAction.DiscardBackup)
        }
        is OnboardingWalletAction.ResumeBackup -> {
            val newAction = when (val backupState = backupService.currentState) {
                BackupService.State.FinalizingPrimaryCard -> BackupAction.PrepareToWritePrimaryCard
                is BackupService.State.FinalizingBackupCard -> BackupAction.PrepareToWriteBackupCard(backupState.index)
                else -> {
                    if (onboardingWalletState.backupState.backupStep == null ||
                        onboardingWalletState.backupState.backupStep == BackupStep.InitBackup ||
                        onboardingWalletState.backupState.backupStep == BackupStep.Finished
                    ) {
                        Analytics.send(Onboarding.Backup.ScreenOpened())

                        val isWallet2 = scanResponse?.cardTypesResolver?.isWallet2() ?: false
                        if (isWallet2) BackupAction.StartBackup else BackupAction.IntroduceBackup
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

private suspend fun handleFinishBackup(scanResponse: ScanResponse, userWallet: UserWallet? = null) {
    val backupState = store.state.onboardingWalletState.backupState
    val updatedScanResponse = updateScanResponseAfterBackup(scanResponse, backupState)

    val userWalletId = userWallet?.walletId ?: UserWalletIdBuilder.scanResponse(scanResponse).build()
    if (backupState.hasRing && userWalletId != null) {
        scope.launch {
            store.inject(DaggerGraphState::walletsRepository).setHasWalletsWithRing(userWalletId = userWalletId)
        }
    }

    OnboardingHelper.saveWallet(
        alreadyCreatedWallet = userWallet,
        scanResponse = updatedScanResponse,
        accessCode = backupState.accessCode,
        backupCardsIds = backupState.backupCardIds,
        hasBackupError = backupState.hasBackupError,
    )
}

private fun navigateToWalletScreen() {
    mainScope.launch {
        val settingsRepository = store.inject(DaggerGraphState::settingsRepository)
        store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
        if (tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen()) {
            delay(timeMillis = 1_800)
            store.dispatchNavigationAction { push(AppRoute.SaveWallet) }
        }
    }
}

private suspend fun readCard(): CompletionResult<ScanResponse> {
    val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()

    store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
        isBiometricsRequestPolicy = shouldSaveAccessCodes,
    )

    return store.inject(DaggerGraphState::scanCardProcessor).scan(analyticsSource = ScreensSources.Intro)
}

private suspend fun loadArtworkForCard(cardId: String, cardPublicKey: ByteArray, defaultArtwork: Uri?): Uri {
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
                        passphrase = action.passphrase,
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
    var scanResponse = globalState.onboardingState.onboardingManager?.scanResponse
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
            store.dispatchOnMain(BackupAction.AddBackupCard.ChangeButtonLoading(true))
            backupService.addBackupCard { result ->
                backupService.skipCompatibilityChecks = false
                store.inject(DaggerGraphState::cardSdkConfigRepository).sdk.config.filter.cardIdFilter = null
                store.dispatchOnMain(BackupAction.AddBackupCard.ChangeButtonLoading(false))
                when (result) {
                    is CompletionResult.Success -> {
                        updateArtworks(backupService.addedBackupCardsCount, result.data)
                        store.dispatchOnMain(BackupAction.AddBackupCard.Success(result.data))
                    }
                    is CompletionResult.Failure -> {
                        val crashlytics = FirebaseCrashlytics.getInstance()

                        when (val error = result.error) {
                            is TangemSdkError.CardVerificationFailed -> {
                                Analytics.send(
                                    event = OnboardingAnalyticsEvent.Onboarding.OfflineAttestationFailed(
                                        ScreensSources.Backup,
                                    ),
                                )

                                val resource = error.localizedDescriptionRes()
                                val resId = resource.resId ?: com.tangem.core.ui.R.string.common_unknown_error
                                val resArgs = resource.args.map { it.value }

                                store.inject(DaggerGraphState::uiMessageSender).send(
                                    message = Dialogs.cardVerificationFailed(
                                        errorDescription = resourceReference(id = resId, resArgs.toWrappedList()),
                                        onRequestSupport = {
                                            mainScope.launch {
                                                store.inject(DaggerGraphState::sendFeedbackEmailUseCase)
                                                    .invoke(type = FeedbackEmailType.CardAttestationFailed)
                                            }
                                        },
                                    ),
                                )
                            }
                            is TangemSdkError.BackupFailedNotEmptyWallets -> {
                                store.dispatchOnMain(
                                    GlobalAction.ShowDialog(
                                        BackupDialog.ResetBackupCard(error.cardId),
                                    ),
                                )
                            }
                            is TangemSdkError.IssuerSignatureLoadingFailed -> {
                                store.dispatchOnMain(
                                    GlobalAction.ShowDialog(BackupDialog.AttestationFailed),
                                )
                            }
                            else -> crashlytics.recordException(error)
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
            val isRing = scanResponse?.cardTypesResolver?.isRing() == true
            val iconScanRes = if (isRing) R.drawable.img_hand_scan_ring else null

            store.dispatchOnMain(BackupAction.SetHasRing(hasRing = isRing))

            tangemSdkManager.changeProductType(isRing)
            backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(1))
                    }
                    is CompletionResult.Failure -> Unit
                }
                tangemSdkManager.clearProductType()
            }
        }
        is BackupAction.WriteBackupCard -> {
            val cardIndex = if (action.cardNumber > 0) action.cardNumber - 1 else action.cardNumber
            val backupCard = backupState.backupCards.getOrNull(cardIndex)
            val isRing = backupCard.isRing()
            val iconScanRes = if (isRing) R.drawable.img_hand_scan_ring else null

            store.dispatchOnMain(BackupAction.SetHasRing(hasRing = isRing))

            tangemSdkManager.changeProductType(isRing)
            backupService.proceedBackup(iconScanRes = iconScanRes) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val backupValidator = BackupValidator()
                        if (!backupValidator.isValidBackupStatus(CardDTO(result.data))) {
                            store.dispatchOnMain(BackupAction.ErrorInBackupCard)
                        }
                        if (backupService.currentState == BackupService.State.Finished) {
                            store.dispatchOnMain(BackupAction.FinishBackup())
                        } else {
                            store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(action.cardNumber + 1))
                        }
                    }
                    is CompletionResult.Failure -> {
                        when (val error = result.error) {
                            is TangemSdkError.BackupFailedNotEmptyWallets -> {
                                store.dispatchOnMain(
                                    GlobalAction.ShowDialog(
                                        BackupDialog.ResetBackupCard(error.cardId),
                                    ),
                                )
                            }
                        }
                    }
                }

                tangemSdkManager.clearProductType()
            }
        }
        is BackupAction.DiscardBackup -> {
            backupService.discardSavedBackup()
        }
        is BackupAction.DiscardSavedBackup -> {
            mainScope.launch {
                backupService.primaryCardId?.let {
                    Analytics.send(Onboarding.Finished())
                    store.state.globalState.onboardingState.onboardingManager?.finishActivation(it)
                }
                backupService.discardSavedBackup()

                val isOnboardingV2Enabled = store.inject(DaggerGraphState::onboardingV2FeatureToggles)
                    .isOnboardingV2Enabled

                if (isOnboardingV2Enabled) {
                    val onboardingRepository = store.inject(DaggerGraphState::onboardingRepository)
                    onboardingRepository.clearUnfinishedFinalizeOnboarding()
                }
            }
        }
        is BackupAction.CheckForUnfinishedBackup -> {
            val isOnboardingV2Enabled = store.inject(DaggerGraphState::onboardingV2FeatureToggles).isOnboardingV2Enabled

            if (isOnboardingV2Enabled) {
                val onboardingRepository = store.inject(DaggerGraphState::onboardingRepository)

                mainScope.launch {
                    val onboardingScanResponse = onboardingRepository.getUnfinishedFinalizeOnboarding() ?: return@launch
                    store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound(onboardingScanResponse)))
                }
            } else if (backupService.hasIncompletedBackup) {
                store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound()))
            }
        }
        is BackupAction.ResumeFoundUnfinishedBackup -> {
            if (action.unfinishedBackupScanResponse != null) {
                // onboarding V2
                store.dispatchNavigationAction {
                    push(
                        AppRoute.Onboarding(
                            scanResponse = action.unfinishedBackupScanResponse,
                            startFromBackup = false,
                        ),
                    )
                }
            } else {
                store.dispatch(
                    GlobalAction.Onboarding.StartForUnfinishedBackup(
                        addedBackupCardsCount = backupService.addedBackupCardsCount,
                    ),
                )

                store.dispatchNavigationAction { push(AppRoute.OnboardingWallet()) }
            }
        }
        is BackupAction.SkipBackup -> {
            Analytics.send(Onboarding.Backup.Skipped())
            Analytics.send(Onboarding.Finished())

            scope.launch {
                launch {
                    store.state.globalState.onboardingState.onboardingManager?.finishActivation(
                        cardIds = gatherCardIds(backupState, card),
                    )
                }

                with(scanResponse) {
                    if (this == null) {
                        when (val result = readCard()) {
                            is CompletionResult.Success -> handleFinishBackup(result.data)
                            is CompletionResult.Failure -> store.dispatchNavigationAction(AppRouter::pop)
                        }
                    } else {
                        handleFinishBackup(scanResponse = this)
                    }
                }
            }
        }
        is BackupAction.FinishBackup -> {
            scope.launch {
                if (action.withAnalytics) {
                    Analytics.send(Onboarding.Backup.Finished(backupState.backupCardsNumber))
                }

                var userWallet: UserWallet? = null
                if (scanResponse != null) {
                    scanResponse = updateScanResponseAfterBackup(scanResponse!!, backupState)
                    userWallet = createUserWallet(
                        scanResponse = requireNotNull(value = scanResponse, lazyMessage = { "ScanResponse is null" }),
                        backupState = backupState,
                    )
                } else {
                    delay(HIDE_PROGRESS_DELAY)

                    when (val result = readCard()) {
                        is CompletionResult.Failure -> {
                            store.dispatchNavigationAction(AppRouter::pop)
                            return@launch
                        }
                        is CompletionResult.Success -> {
                            scanResponse = result.data
                            userWallet = createUserWallet(scanResponse = result.data, backupState = backupState)
                        }
                    }
                }

                val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)
                when (backupState.startedSource) {
                    BackupStartedSource.Onboarding -> saveWallet(
                        userWalletsListManager = userWalletsListManager,
                        userWallet = userWallet,
                        scanResponse = scanResponse,
                        backupState = backupState,
                    )
                    BackupStartedSource.CreateBackup -> updateWallet(
                        userWalletsListManager = userWalletsListManager,
                        userWallet = userWallet,
                        backupState = backupState,
                    )
                }

                scope.launch {
                    if (userWallet.scanResponse.cardTypesResolver.isWallet2() && userWallet.isImported) {
                        store.inject(DaggerGraphState::walletsRepository).markWallet2WasCreated(userWallet.walletId)
                    }
                }

                val notActivatedCardIds = gatherCardIds(backupState, card).mapNotNull {
                    if (store.state.globalState.onboardingState.onboardingManager?.isActivationFinished(it) == true) {
                        null
                    } else {
                        it
                    }
                }

                // All cardIds may already be activated if the backup was skipped before.
                if (notActivatedCardIds.isEmpty()) {
                    delay(1000)
                    store.dispatchWithMain(BackupAction.BackupFinished(userWallet.walletId))
                    return@launch
                }

                Analytics.send(Onboarding.Finished())

                store.state.globalState.onboardingState.onboardingManager?.finishActivation(notActivatedCardIds)
                handleFinishBackup(requireNotNull(scanResponse), userWallet)
                store.dispatchWithMain(BackupAction.BackupFinished(userWalletId = userWallet.walletId))
            }
        }

        is BackupAction.ResetBackupCard -> {
            scope.launch { tangemSdkManager.resetToFactorySettings(action.cardId, false) }
        }

        else -> Unit
    }
}

private fun Card?.isRing(): Boolean {
    return this?.let { RING_BATCH_IDS.contains(batchId) || batchId.startsWith(RING_BATCH_PREFIX) } ?: false
}

private suspend fun saveWallet(
    userWalletsListManager: UserWalletsListManager,
    userWallet: UserWallet,
    scanResponse: ScanResponse?,
    backupState: BackupState,
) {
    userWalletsListManager.save(
        userWallet = userWallet.copy(
            scanResponse = updateScanResponseAfterBackup(
                scanResponse = requireNotNull(
                    value = scanResponse,
                    lazyMessage = { "ScanResponse is null" },
                ),
                backupState = backupState,
            ),
        ),
        canOverride = true,
    )
}

private suspend fun updateWallet(
    userWalletsListManager: UserWalletsListManager,
    userWallet: UserWallet,
    backupState: BackupState,
) {
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
}

private suspend fun createUserWallet(scanResponse: ScanResponse, backupState: BackupState): UserWallet {
    val walletNameGenerateUseCase = store.inject(DaggerGraphState::generateWalletNameUseCase)
    return requireNotNull(
        value = UserWalletBuilder(scanResponse, walletNameGenerateUseCase)
            .backupCardsIds(backupState.backupCardIds.toSet())
            .hasBackupError(backupState.hasBackupError)
            .build(),
        lazyMessage = { "User wallet not created" },
    )
}

fun updateArtworks(addedBackupCardsCount: Int, card: Card) {
    mainScope.launch {
        withIOContext {
            val imageUri = loadArtworkForCard(card.cardId, card.cardPublicKey, Uri.EMPTY)
            when (addedBackupCardsCount) {
                1 -> {
                    store.dispatchOnMain(OnboardingWalletAction.SetSecondCardArtworkUrl(imageUri))
                }
                2 -> {
                    store.dispatchOnMain(OnboardingWalletAction.SetThirdCardArtworkUrl(imageUri))
                }
            }
        }
    }
}

internal fun gatherCardIds(backupState: BackupState, card: CardDTO?): List<String> {
    return (listOf(backupState.primaryCardId, card?.cardId) + backupState.backupCardIds)
        .filterNotNull()
        .distinct()
}

private fun handleOnBackPressed(state: OnboardingWalletState) {
    when (state.backupState.backupStep) {
        null, BackupStep.InitBackup, BackupStep.ScanOriginCard, BackupStep.AddBackupCards, BackupStep.EnterAccessCode,
        BackupStep.ReenterAccessCode, BackupStep.SetAccessCode, BackupStep.WritePrimaryCard,
        -> {
            showInterruptOnboardingDialog()
        }
        is BackupStep.WriteBackupCard -> {
            store.dispatch(GlobalAction.ShowDialog(BackupDialog.BackupInProgress))
        }
        BackupStep.Finished -> {
            OnboardingHelper.onInterrupted()
            store.dispatchNavigationAction(AppRouter::pop)
        }
    }
}

private fun showInterruptOnboardingDialog() {
    store.dispatchDialogShow(
        OnboardingDialog.InterruptOnboarding(
            onOk = {
                OnboardingHelper.onInterrupted()
                store.dispatch(BackupAction.DiscardBackup)
                store.dispatchNavigationAction(AppRouter::pop)
            },
        ),
    )
}