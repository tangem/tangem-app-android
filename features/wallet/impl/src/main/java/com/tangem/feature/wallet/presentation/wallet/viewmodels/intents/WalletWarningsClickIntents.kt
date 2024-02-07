package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.NeverToSuggestRateAppUseCase
import com.tangem.domain.settings.RemindToRateAppLaterUseCase
import com.tangem.domain.tokens.FetchTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UnlockWalletsError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletsUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletClickHandler
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletError
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onUnlockWalletClick()

    fun onScanToUnlockWalletClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()
}

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletWarningsClickIntentsImplementer @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val scanCardToUnlockWalletClickHandler: ScanCardToUnlockWalletClickHandler,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
        analyticsEventHandler.send(MainScreen.NoticeBackupYourWalletTapped)

        prepareOnboardingProcess()
        router.openOnboardingScreen()
    }

    private fun prepareOnboardingProcess() {
        getSelectedWalletSyncUseCase.unwrap()?.let {
            reduxStateHolder.dispatch(
                LegacyAction.StartOnboardingProcess(
                    scanResponse = it.scanResponse,
                    canSkipBackup = false,
                ),
            )
        }
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        viewModelScope.launch(dispatchers.main) {
            setCardWasScannedUseCase(cardId = userWallet.cardId)
        }
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Main))
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped)

        viewModelScope.launch(dispatchers.main) {
            derivePublicKeysUseCase(
                userWalletId = userWallet.walletId,
                currencies = missedAddressCurrencies,
            )
                .onRight { fetchTokenListUseCase(userWalletId = userWallet.walletId) }
                .onLeft { Timber.e("Failed to derive public keys: $it") }
        }
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
        val config = requireNotNull(stateHolder.getSelectedWallet().bottomSheetConfig) {
            "Impossible to open unlock wallet bottom sheet if it's null"
        }

        stateHolder.showBottomSheet(config.content)
    }

    override fun onUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockAllWithBiometrics)

        viewModelScope.launch(dispatchers.main) {
            unlockWalletsUseCase(throwIfNotAllWalletsUnlocked = true)
                .onRight { stateHolder.update(CloseBottomSheetTransformer(stateHolder.getSelectedWalletId())) }
                .onLeft(::handleUnlockWalletsError)
        }
    }

    private fun handleUnlockWalletsError(error: UnlockWalletsError) {
        val event = when (error) {
            is UnlockWalletsError.DataError,
            is UnlockWalletsError.UnableToUnlockWallets,
            -> WalletEvent.ShowToast(resourceReference(R.string.user_wallet_list_error_unable_to_unlock))
            is UnlockWalletsError.NoUserWalletSelected,
            is UnlockWalletsError.NotAllUserWalletsUnlocked,
            -> WalletEvent.ShowAlert(WalletAlertState.RescanWallets)
        }

        walletEventSender.send(event)
    }

    override fun onScanToUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockWithCardScan)

        viewModelScope.launch(dispatchers.main) {
            scanCardToUnlockWalletClickHandler(walletId = stateHolder.getSelectedWalletId())
                .onLeft { error ->
                    when (error) {
                        ScanCardToUnlockWalletError.WrongCardIsScanned -> {
                            walletEventSender.send(
                                event = WalletEvent.ShowAlert(WalletAlertState.WrongCardIsScanned),
                            )
                        }
                        ScanCardToUnlockWalletError.ManyScanFails -> router.openScanFailedDialog()
                    }
                }
        }
    }

    override fun onLikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))

        walletEventSender.send(
            event = WalletEvent.RateApp(
                onDismissClick = {
                    viewModelScope.launch(dispatchers.main) {
                        neverToSuggestRateAppUseCase()
                    }
                },
            ),
        )
    }

    override fun onDislikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))

        viewModelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            reduxStateHolder.dispatch(LegacyAction.SendEmailRateCanBeBetter)
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        viewModelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }
}