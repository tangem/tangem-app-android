package com.tangem.tap.features.home.redux

import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
import com.tangem.common.doOnSuccess
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.postUiDelayBg
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.scanCard.ScanCardProcessor
import com.tangem.tap.domain.scanCard.chains.DisclaimerChainData
import com.tangem.tap.features.home.BELARUS_COUNTRY_CODE
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.home.redux.HomeMiddleware.Companion.BUY_WALLET_URL
import com.tangem.tap.features.send.redux.states.ButtonState
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

class HomeMiddleware {
    companion object {
        val handler = homeMiddleware

        const val BUY_WALLET_URL = "https://tangem.com/ru/resellers/"
    }
}

private val homeMiddleware: Middleware<AppState> = { _, _ ->
    { next ->
        { action ->
            handleHomeAction(action)
            next(action)
        }
    }
}

private fun handleHomeAction(action: Action) {
    when (action) {
        is HomeAction.Init -> {
            store.dispatch(GlobalAction.RestoreAppCurrency)
            store.dispatch(GlobalAction.ExchangeManager.Init)
            store.dispatch(GlobalAction.FetchUserCountry)
        }
        is HomeAction.ShouldScanCardOnResume -> {
            if (action.shouldScanCard) {
                store.dispatch(HomeAction.ShouldScanCardOnResume(false))
                postUiDelayBg(700) { store.dispatch(HomeAction.ReadCard) }
            }
        }
        is HomeAction.ReadCard -> readCard()
        is HomeAction.GoToShop -> {
            when (action.userCountryCode) {
                RUSSIA_COUNTRY_CODE, BELARUS_COUNTRY_CODE -> store.dispatchOpenUrl(BUY_WALLET_URL)
                else -> store.dispatch(NavigationAction.NavigateTo(AppScreen.Shop))
            }
        }
    }
}

private fun readCard() = scope.launch {
    delay(timeMillis = 200)
    Timber.d("readCardChain")
    tangemSdkManager.setAccessCodeRequestPolicy(
        useBiometricsForAccessCode = preferencesStorage.shouldSaveAccessCodes,
    )
    changeButtonState(ButtonState.PROGRESS)

    val processor = ScanCardProcessor.scan(
        disclaimerChainData = DisclaimerChainData(AppScreen.Home),
        onScanStateChange = { store.dispatchOnMain(HomeAction.ScanInProgress(it)) },
        cardScannedEvent = IntroductionProcess.CardWasScanned(),
    )

    when (val result = processor.launch()) {
        is ChainResult.Success -> {
            val scanResponse = result.data
            if (preferencesStorage.shouldSaveUserWallets) {
                val userWallet = UserWalletBuilder(scanResponse).build() ?: return@launch

                userWalletsListManager.save(userWallet)
                    .doOnFailure { error ->
                        Timber.e(error, "Unable to save user wallet")
                        store.onCardScanned(scanResponse)
                    }
                    .doOnSuccess {
                        scope.launch { store.onUserWalletSelected(userWallet) }
                    }
                    .doOnResult {
                        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                        changeButtonState(ButtonState.ENABLED)
                    }
            } else {
                store.onCardScanned(scanResponse)
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
                changeButtonState(ButtonState.ENABLED)
            }
        }
        is ChainResult.Failure -> {
            changeButtonState(ButtonState.ENABLED)
        }
    }
}

private fun changeButtonState(state: ButtonState) {
    store.dispatchOnMain(HomeAction.ChangeScanCardButtonState(IndeterminateProgressButton(state)))
}
