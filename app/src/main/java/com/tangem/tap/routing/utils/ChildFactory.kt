package com.tangem.tap.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.feature.qrscanning.QrScanningComponent
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.stories.api.StoriesComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onramp.component.*
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.send.api.SendComponent
import com.tangem.features.swap.SwapComponent
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.wallet.WalletEntryComponent
import com.tangem.tap.features.details.ui.appcurrency.api.AppCurrencySelectorComponent
import com.tangem.tap.features.details.ui.appsettings.api.AppSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.api.AccessCodeRecoveryComponent
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.features.details.ui.securitymode.api.SecurityModeComponent
import com.tangem.tap.features.details.ui.walletconnect.api.WalletConnectComponent
import com.tangem.tap.features.home.api.HomeComponent
import com.tangem.tap.features.onboarding.products.note.OnboardingNoteFragment
import com.tangem.tap.features.onboarding.products.otherCards.OnboardingOtherCardsFragment
import com.tangem.tap.features.onboarding.products.twins.ui.OnboardingTwinsFragment
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.features.saveWallet.ui.SaveWalletBottomSheetFragment
import com.tangem.tap.features.welcome.component.WelcomeComponent
import com.tangem.tap.routing.component.RoutingComponent.Child
import com.tangem.tap.routing.toggle.RoutingFeatureToggles
import com.tangem.utils.Provider
import dagger.hilt.android.scopes.ActivityScoped
import java.util.WeakHashMap
import javax.inject.Inject

