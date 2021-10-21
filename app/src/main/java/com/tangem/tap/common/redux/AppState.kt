package com.tangem.tap.common.redux

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
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletMiddleware
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.send.redux.middlewares.SendMiddleware
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.tokens.redux.TokensMiddleware
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.twins.redux.CreateTwinWalletMiddleware
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.middlewares.WalletMiddleware
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
    val sendState: SendState = SendState(),
    val detailsState: DetailsState = DetailsState(),
    val disclaimerState: DisclaimerState = DisclaimerState(),
    val tokensState: TokensState = TokensState(),
    val walletConnectState: WalletConnectState = WalletConnectState(),
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                logMiddleware, navigationMiddleware, notificationsMiddleware,
                GlobalMiddleware.handler,
                HomeMiddleware.handler,
                OnboardingNoteMiddleware.handler,
                OnboardingWalletMiddleware.handler,
                OnboardingOtherCardsMiddleware.handler,
                WalletMiddleware().walletMiddleware,
                CreateTwinWalletMiddleware.handler,
                SendMiddleware().sendMiddleware,
                DetailsMiddleware().detailsMiddleware,
                DisclaimerMiddleware().disclaimerMiddleware,
                TokensMiddleware().tokensMiddleware,
                WalletConnectMiddleware().walletConnectMiddleware
            )
        }
    }
}

