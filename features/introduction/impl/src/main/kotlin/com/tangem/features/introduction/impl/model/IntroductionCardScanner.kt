package com.tangem.features.introduction.impl.model

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

/**
 * Reads a cold wallet from the Introduction screen and opens it.
 */
@ModelScoped
@Suppress("LongParameterList")
internal class IntroductionCardScanner @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
) {

    /**
     * @param analyticsSource reported by the scan itself, and by the tap the caller reports, so the two stay
     *   joinable in one funnel.
     * @param onNavigatedAway invoked just before navigation takes the viewer off this screen: the wallet opened,
     *   or an inactive card left for its activation flow.
     * @param onFailed invoked on every exit that keeps the viewer here — cancel, scan failure, a card that could
     *   not become a saved wallet — so the caller can put them back where they acted from.
     */
    suspend fun scanAndOpenWallet(
        analyticsSource: AnalyticsParam.ScreensSources,
        onNavigatedAway: () -> Unit,
        onFailed: () -> Unit,
    ) {
        // Deprecated in favour of walletsRepository.requireAccessCode, which is its inverse over a different
        // fallback key. CreateWalletStartModel, the other entry into this same scan, still reads it, and the two
        // must not resolve the policy differently — so both migrate together or neither.
        @Suppress("DEPRECATION")
        cardSdkConfigRepository.setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = settingsRepository.shouldSaveAccessCodes(),
        )

        scanCardProcessor.scan(
            analyticsSource = analyticsSource,
            shouldCheckIsAlreadyActivated = true,
            // The scan layer itself pushes an inactive card's activation route right after this callback.
            onWalletNotCreated = { onNavigatedAway() },
            onCancel = { onFailed() },
            onFailure = { error ->
                onScanFailure(error, analyticsSource)
                onFailed()
            },
            onSuccess = { scanResponse ->
                saveAndOpen(
                    scanResponse = scanResponse,
                    analyticsSource = analyticsSource,
                    onNavigatedAway = onNavigatedAway,
                    onFailed = onFailed,
                )
            },
        )
    }

    private suspend fun saveAndOpen(
        scanResponse: ScanResponse,
        analyticsSource: AnalyticsParam.ScreensSources,
        onNavigatedAway: () -> Unit,
        onFailed: () -> Unit,
    ) {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build()
        if (userWallet == null) {
            TangemLogger.e("User wallet not created")
            reportFailure(messageId = null, onFailed = onFailed)
            return
        }

        saveWalletUseCase(userWallet = userWallet, analyticsSource = analyticsSource).fold(
            ifLeft = { error ->
                when (error) {
                    is SaveWalletError.DataError -> {
                        TangemLogger.e("Unable to save user wallet: $error")
                        reportFailure(messageId = error.messageId, onFailed = onFailed)
                    }
                    // Unlocking reports the sign-in itself, so this branch must not send it again.
                    is SaveWalletError.WalletAlreadySaved -> userWalletsListRepository.unlock(
                        userWalletId = userWallet.walletId,
                        unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(
                            scanResponse = scanResponse,
                            source = analyticsSource,
                        ),
                    ).onLeft {
                        onFailed()
                    }.onRight {
                        onNavigatedAway()
                        router.replaceAll(AppRoute.Wallet)
                    }
                }
            },
            ifRight = {
                analyticsEventHandler.send(
                    Basic.SignedIn(
                        signInType = AnalyticsParam.SignInType.Card,
                        walletsCount = userWalletsListRepository.userWalletsSync().size,
                        isImported = userWallet.isImported,
                        isBackedUp = scanResponse.card.backupStatus?.isActive == true,
                    ),
                )
                onNavigatedAway()
                openWalletOrAskForPush()
            },
        )
    }

    private suspend fun openWalletOrAskForPush() {
        // The wallet is saved by now, so a failing read opens it rather than stranding the viewer.
        val shouldAsk = runSuspendCatching { shouldAskPermissionUseCase(PUSH_PERMISSION) }
            .getOrElse { error ->
                TangemLogger.e("Unable to read the push permission preference", error)
                false
            }
        val route = if (shouldAsk) {
            AppRoute.PushNotification(
                source = AppRoute.PushNotification.Source.FirstSignIn,
                nextRoute = AppRoute.Wallet,
            )
        } else {
            AppRoute.Wallet
        }
        router.replaceAll(route)
    }

    private fun reportFailure(messageId: Int?, onFailed: () -> Unit) {
        messageSender.send(
            SnackbarMessage(resourceReference(messageId ?: CoreResR.string.common_unknown_error)),
        )
        onFailed()
    }

    private fun onScanFailure(error: TangemError, analyticsSource: AnalyticsParam.ScreensSources) {
        // Card verification failures and repeated-scan prompts are shown by the sdk and the app-global scan-fail
        // requester, so the caller only surfaces the missing nfc and reports a cancellation.
        when (error) {
            is TangemSdkError.NfcFeatureIsUnavailable -> messageSender.send(Dialogs.nfcFeatureUnavailable())
            is TangemSdkError.UserCancelled -> {
                analyticsEventHandler.send(IntroductionProcess.ScanCancelled(analyticsSource))
            }
            else -> TangemLogger.e("Card scan failed: $error")
        }
    }
}