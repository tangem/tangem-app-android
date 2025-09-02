package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.settings.NeverToSuggestRateAppUseCase
import com.tangem.domain.settings.RemindToRateAppLaterUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.tokens.model.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.domain.wallets.legacy.UserWalletsListManager.Lockable.UnlockType
import com.tangem.domain.wallets.models.UnlockWalletsError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
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
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onUnlockWalletClick()

    fun onUnlockVisaAccessClick()

    fun onScanToUnlockWalletClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()

    fun onClosePromoClick(promoId: PromoId)

    fun onPromoClick(promoId: PromoId, cryptoCurrency: CryptoCurrency? = null)

    fun onSupportClick()

    fun onNoteMigrationButtonClick(url: String)

    fun onSeedPhraseNotificationConfirm()

    fun onSeedPhraseNotificationDecline()

    fun onSeedPhraseSecondNotificationAccept()

    fun onSeedPhraseSecondNotificationReject()

    fun onFinishWalletActivationClick()
}

@Suppress("LargeClass", "LongParameterList")
@ModelScoped
internal class WalletWarningsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val scanCardToUnlockWalletClickHandler: ScanCardToUnlockWalletClickHandler,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val shouldShowPromoWalletUseCase: ShouldShowPromoWalletUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val urlOpener: UrlOpener,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val appRouter: AppRouter,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val userWalletsListRepository: UserWalletsListRepository,
) : BaseWalletClickIntents(), WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
        analyticsEventHandler.send(MainScreen.NoticeBackupYourWalletTapped)

        prepareAndStartOnboardingProcess()
    }

    private fun prepareAndStartOnboardingProcess() {
        modelScope.launch(dispatchers.main) {
            getSelectedUserWallet()?.requireColdWallet()?.let {
                router.openOnboardingScreen(
                    scanResponse = it.scanResponse,
                    continueBackup = true,
                )
            }
        }
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        modelScope.launch(dispatchers.main) {
            val userWallet = getSelectedUserWallet() ?: return@launch

            if (userWallet is UserWallet.Cold) {
                setCardWasScannedUseCase(cardId = userWallet.cardId)
            }
        }
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        analyticsEventHandler.send(Basic.CardWasScanned(AnalyticsParam.ScreensSources.Main))
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped)

        modelScope.launch {
            val userWallet = getSelectedUserWallet() ?: return@launch

            derivePublicKeysUseCase(
                userWalletId = userWallet.walletId,
                currencies = missedAddressCurrencies,
            ).fold(
                ifLeft = { Timber.e(it, "Failed to derive public keys") },
                ifRight = {
                    fetchCryptoCurrencies(userWalletId = userWallet.walletId, currencies = missedAddressCurrencies)
                },
            )
        }
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
        analyticsEventHandler.send(MainScreen.WalletUnlockTapped)

        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            modelScope.launch {
                userWalletsListRepository.unlockAllWallets()
                    .onLeft {
                        val selectedUserWallet = getSelectedUserWallet() ?: return@onLeft
                        val method = when (selectedUserWallet) {
                            is UserWallet.Cold -> UserWalletsListRepository.UnlockMethod.Scan
                            is UserWallet.Hot -> UserWalletsListRepository.UnlockMethod.AccessCode
                        }
                        userWalletsListRepository.unlock(stateHolder.getSelectedWalletId(), method)
                    }
            }
            return
        }

        // Will be removed after hot wallet release
        stateHolder.showBottomSheet(
            WalletBottomSheetConfig.UnlockWallets(
                onUnlockClick = this::onUnlockWalletClick,
                onScanClick = this::onScanToUnlockWalletClick,
            ),
        )
    }

    @Deprecated("Will be removed with hot wallet release")
    override fun onUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockAllWithBiometrics)

        modelScope.launch(dispatchers.main) {
            unlockWalletsUseCase(type = UnlockType.ALL_WITHOUT_SELECT)
                .onRight { stateHolder.update(CloseBottomSheetTransformer(stateHolder.getSelectedWalletId())) }
                .onLeft(::handleUnlockWalletsError)
        }
    }

    override fun onUnlockVisaAccessClick() {
        openScanCardDialog()
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

    @Deprecated("Will be removed with hot wallet release")
    override fun onScanToUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.UnlockWithCardScan)
        openScanCardDialog()
    }

    private fun openScanCardDialog() {
        modelScope.launch(dispatchers.main) {
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
                    modelScope.launch(dispatchers.main) {
                        neverToSuggestRateAppUseCase()
                    }
                },
            ),
        )
    }

    override fun onDislikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))

        modelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            val userWallet = getSelectedUserWallet() ?: return@launch
            val cardInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.RateCanBeBetter(walletMetaInfo = cardInfo))
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        modelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onClosePromoClick(promoId: PromoId) {
        analyticsEventHandler.send(
            when (promoId) {
                PromoId.Referral -> MainScreen.ReferralPromoButtonDismiss
                PromoId.Sepa -> TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Main,
                    program = TokenSwapPromoAnalyticsEvent.Program.Sepa,
                    action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
                )
            },

        )
        modelScope.launch(dispatchers.main) {
            shouldShowPromoWalletUseCase.neverToShow(promoId)
        }
    }

    override fun onPromoClick(promoId: PromoId, cryptoCurrency: CryptoCurrency?) {
        val userWallet = getSelectedUserWallet() ?: return
        when (promoId) {
            PromoId.Referral -> {
                analyticsEventHandler.send(MainScreen.ReferralPromoButtonParticipate)
                appRouter.push(AppRoute.ReferralProgram(userWalletId = userWallet.walletId))
            }
            PromoId.Sepa -> {
                analyticsEventHandler.send(
                    TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                        source = AnalyticsParam.ScreensSources.Main,
                        program = TokenSwapPromoAnalyticsEvent.Program.Sepa,
                        action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Clicked,
                    ),
                )
                cryptoCurrency ?: return
                appRouter.push(
                    AppRoute.Onramp(
                        userWalletId = userWallet.walletId,
                        currency = cryptoCurrency,
                        source = OnrampSource.SEPA_BANNER,
                        launchSepa = true,
                    ),
                )
            }
        }
    }

    override fun onSupportClick() {
        val userWallet = getSelectedUserWallet() ?: return

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    override fun onNoteMigrationButtonClick(url: String) {
        analyticsEventHandler.send(MainScreen.NotePromoButton)
        modelScope.launch(dispatchers.main) {
            router.openUrl(url)
        }
    }

    override fun onSeedPhraseNotificationConfirm() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonYes)

        walletEventSender.send(
            event = WalletEvent.ShowAlert(
                state = WalletAlertState.SimpleOkAlert(
                    message = resourceReference(R.string.warning_seedphrase_issue_answer_yes),
                    onOkClick = {
                        modelScope.launch {
                            seedPhraseNotificationUseCase.confirm(userWalletId = userWallet.walletId)

                            urlOpener.openUrl(
                                url = TangemBlogUrlBuilder.build(post = TangemBlogUrlBuilder.Post.SeedNotify),
                            )
                        }
                    },
                ),
            ),
        )
    }

    override fun onSeedPhraseNotificationDecline() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonNo)

        walletEventSender.send(
            event = WalletEvent.ShowAlert(
                state = WalletAlertState.SimpleOkAlert(
                    message = resourceReference(R.string.warning_seedphrase_issue_answer_no),
                    onOkClick = {
                        modelScope.launch {
                            seedPhraseNotificationUseCase.decline(userWalletId = userWallet.walletId)
                        }
                    },
                ),
            ),
        )
    }

    override fun onSeedPhraseSecondNotificationAccept() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonUsed)

        walletEventSender.send(
            event = WalletEvent.ShowAlert(
                state = WalletAlertState.SimpleOkAlert(
                    message = resourceReference(R.string.warning_seedphrase_issue_answer_yes),
                    onOkClick = {
                        modelScope.launch {
                            seedPhraseNotificationUseCase.acceptSecond(userWalletId = userWallet.walletId)

                            urlOpener.openUrl(
                                url = TangemBlogUrlBuilder.build(post = TangemBlogUrlBuilder.Post.SeedNotifySecond),
                            )
                        }
                    },
                ),
            ),
        )
    }

    override fun onSeedPhraseSecondNotificationReject() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonDeclined)

        modelScope.launch {
            seedPhraseNotificationUseCase.rejectSecond(userWalletId = userWallet.walletId)
        }
    }

    override fun onFinishWalletActivationClick() {
        val userWallet = getSelectedUserWallet() ?: return
        appRouter.push(AppRoute.WalletActivation(userWallet.walletId))
    }

    private suspend fun fetchCryptoCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        coroutineScope {
            listOf(
                async {
                    multiNetworkStatusFetcher(
                        params = MultiNetworkStatusFetcher.Params(
                            userWalletId = userWalletId,
                            networks = currencies.map(CryptoCurrency::network).toSet(),
                        ),
                    )
                        .onLeft { Timber.e("Unable to fetch networks: $it") }
                },
                async {
                    multiQuoteStatusFetcher(
                        params = MultiQuoteStatusFetcher.Params(
                            currenciesIds = currencies.mapNotNull { it.id.rawCurrencyId }.toSet(),
                            appCurrencyId = null,
                        ),
                    )
                        .onLeft { Timber.e("Unable to fetch quotes: $it") }
                },
                async {
                    val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
                        stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
                    }

                    multiYieldBalanceFetcher(
                        params = MultiYieldBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
                    )
                        .onLeft { Timber.e("Unable to fetch yield balances: $it") }
                },
            )
                .awaitAll()
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