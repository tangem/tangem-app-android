package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store

class TwinsMiddleware {
    fun handle(action: WalletAction.TwinsAction) {
        when (action) {
            is WalletAction.TwinsAction.SetTwinCard -> {
                val showOnboarding = !preferencesStorage.wasTwinsOnboardingShown()
                if (showOnboarding) store.dispatch(WalletAction.TwinsAction.ShowOnboarding)
            }
            WalletAction.TwinsAction.SetOnboardingShown -> {
                preferencesStorage.saveTwinsOnboardingShown()
            }
        }
    }
}