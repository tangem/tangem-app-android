package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.GlobalMiddleware
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.legacy.LegacyMiddleware
import com.tangem.tap.features.details.redux.DetailsMiddleware
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectMiddleware
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
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
import com.tangem.tap.features.saveWallet.redux.SaveWalletMiddleware
import com.tangem.tap.features.saveWallet.redux.SaveWalletState
import com.tangem.tap.features.wallet.redux.middlewares.TradeCryptoMiddleware
import com.tangem.tap.features.welcome.redux.WelcomeMiddleware
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.proxy.redux.DaggerGraphMiddleware
import com.tangem.tap.proxy.redux.DaggerGraphState
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
    val globalState: GlobalState = GlobalState(),
    val homeState: HomeState = HomeState(),
    val onboardingNoteState: OnboardingNoteState = OnboardingNoteState(),
    val onboardingWalletState: OnboardingWalletState = OnboardingWalletState(),
    val onboardingOtherCardsState: OnboardingOtherCardsState = OnboardingOtherCardsState(),
    val twinCardsState: TwinCardsState = TwinCardsState(),
    val detailsState: DetailsState = DetailsState(),
    val walletConnectState: WalletConnectState = WalletConnectState(),
    val welcomeState: WelcomeState = WelcomeState(),
    val saveWalletState: SaveWalletState = SaveWalletState(),
    val daggerGraphState: DaggerGraphState = DaggerGraphState(),
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                logMiddleware,
                notificationsMiddleware,
                GlobalMiddleware.handler,
                HomeMiddleware.handler,
                OnboardingNoteMiddleware.handler,
                OnboardingWalletMiddleware.handler,
                OnboardingOtherCardsMiddleware.handler,
                TwinCardsMiddleware.handler,
                DetailsMiddleware().detailsMiddleware,
                WalletConnectMiddleware().walletConnectMiddleware,
                BackupMiddleware().backupMiddleware,
                WelcomeMiddleware().middleware,
                SaveWalletMiddleware().middleware,
                LockUserWalletsTimerMiddleware().middleware,
                AccessCodeRequestPolicyMiddleware().middleware,
                DaggerGraphMiddleware.daggerGraphMiddleware,
                LegacyMiddleware.legacyMiddleware,
                TradeCryptoMiddleware.middleware,
            )
        }
    }
}
