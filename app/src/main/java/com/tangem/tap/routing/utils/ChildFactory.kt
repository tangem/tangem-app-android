package com.tangem.tap.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.QrScanningComponent
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.feature.usedesk.api.UsedeskComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.features.createwalletselection.CreateWalletSelectionComponent
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.markets.tokenlist.MarketsTokenListComponent
import com.tangem.features.nft.component.NFTComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onramp.component.*
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.swap.SwapComponent
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.wallet.WalletEntryComponent
import com.tangem.features.walletconnect.components.WalletConnectEntryComponent
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles
import com.tangem.tap.features.details.ui.appcurrency.api.AppCurrencySelectorComponent
import com.tangem.tap.features.details.ui.appsettings.api.AppSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.api.AccessCodeRecoveryComponent
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.features.details.ui.securitymode.api.SecurityModeComponent
import com.tangem.tap.features.details.ui.walletconnect.api.WalletConnectComponent
import com.tangem.tap.features.home.api.HomeComponent
import com.tangem.tap.features.welcome.component.WelcomeComponent
import com.tangem.tap.routing.component.RoutingComponent.Child
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import com.tangem.features.walletconnect.components.WalletConnectEntryComponent as RedesignedWalletConnectComponent

@ActivityScoped
@Suppress("LongParameterList", "LargeClass")
internal class ChildFactory @Inject constructor(
    private val detailsComponentFactory: DetailsComponent.Factory,
    private val walletSettingsComponentFactory: WalletSettingsComponent.Factory,
    private val disclaimerComponentFactory: DisclaimerComponent.Factory,
    private val manageTokensComponentFactory: ManageTokensComponent.Factory,
    private val marketsTokenDetailsComponentFactory: MarketsTokenDetailsComponent.Factory,
    private val marketsTokenListComponentFactory: MarketsTokenListComponent.FactoryScreen,
    private val onrampComponentFactory: OnrampComponent.Factory,
    private val onrampSuccessComponentFactory: OnrampSuccessComponent.Factory,
    private val buyCryptoComponentFactory: BuyCryptoComponent.Factory,
    private val sellCryptoComponentFactory: SellCryptoComponent.Factory,
    private val swapSelectTokensComponentFactory: SwapSelectTokensComponent.Factory,
    private val onboardingEntryComponentFactory: OnboardingEntryComponent.Factory,
    private val welcomeComponentFactory: WelcomeComponent.Factory,
    private val storiesComponentFactory: StoriesComponent.Factory,
    private val stakingComponentFactory: StakingComponent.Factory,
    private val swapComponentFactory: SwapComponent.Factory,
    private val homeComponentFactory: HomeComponent.Factory,
    private val tokenDetailsComponentFactory: TokenDetailsComponent.Factory,
    private val walletConnectComponentFactory: WalletConnectComponent.Factory,
    private val qrScanningComponentFactory: QrScanningComponent.Factory,
    private val accessCodeRecoveryComponentFactory: AccessCodeRecoveryComponent.Factory,
    private val cardSettingsComponentFactory: CardSettingsComponent.Factory,
    private val appCurrencySelectorComponentFactory: AppCurrencySelectorComponent.Factory,
    private val appSettingsComponentFactory: AppSettingsComponent.Factory,
    private val securityModeComponentFactory: SecurityModeComponent.Factory,
    private val resetCardComponentFactory: ResetCardComponent.Factory,
    private val referralComponentFactory: ReferralComponent.Factory,
    private val pushNotificationsComponentFactory: PushNotificationsComponent.Factory,
    private val walletComponentFactory: WalletEntryComponent.Factory,
    private val sendComponentFactoryV2: SendComponent.Factory,
    private val redesignedWalletConnectComponentFactory: WalletConnectEntryComponent.Factory,
    private val nftComponentFactory: NFTComponent.Factory,
    private val nftSendComponentFactory: NFTSendComponent.Factory,
    private val usedeskComponentFactory: UsedeskComponent.Factory,
    private val chooseManagedTokensComponentFactory: ChooseManagedTokensComponent.Factory,
    private val createWalletSelectionComponentFactory: CreateWalletSelectionComponent.Factory,
    private val createMobileWalletComponentFactory: CreateMobileWalletComponent.Factory,
    private val addExistingWalletComponentFactory: AddExistingWalletComponent.Factory,
    private val walletConnectFeatureToggles: WalletConnectFeatureToggles,
) {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun createChild(route: AppRoute, context: AppComponentContext): Child {
        return when (route) {
            is AppRoute.Initial -> {
                Child.Initial
            }
            is AppRoute.Details -> {
                createComponentChild(
                    context = context,
                    params = DetailsComponent.Params(route.userWalletId),
                    componentFactory = detailsComponentFactory,
                )
            }
            is AppRoute.Disclaimer -> {
                createComponentChild(
                    context = context,
                    params = DisclaimerComponent.Params(route.isTosAccepted),
                    componentFactory = disclaimerComponentFactory,
                )
            }
            is AppRoute.ManageTokens -> {
                val source = when (route.source) {
                    AppRoute.ManageTokens.Source.SETTINGS -> ManageTokensSource.SETTINGS
                    AppRoute.ManageTokens.Source.ONBOARDING -> ManageTokensSource.ONBOARDING
                    AppRoute.ManageTokens.Source.STORIES -> ManageTokensSource.STORIES
                }

                createComponentChild(
                    context = context,
                    params = ManageTokensComponent.Params(route.userWalletId, source),
                    componentFactory = manageTokensComponentFactory,
                )
            }
            is AppRoute.Welcome -> {
                createComponentChild(
                    context = context,
                    params = WelcomeComponent.Params(
                        intent = route.intent,
                    ),
                    componentFactory = welcomeComponentFactory,
                )
            }
            is AppRoute.WalletSettings -> {
                createComponentChild(
                    context = context,
                    params = WalletSettingsComponent.Params(route.userWalletId),
                    componentFactory = walletSettingsComponentFactory,
                )
            }
            is AppRoute.MarketsTokenDetails -> {
                createComponentChild(
                    context = context,
                    params = MarketsTokenDetailsComponent.Params(
                        token = route.token,
                        appCurrency = route.appCurrency,
                        showPortfolio = route.showPortfolio,
                        analyticsParams = route.analyticsParams?.let {
                            MarketsTokenDetailsComponent.AnalyticsParams(
                                blockchain = it.blockchain,
                                source = it.source,
                            )
                        },
                    ),
                    componentFactory = marketsTokenDetailsComponentFactory,
                )
            }
            is AppRoute.Onramp -> {
                createComponentChild(
                    context = context,
                    params = OnrampComponent.Params(
                        userWalletId = route.userWalletId,
                        cryptoCurrency = route.currency,
                        source = route.source,
                    ),
                    componentFactory = onrampComponentFactory,
                )
            }
            is AppRoute.OnrampSuccess -> {
                createComponentChild(
                    context = context,
                    params = OnrampSuccessComponent.Params(route.txId),
                    componentFactory = onrampSuccessComponentFactory,
                )
            }
            is AppRoute.BuyCrypto -> {
                createComponentChild(
                    context = context,
                    params = BuyCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = buyCryptoComponentFactory,
                )
            }
            is AppRoute.SellCrypto -> {
                createComponentChild(
                    context = context,
                    params = SellCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = sellCryptoComponentFactory,
                )
            }
            is AppRoute.SwapCrypto -> {
                createComponentChild(
                    context = context,
                    params = SwapSelectTokensComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = swapSelectTokensComponentFactory,
                )
            }
            is AppRoute.Onboarding -> {
                createComponentChild(
                    context = context,
                    params = OnboardingEntryComponent.Params(
                        scanResponse = route.scanResponse,
                        mode = when (route.mode) {
                            AppRoute.Onboarding.Mode.Onboarding -> OnboardingEntryComponent.Mode.Onboarding
                            AppRoute.Onboarding.Mode.AddBackupWallet1 -> OnboardingEntryComponent.Mode.AddBackupWallet1
                            AppRoute.Onboarding.Mode.WelcomeOnlyTwin -> OnboardingEntryComponent.Mode.WelcomeOnlyTwin
                            AppRoute.Onboarding.Mode.RecreateWalletTwin ->
                                OnboardingEntryComponent.Mode.RecreateWalletTwin
                            AppRoute.Onboarding.Mode.ContinueFinalize ->
                                OnboardingEntryComponent.Mode.ContinueFinalize
                        },
                    ),
                    componentFactory = onboardingEntryComponentFactory,
                )
            }
            is AppRoute.Stories -> {
                createComponentChild(
                    context = context,
                    params = StoriesComponent.Params(
                        storyId = route.storyId,
                        nextScreen = route.nextScreen,
                        screenSource = route.screenSource,
                    ),
                    componentFactory = storiesComponentFactory,
                )
            }
            is AppRoute.CurrencyDetails -> {
                createComponentChild(
                    context = context,
                    params = TokenDetailsComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                    ),
                    componentFactory = tokenDetailsComponentFactory,
                )
            }
            is AppRoute.Staking -> {
                createComponentChild(
                    context = context,
                    params = StakingComponent.Params(
                        userWalletId = route.userWalletId,
                        cryptoCurrencyId = route.cryptoCurrencyId,
                        yieldId = route.yieldId,
                    ),
                    componentFactory = stakingComponentFactory,
                )
            }
            is AppRoute.Swap -> {
                createComponentChild(
                    context = context,
                    params = SwapComponent.Params(
                        currencyFrom = route.currencyFrom,
                        currencyTo = route.currencyTo,
                        userWalletId = route.userWalletId,
                        isInitialReverseOrder = route.isInitialReverseOrder,
                        screenSource = route.screenSource,
                    ),
                    componentFactory = swapComponentFactory,
                )
            }
            is AppRoute.Send -> {
                createComponentChild(
                    context = context,
                    params = SendComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                        transactionId = route.transactionId,
                        amount = route.amount,
                        tag = route.tag,
                        destinationAddress = route.destinationAddress,
                    ),
                    componentFactory = sendComponentFactoryV2,
                )
            }
            is AppRoute.Home -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = homeComponentFactory,
                )
            }
            is AppRoute.WalletConnectSessions -> {
                if (walletConnectFeatureToggles.isRedesignedWalletConnectEnabled) {
                    createComponentChild(
                        context = context,
                        params = RedesignedWalletConnectComponent.Params(route.userWalletId),
                        componentFactory = redesignedWalletConnectComponentFactory,
                    )
                } else {
                    createComponentChild(
                        context = context,
                        params = WalletConnectComponent.Params(route.userWalletId),
                        componentFactory = walletConnectComponentFactory,
                    )
                }
            }
            is AppRoute.QrScanning -> {
                val source = when (route.source) {
                    is AppRoute.QrScanning.Source.Send -> SourceType.SEND
                    is AppRoute.QrScanning.Source.WalletConnect -> SourceType.WALLET_CONNECT
                }
                createComponentChild(
                    context = context,
                    params = QrScanningComponent.Params(
                        source = source,
                        networkName = (route.source as? AppRoute.QrScanning.Source.Send)?.networkName,
                    ),
                    componentFactory = qrScanningComponentFactory,
                )
            }
            is AppRoute.AccessCodeRecovery -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = accessCodeRecoveryComponentFactory,
                )
            }
            is AppRoute.CardSettings -> {
                createComponentChild(
                    context = context,
                    params = CardSettingsComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = cardSettingsComponentFactory,
                )
            }
            is AppRoute.AppCurrencySelector -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = appCurrencySelectorComponentFactory,
                )
            }
            is AppRoute.AppSettings -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = appSettingsComponentFactory,
                )
            }
            is AppRoute.DetailsSecurity -> {
                createComponentChild(
                    context = context,
                    params = SecurityModeComponent.Params(
                        userWalletId = route.userWalletId,
                    ),
                    componentFactory = securityModeComponentFactory,
                )
            }
            is AppRoute.ResetToFactory -> {
                createComponentChild(
                    context = context,
                    params = ResetCardComponent.Params(
                        userWalletId = route.userWalletId,
                        cardId = route.cardId,
                        isActiveBackupStatus = route.isActiveBackupStatus,
                        backupCardsCount = route.backupCardsCount,
                    ),
                    componentFactory = resetCardComponentFactory,
                )
            }
            is AppRoute.ReferralProgram -> {
                createComponentChild(
                    context = context,
                    params = ReferralComponent.Params(route.userWalletId),
                    componentFactory = referralComponentFactory,
                )
            }
            is AppRoute.PushNotification -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = pushNotificationsComponentFactory,
                )
            }
            is AppRoute.Wallet -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = walletComponentFactory,
                )
            }
            is AppRoute.NFT ->
                createComponentChild(
                    context = context,
                    params = NFTComponent.Params(
                        userWalletId = route.userWalletId,
                        walletName = route.walletName,
                    ),
                    componentFactory = nftComponentFactory,
                )
            is AppRoute.NFTSend -> {
                createComponentChild(
                    context = context,
                    params = NFTSendComponent.Params(
                        userWalletId = route.userWalletId,
                        nftAsset = route.nftAsset,
                        nftCollectionName = route.nftCollectionName,
                    ),
                    componentFactory = nftSendComponentFactory,
                )
            }
            is AppRoute.Markets -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = marketsTokenListComponentFactory,
                )
            }
            is AppRoute.Usedesk -> { // TODO [REDACTED_TASK_KEY] pass params
                createComponentChild(
                    context = context,
                    params = UsedeskComponent.Params(),
                    componentFactory = usedeskComponentFactory,
                )
            }
            is AppRoute.ChooseManagedTokens -> {
                createComponentChild(
                    context = context,
                    params = ChooseManagedTokensComponent.Params(
                        userWalletId = route.userWalletId,
                        initialCurrency = route.initialCurrency,
                        source = ChooseManagedTokensComponent.Source.valueOf(route.source.name),
                    ),
                    componentFactory = chooseManagedTokensComponentFactory,
                )
            }
            is AppRoute.CreateWalletSelection -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = createWalletSelectionComponentFactory,
                )
            }
            is AppRoute.CreateMobileWallet -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = createMobileWalletComponentFactory,
                )
            }
            is AppRoute.AddExistingWallet -> {
                createComponentChild(
                    context = context,
                    params = Unit,
                    componentFactory = addExistingWalletComponentFactory,
                )
            }
        }
    }
}