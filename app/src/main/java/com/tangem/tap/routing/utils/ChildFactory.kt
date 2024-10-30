package com.tangem.tap.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.feature.referral.ReferralFragment
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.disclaimer.api.components.DisclaimerComponent
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.onramp.component.BuyCryptoComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.component.SellCryptoComponent
import com.tangem.features.pushnotifications.api.navigation.PushNotificationsRouter
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.staking.api.navigation.StakingRouter
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorFragment
import com.tangem.tap.features.details.ui.appsettings.AppSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsFragment
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.AccessCodeRecoveryFragment
import com.tangem.tap.features.details.ui.resetcard.ResetCardFragment
import com.tangem.tap.features.details.ui.securitymode.SecurityModeFragment
import com.tangem.tap.features.details.ui.walletconnect.WalletConnectFragment
import com.tangem.tap.features.home.HomeFragment
import com.tangem.tap.features.main.ui.ModalNotificationBottomSheetFragment
import com.tangem.tap.features.onboarding.products.note.OnboardingNoteFragment
import com.tangem.tap.features.onboarding.products.otherCards.OnboardingOtherCardsFragment
import com.tangem.tap.features.onboarding.products.twins.ui.OnboardingTwinsFragment
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.features.saveWallet.ui.SaveWalletBottomSheetFragment
import com.tangem.tap.features.welcome.ui.WelcomeFragment
import com.tangem.tap.routing.RoutingComponent.Child
import com.tangem.utils.Provider
import dagger.hilt.android.scopes.ActivityScoped
import java.util.WeakHashMap
import javax.inject.Inject

@ActivityScoped
@Suppress("LongParameterList")
internal class ChildFactory @Inject constructor(
    private val detailsComponentFactory: DetailsComponent.Factory,
    private val walletSettingsComponentFactory: WalletSettingsComponent.Factory,
    private val disclaimerComponentFactory: DisclaimerComponent.Factory,
    private val manageTokensComponentFactory: ManageTokensComponent.Factory,
    private val marketsTokenDetailsComponentFactory: MarketsTokenDetailsComponent.Factory,
    private val onrampComponentFactory: OnrampComponent.Factory,
    private val buyCryptoComponentFactory: BuyCryptoComponent.Factory,
    private val sellCryptoComponentFactory: SellCryptoComponent.Factory,
    private val onboardingEntryComponentFactory: OnboardingEntryComponent.Factory,
    private val sendRouter: SendRouter,
    private val tokenDetailsRouter: TokenDetailsRouter,
    private val walletRouter: WalletRouter,
    private val qrScanningRouter: QrScanningRouter,
    private val stakingRouter: StakingRouter,
    private val testerRouter: TesterRouter,
    private val pushNotificationRouter: PushNotificationsRouter,
) {

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun createChild(route: AppRoute, contextFactory: (route: AppRoute) -> AppComponentContext): Child {
        componentContexts[route] = contextFactory(route)

        return when (route) {
            is AppRoute.Initial -> {
                Child.Initial
            }
            is AppRoute.AccessCodeRecovery -> {
                route.asFragmentChild(Provider { AccessCodeRecoveryFragment() })
            }
            is AppRoute.AppCurrencySelector -> {
                route.asFragmentChild(Provider { AppCurrencySelectorFragment() })
            }
            is AppRoute.ModalNotification -> {
                route.asFragmentChild(Provider { ModalNotificationBottomSheetFragment() })
            }
            is AppRoute.SaveWallet -> {
                route.asFragmentChild(Provider { SaveWalletBottomSheetFragment() })
            }
            is AppRoute.Send -> {
                route.asFragmentChild(Provider { sendRouter.getEntryFragment() })
            }
            is AppRoute.AppSettings -> {
                route.asFragmentChild(Provider { AppSettingsFragment() })
            }
            is AppRoute.CardSettings -> {
                route.asFragmentChild(Provider { CardSettingsFragment() })
            }
            is AppRoute.Details -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = DetailsComponent.Params(route.userWalletId),
                    componentFactory = detailsComponentFactory,
                )
            }
            is AppRoute.DetailsSecurity -> {
                route.asFragmentChild(Provider { SecurityModeFragment() })
            }
            is AppRoute.Disclaimer -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = DisclaimerComponent.Params(route.isTosAccepted),
                    componentFactory = disclaimerComponentFactory,
                )
            }
            is AppRoute.Home -> {
                route.asFragmentChild(Provider { HomeFragment() })
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
                route.asFragmentChild(Provider { qrScanningRouter.getEntryFragment() })
            }
            is AppRoute.ReferralProgram -> {
                route.asFragmentChild(Provider { ReferralFragment() })
            }
            is AppRoute.ResetToFactory -> {
                route.asFragmentChild(Provider { ResetCardFragment() })
            }
            is AppRoute.Swap -> {
                route.asFragmentChild(Provider { SwapFragment() })
            }
            is AppRoute.Wallet -> {
                route.asFragmentChild(Provider { walletRouter.getEntryFragment() })
            }
            is AppRoute.WalletConnectSessions -> {
                route.asFragmentChild(Provider { WalletConnectFragment() })
            }
            is AppRoute.CurrencyDetails -> {
                route.asFragmentChild(Provider { tokenDetailsRouter.getEntryFragment() })
            }
            is AppRoute.Welcome -> {
                route.asFragmentChild(Provider { WelcomeFragment() })
            }
            is AppRoute.TesterMenu -> {
                Child.LegacyIntent(testerRouter.getEntryIntent())
            }
            is AppRoute.Staking -> {
                route.asFragmentChild(Provider { stakingRouter.getEntryFragment() })
            }
            is AppRoute.PushNotification -> {
                route.asFragmentChild(Provider { pushNotificationRouter.entryFragment() })
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
                    params = OnrampComponent.Params(),
                    componentFactory = onrampComponentFactory,
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
            is AppRoute.Onboarding -> {
                route.asComponentChild(
                    contextProvider = contextProvider(route, contextFactory),
                    params = OnboardingEntryComponent.Params(route.scanResponse),
                    componentFactory = onboardingEntryComponentFactory,
                )
            }
        }
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
