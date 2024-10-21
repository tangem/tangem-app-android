package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.FetchTokenListUseCase
import com.tangem.domain.tokens.FetchTokenListUseCase.RefreshMode
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.wallets.models.UnlockWalletsError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletsUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletClickHandler
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletError
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
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

    fun onCloseSwapPromoClick()

    fun onCloseRingPromoClick()

    fun onTravalaPromoClick(link: String?)

    fun onCloseTravalaPromoClick()

    fun onSupportClick()
}

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletWarningsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val scanCardToUnlockWalletClickHandler: ScanCardToUnlockWalletClickHandler,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
    private val shouldShowSwapPromoWalletUseCase: ShouldShowSwapPromoWalletUseCase,
    private val shouldShowRingPromoUseCase: ShouldShowRingPromoUseCase,
    private val shouldShowTravalaPromoWalletUseCase: ShouldShowTravalaPromoWalletUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : BaseWalletClickIntents(), WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
        analyticsEventHandler.send(MainScreen.NoticeBackupYourWalletTapped)

        prepareAndStartOnboardingProcess()
    }

    private fun prepareAndStartOnboardingProcess() {
        viewModelScope.launch(dispatchers.main) {
            getSelectedUserWallet()?.let {
                reduxStateHolder.dispatch(
                    LegacyAction.StartOnboardingProcess(
                        scanResponse = it.scanResponse,
                        canSkipBackup = false,
                    ),
                )
            }
            // navigation action shouldn't be out of coroutine to avoid race
            router.openOnboardingScreen()
        }
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        viewModelScope.launch(dispatchers.main) {
            val userWallet = getSelectedUserWallet() ?: return@launch

            setCardWasScannedUseCase(cardId = userWallet.cardId)
        }
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        analyticsEventHandler.send(Basic.CardWasScanned(AnalyticsParam.ScreensSources.Main))
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped)

        viewModelScope.launch {
            val userWallet = getSelectedUserWallet() ?: return@launch

            derivePublicKeysUseCase(
                userWalletId = userWallet.walletId,
                currencies = missedAddressCurrencies,
            ).onLeft {
                Timber.e(it, "Failed to derive public keys")
                return@launch
            }

            // Refresh must be set to true to ensure that yield balances are updated
            fetchTokenListUseCase(userWallet.walletId, mode = RefreshMode.SKIP_CURRENCIES)
        }
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
        analyticsEventHandler.send(MainScreen.WalletUnlockTapped)

        stateHolder.showBottomSheet(
            WalletBottomSheetConfig.UnlockWallets(
                onUnlockClick = this::onUnlockWalletClick,
                onScanClick = this::onScanToUnlockWalletClick,
            ),
        )
    }

    override fun onUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockAllWithBiometrics)

        viewModelScope.launch(dispatchers.main) {
            unlockWalletsUseCase(type = UnlockType.ALL_WITHOUT_SELECT)
                .onRight { stateHolder.update(CloseBottomSheetTransformer(stateHolder.getSelectedWalletId())) }
                .onLeft(::handleUnlockWalletsError)
        }
    }

    private fun handleUnlockWalletsError(error: UnlockWalletsError) {
        val event = when (error) {
            is UnlockWalletsError.DataError,
            is UnlockWalletsError.UnableToUnlockWallets,
            -> WalletEvent.ShowError(resourceReference(R.string.user_wallet_list_error_unable_to_unlock))
            is UnlockWalletsError.NoUserWalletSelected,
            is UnlockWalletsError.NotAllUserWalletsUnlocked,
            -> WalletEvent.ShowAlert(WalletAlertState.RescanWallets)
        }

        walletEventSender.send(event)
    }

    override fun onScanToUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockWithCardScan)
        openScanCardDialog()
    }

    private fun openScanCardDialog() {
        viewModelScope.launch(dispatchers.main) {
            scanCardToUnlockWalletClickHandler(walletId = stateHolder.getSelectedWalletId())
                .onLeft { error ->
                    when (error) {
                        ScanCardToUnlockWalletError.WrongCardIsScanned -> {
                            walletEventSender.send(
                                event = WalletEvent.ShowAlert(WalletAlertState.WrongCardIsScanned),
                            )
                        }
                        ScanCardToUnlockWalletError.ManyScanFails -> router.openScanFailedDialog(::openScanCardDialog)
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

            val scanResponse = getSelectedUserWallet()?.scanResponse ?: return@launch
            val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.RateCanBeBetter(cardInfo = cardInfo))
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        viewModelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onCloseSwapPromoClick() {
        analyticsEventHandler.send(
            TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                source = AnalyticsParam.ScreensSources.Main,
                programName = TokenSwapPromoAnalyticsEvent.ProgramName.OKX,
                action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
            ),
        )
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoWalletUseCase.neverToShow()
        }
    }

    override fun onCloseRingPromoClick() {
        analyticsEventHandler.send(
            TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                source = AnalyticsParam.ScreensSources.Main,
                programName = TokenSwapPromoAnalyticsEvent.ProgramName.Ring,
                action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
            ),
        )

        viewModelScope.launch {
            shouldShowRingPromoUseCase.neverToShow()
        }
    }

    override fun onTravalaPromoClick(link: String?) {
        analyticsEventHandler.send(
            TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                source = AnalyticsParam.ScreensSources.Main,
                programName = TokenSwapPromoAnalyticsEvent.ProgramName.Travala,
                action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Clicked,
            ),
        )
        link?.let {
            viewModelScope.launch(dispatchers.main) {
                router.openUrl(link)
            }
        }
    }

    override fun onCloseTravalaPromoClick() {
        analyticsEventHandler.send(
            TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                source = AnalyticsParam.ScreensSources.Main,
                programName = TokenSwapPromoAnalyticsEvent.ProgramName.Travala,
                action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
            ),
        )
        viewModelScope.launch(dispatchers.main) {
            shouldShowTravalaPromoWalletUseCase.neverToShow()
        }
    }

    override fun onSupportClick() {
        val scanResponse = getSelectedUserWallet()?.scanResponse ?: return
        val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return

        viewModelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(cardInfo = cardInfo))
        }
    }

    private fun getSelectedUserWallet(): UserWallet? {
        val userWalletId = stateHolder.getSelectedWalletId()
        return getUserWalletUseCase(userWalletId).getOrElse {
            Timber.e(
                """
                    Unable to get user wallet
                    |- ID: $userWalletId
                    |- Exception: $it
                """.trimIndent(),
            )

            null
        }
    }
}
