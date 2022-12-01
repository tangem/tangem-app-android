package com.tangem.tap.common.redux

import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.domainStore
import com.tangem.domain.redux.global.NetworkServices
import com.tangem.tap.common.redux.global.GlobalMiddleware
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.NavigationState
import com.tangem.tap.common.redux.navigation.navigationMiddleware
import com.tangem.tap.features.details.redux.DetailsMiddleware
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectMiddleware
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.features.disclaimer.redux.DisclaimerMiddleware
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.features.home.redux.HomeMiddleware
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteMiddleware
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteState
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsMiddleware
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsState
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsMiddleware
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupMiddleware
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletMiddleware
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayMiddleware
import com.tangem.tap.features.saveWallet.redux.SaveWalletMiddleware
import com.tangem.tap.features.saveWallet.redux.SaveWalletState
import com.tangem.tap.features.send.redux.middlewares.SendMiddleware
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.shop.redux.ShopMiddleware
import com.tangem.tap.features.shop.redux.ShopState
import com.tangem.tap.features.tokens.redux.TokensMiddleware
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.middlewares.WalletMiddleware
import com.tangem.tap.features.walletSelector.redux.WalletSelectorMiddleware
import com.tangem.tap.features.walletSelector.redux.WalletSelectorState
import com.tangem.tap.features.welcome.redux.WelcomeMiddleware
import com.tangem.tap.features.welcome.redux.WelcomeState
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
    val navigationState: NavigationState = NavigationState(),
    val globalState: GlobalState = GlobalState(),
    val homeState: HomeState = HomeState(),
    val onboardingNoteState: OnboardingNoteState = OnboardingNoteState(),
    val onboardingWalletState: OnboardingWalletState = OnboardingWalletState(),
    val onboardingOtherCardsState: OnboardingOtherCardsState = OnboardingOtherCardsState(),
    val walletState: WalletState = WalletState(),
    val twinCardsState: TwinCardsState = TwinCardsState(),
    val sendState: SendState = SendState(),
    val detailsState: DetailsState = DetailsState(),
    val disclaimerState: DisclaimerState = DisclaimerState(),
    val tokensState: TokensState = TokensState(),
    val walletConnectState: WalletConnectState = WalletConnectState(),
    val shopState: ShopState = ShopState(),
    val welcomeState: WelcomeState = WelcomeState(),
    val saveWalletState: SaveWalletState = SaveWalletState(),
    val walletSelectorState: WalletSelectorState = WalletSelectorState(),
) : StateType {

    val domainState: DomainState
        get() = domainStore.state

    val domainNetworks: NetworkServices
        get() = domainState.globalState.networkServices

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                logMiddleware,
                navigationMiddleware,
                notificationsMiddleware,
                GlobalMiddleware.handler,
                HomeMiddleware.handler,
                OnboardingNoteMiddleware.handler,
                OnboardingWalletMiddleware.handler,
                OnboardingSaltPayMiddleware.handler,
                OnboardingOtherCardsMiddleware.handler,
                WalletMiddleware().walletMiddleware,
                TwinCardsMiddleware.handler,
                SendMiddleware().sendMiddleware,
                DetailsMiddleware().detailsMiddleware,
                DisclaimerMiddleware().disclaimerMiddleware,
                TokensMiddleware().tokensMiddleware,
                WalletConnectMiddleware().walletConnectMiddleware,
                BackupMiddleware().backupMiddleware,
                ShopMiddleware().shopMiddleware,
                WelcomeMiddleware().middleware,
                SaveWalletMiddleware().middleware,
                WalletSelectorMiddleware().middleware,
                LockUserWalletsTimerMiddleware().middleware,
            )
        }
    }
}