@ActivityScoped
@Suppress("LongParameterList", "LargeClass")
internal class ChildFactory @Inject constructor(
    private val detailsComponentFactory: DetailsComponent.Factory,
    private val walletSettingsComponentFactory: WalletSettingsComponent.Factory,
    private val disclaimerComponentFactory: DisclaimerComponent.Factory,
    private val manageTokensComponentFactory: ManageTokensComponent.Factory,
    private val marketsTokenDetailsComponentFactory: MarketsTokenDetailsComponent.Factory,
    private val onrampComponentFactory: OnrampComponent.Factory,
    private val onrampSuccessComponentFactory: OnrampSuccessComponent.Factory,
    private val buyCryptoComponentFactory: BuyCryptoComponent.Factory,
    private val sellCryptoComponentFactory: SellCryptoComponent.Factory,
    private val swapSelectTokensComponentFactory: SwapSelectTokensComponent.Factory,
    private val onboardingEntryComponentFactory: OnboardingEntryComponent.Factory,
    private val welcomeComponentFactory: WelcomeComponent.Factory,
    private val storiesComponentFactory: StoriesComponent.Factory,
    private val sendComponentFactory: SendComponent.Factory,
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
    private val testerRouter: TesterRouter,
    private val routingFeatureToggles: RoutingFeatureToggles,
) {

    fun createChild(route: AppRoute, contextFactory: (route: AppRoute) -> AppComponentContext): Child {
        return if (routingFeatureToggles.isNavigationRefactoringEnabled) {
            createChildNew(route, contextFactory)
        } else {
            createChildLegacy(route, contextFactory)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createChildNew(route: AppRoute, contextFactory: (route: AppRoute) -> AppComponentContext): Child {
        componentContexts[route] = contextFactory(route)

        // region Child creation
        return when (route) {
            is AppRoute.Initial -> {
                Child.Initial
            }
            is AppRoute.Details -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = DetailsComponent.Params(route.userWalletId),
                    componentFactory = detailsComponentFactory,
                )
            }
            is AppRoute.Disclaimer -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
                    params = ManageTokensComponent.Params(route.userWalletId, source),
                    componentFactory = manageTokensComponentFactory,
                )
            }
            is AppRoute.Welcome -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = WelcomeComponent.Params(
                        intent = route.intent,
                    ),
                    componentFactory = welcomeComponentFactory,
                )
            }
            is AppRoute.TesterMenu -> {
                Child.LegacyIntent(testerRouter.getEntryIntent())
            }
            is AppRoute.WalletSettings -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = WalletSettingsComponent.Params(route.userWalletId),
                    componentFactory = walletSettingsComponentFactory,
                )
            }
            is AppRoute.MarketsTokenDetails -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnrampSuccessComponent.Params(route.externalTxId),
                    componentFactory = onrampSuccessComponentFactory,
                )
            }
            is AppRoute.BuyCrypto -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = BuyCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = buyCryptoComponentFactory,
                )
            }
            is AppRoute.SellCrypto -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SellCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = sellCryptoComponentFactory,
                )
            }
            is AppRoute.SwapCrypto -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SwapSelectTokensComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = swapSelectTokensComponentFactory,
                )
            }
            is AppRoute.Onboarding -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnboardingEntryComponent.Params(
                        scanResponse = route.scanResponse,
                        multiWalletMode = when (route.mode) {
                            AppRoute.Onboarding.Mode.Onboarding -> OnboardingEntryComponent.MultiWalletMode.Onboarding
                            AppRoute.Onboarding.Mode.AddBackup -> OnboardingEntryComponent.MultiWalletMode.AddBackup
                            AppRoute.Onboarding.Mode.ContinueFinalize ->
                                OnboardingEntryComponent.MultiWalletMode.ContinueFinalize
                        },
                    ),
                    componentFactory = onboardingEntryComponentFactory,
                )
            }
            is AppRoute.Stories -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
                    params = TokenDetailsComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                    ),
                    componentFactory = tokenDetailsComponentFactory,
                )
            }
            is AppRoute.Staking -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
                    params = SendComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                        transactionId = route.transactionId,
                        amount = route.amount,
                        tag = route.tag,
                        destinationAddress = route.destinationAddress,
                    ),
                    componentFactory = sendComponentFactory,
                )
            }
            is AppRoute.Home -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = homeComponentFactory,
                )
            }
            is AppRoute.WalletConnectSessions -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = walletConnectComponentFactory,
                )
            }
            is AppRoute.QrScanning -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = QrScanningComponent.Params(
                        source = route.source,
                        networkName = route.networkName,
                    ),
                    componentFactory = qrScanningComponentFactory,
                )
            }
            is AppRoute.AccessCodeRecovery -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = accessCodeRecoveryComponentFactory,
                )
            }
            is AppRoute.CardSettings -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = CardSettingsComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = cardSettingsComponentFactory,
                )
            }
            is AppRoute.AppCurrencySelector -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = appCurrencySelectorComponentFactory,
                )
            }
            is AppRoute.AppSettings -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = appSettingsComponentFactory,
                )
            }
            is AppRoute.DetailsSecurity -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SecurityModeComponent.Params(
                        userWalletId = route.userWalletId,
                    ),
                    componentFactory = securityModeComponentFactory,
                )
            }
            is AppRoute.ResetToFactory -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                    contextProvider = contextProvider(route, contextFactory),
                    params = ReferralComponent.Params(route.userWalletId),
                    componentFactory = referralComponentFactory,
                )
            }
            is AppRoute.PushNotification -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = pushNotificationsComponentFactory,
                )
            }
            is AppRoute.Wallet -> {
                createComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = walletComponentFactory,
                )
            }
            is AppRoute.OnboardingNote,
            is AppRoute.SaveWallet,
            is AppRoute.OnboardingOther,
            is AppRoute.OnboardingTwins,
            is AppRoute.OnboardingWallet,
            -> error("Unsupported route: $route")
        }
        // endregion
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createChildLegacy(route: AppRoute, contextFactory: (route: AppRoute) -> AppComponentContext): Child {
        componentContexts[route] = contextFactory(route)

        // region Child creation
        return when (route) {
            is AppRoute.Initial -> {
                Child.Initial
            }
            is AppRoute.AccessCodeRecovery -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = accessCodeRecoveryComponentFactory,
                )
            }
            is AppRoute.AppCurrencySelector -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = appCurrencySelectorComponentFactory,
                )
            }
            is AppRoute.SaveWallet -> {
                route.asFragmentChild(Provider { SaveWalletBottomSheetFragment() })
            }
            is AppRoute.Send -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SendComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                        transactionId = route.transactionId,
                        amount = route.amount,
                        tag = route.tag,
                        destinationAddress = route.destinationAddress,
                    ),
                    componentFactory = sendComponentFactory,
                )
            }
            is AppRoute.AppSettings -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = appSettingsComponentFactory,
                )
            }
            is AppRoute.CardSettings -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = CardSettingsComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = cardSettingsComponentFactory,
                )
            }
            is AppRoute.Details -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = DetailsComponent.Params(route.userWalletId),
                    componentFactory = detailsComponentFactory,
                )
            }
            is AppRoute.DetailsSecurity -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SecurityModeComponent.Params(
                        userWalletId = route.userWalletId,
                    ),
                    componentFactory = securityModeComponentFactory,
                )
            }
            is AppRoute.Disclaimer -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = DisclaimerComponent.Params(route.isTosAccepted),
                    componentFactory = disclaimerComponentFactory,
                )
            }
            is AppRoute.Home -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = homeComponentFactory,
                )
            }
            is AppRoute.ManageTokens -> {
                val source = when (route.source) {
                    AppRoute.ManageTokens.Source.SETTINGS -> ManageTokensSource.SETTINGS
                    AppRoute.ManageTokens.Source.ONBOARDING -> ManageTokensSource.ONBOARDING
                    AppRoute.ManageTokens.Source.STORIES -> ManageTokensSource.STORIES
                }

                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = ManageTokensComponent.Params(route.userWalletId, source),
                    componentFactory = manageTokensComponentFactory,
                )
            }
            is AppRoute.OnboardingNote -> {
                route.asFragmentChild(Provider { OnboardingNoteFragment() })
            }
            is AppRoute.OnboardingOther -> {
                route.asFragmentChild(Provider { OnboardingOtherCardsFragment() })
            }
            is AppRoute.OnboardingTwins -> {
                route.asFragmentChild(Provider { OnboardingTwinsFragment() })
            }
            is AppRoute.OnboardingWallet -> {
                route.asFragmentChild(Provider { OnboardingWalletFragment() })
            }
            is AppRoute.QrScanning -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = QrScanningComponent.Params(
                        source = route.source,
                        networkName = route.networkName,
                    ),
                    componentFactory = qrScanningComponentFactory,
                )
            }
            is AppRoute.ReferralProgram -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = ReferralComponent.Params(route.userWalletId),
                    componentFactory = referralComponentFactory,
                )
            }
            is AppRoute.ResetToFactory -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = ResetCardComponent.Params(
                        userWalletId = route.userWalletId,
                        cardId = route.cardId,
                        isActiveBackupStatus = route.isActiveBackupStatus,
                        backupCardsCount = route.backupCardsCount,
                    ),
                    componentFactory = resetCardComponentFactory,
                )
            }
            is AppRoute.Swap -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
            is AppRoute.Wallet -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = walletComponentFactory,
                )
            }
            is AppRoute.WalletConnectSessions -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = walletConnectComponentFactory,
                )
            }
            is AppRoute.CurrencyDetails -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = TokenDetailsComponent.Params(
                        userWalletId = route.userWalletId,
                        currency = route.currency,
                    ),
                    componentFactory = tokenDetailsComponentFactory,
                )
            }
            is AppRoute.Welcome -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = WelcomeComponent.Params(
                        intent = route.intent,
                    ),
                    componentFactory = welcomeComponentFactory,
                )
            }
            is AppRoute.TesterMenu -> {
                Child.LegacyIntent(testerRouter.getEntryIntent())
            }
            is AppRoute.Staking -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = StakingComponent.Params(
                        userWalletId = route.userWalletId,
                        cryptoCurrencyId = route.cryptoCurrencyId,
                        yieldId = route.yieldId,
                    ),
                    componentFactory = stakingComponentFactory,
                )
            }
            is AppRoute.PushNotification -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = Unit,
                    componentFactory = pushNotificationsComponentFactory,
                )
            }
            is AppRoute.WalletSettings -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = WalletSettingsComponent.Params(route.userWalletId),
                    componentFactory = walletSettingsComponentFactory,
                )
            }
            is AppRoute.MarketsTokenDetails -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
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
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnrampComponent.Params(
                        userWalletId = route.userWalletId,
                        cryptoCurrency = route.currency,
                        source = route.source,
                    ),
                    componentFactory = onrampComponentFactory,
                )
            }
            is AppRoute.OnrampSuccess -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnrampSuccessComponent.Params(route.externalTxId),
                    componentFactory = onrampSuccessComponentFactory,
                )
            }
            is AppRoute.BuyCrypto -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = BuyCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = buyCryptoComponentFactory,
                )
            }
            is AppRoute.SellCrypto -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SellCryptoComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = sellCryptoComponentFactory,
                )
            }
            is AppRoute.SwapCrypto -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = SwapSelectTokensComponent.Params(userWalletId = route.userWalletId),
                    componentFactory = swapSelectTokensComponentFactory,
                )
            }
            is AppRoute.Onboarding -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnboardingEntryComponent.Params(
                        scanResponse = route.scanResponse,
                        multiWalletMode = when (route.mode) {
                            AppRoute.Onboarding.Mode.Onboarding -> OnboardingEntryComponent.MultiWalletMode.Onboarding
                            AppRoute.Onboarding.Mode.AddBackup -> OnboardingEntryComponent.MultiWalletMode.AddBackup
                            AppRoute.Onboarding.Mode.ContinueFinalize ->
                                OnboardingEntryComponent.MultiWalletMode.ContinueFinalize
                        },
                    ),
                    componentFactory = onboardingEntryComponentFactory,
                )
            }
            is AppRoute.Stories -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = StoriesComponent.Params(
                        storyId = route.storyId,
                        nextScreen = route.nextScreen,
                        screenSource = route.screenSource,
                    ),
                    componentFactory = storiesComponentFactory,
                )
            }
        }
        // endregion
    }

    fun doOnDestroy() {
        componentContexts.clear()
    }

    private fun contextProvider(
        appRoute: AppRoute,
        contextFactory: (route: AppRoute) -> AppComponentContext,
    ): Provider<AppComponentContext> = Provider {
        componentContexts.getOrPut(appRoute) { contextFactory(appRoute) }
    }

    private companion object {

        val componentContexts = WeakHashMap<AppRoute, AppComponentContext>()
    }
}