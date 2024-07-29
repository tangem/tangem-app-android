package com.tangem.tap.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.SetAccessCodeRequestPolicyUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.ShouldSaveAccessCodesUseCase
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.home.redux.HIDE_PROGRESS_DELAY
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val shouldSaveAccessCodesUseCase: ShouldSaveAccessCodesUseCase,
    private val setAccessCodeRequestPolicyUseCase: SetAccessCodeRequestPolicyUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    fun onScanClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard())
        scanCard()
    }

    fun onShopClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonBuyCards())
        analyticsEventHandler.send(Shop.ScreenOpened())

        urlOpener.openUrl(NEW_BUY_WALLET_URL)
    }

    fun onSearchClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonTokensList())

        store.dispatch(TokensAction.SetArgs.ReadAccess)
        store.dispatchNavigationAction { push(AppRoute.ManageTokens) }
    }

    private fun scanCard() {
        viewModelScope.launch {
            setAccessCodeRequestPolicyUseCase(isBiometricsRequestPolicy = shouldSaveAccessCodesUseCase())

            scanCardProcessor.scan(
                analyticsSource = AnalyticsParam.ScreensSources.Intro,
                onProgressStateChange = { showProgress ->
                    if (showProgress) {
                        store.dispatch(HomeAction.ScanInProgress(scanInProgress = true))
                    } else {
                        delay(HIDE_PROGRESS_DELAY)
                        store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
                    }
                },
                onFailure = {
                    Timber.e(it, "Unable to scan card")
                    delay(HIDE_PROGRESS_DELAY)
                    store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
                },
                onSuccess = ::proceedWithScanResponse,
            )
        }
    }

    private suspend fun proceedWithScanResponse(scanResponse: ScanResponse) {
        val userWallet = UserWalletBuilder(
            scanResponse = scanResponse,
            generateWalletNameUseCase = generateWalletNameUseCase,
        ).build()

        if (userWallet == null) {
            Timber.e("User wallet not created")
            return
        }

        saveWalletUseCase(userWallet).fold(
            ifLeft = { Timber.e(it.toString(), "Unable to save user wallet") },
            ifRight = {
                sendSignedInCardAnalyticsEvent(scanResponse)
                coroutineScope { store.onUserWalletSelected(userWallet = userWallet) }
            },
        )

        store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
        delay(HIDE_PROGRESS_DELAY)

        store.dispatchNavigationAction { push(AppRoute.Wallet) }
    }

    private fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse) {
        val currency = ParamCardCurrencyConverter().convert(value = scanResponse.cardTypesResolver)

        if (currency != null) {
            Analytics.send(
                event = Basic.SignedIn(
                    currency = currency,
                    batch = scanResponse.card.batchId,
                    signInType = Basic.SignedIn.SignInType.Card,
                    walletsCount = "1",
                    hasBackup = scanResponse.card.backupStatus?.isActive,
                ),
            )
        }
    }
}
