package com.tangem.tap.features.home

import androidx.compose.runtime.Stable
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.features.home.redux.HIDE_PROGRESS_DELAY
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeMiddleware.NEW_BUY_WALLET_URL
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class HomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val scanCardProcessor: ScanCardProcessor,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val userWalletBuilderFactory: UserWalletBuilder.Factory,
) : Model() {

    private val tangemErrorHandler = TangemTangemErrorsHandler(store)

    fun onScanClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonScanCard())
        scanCard()
    }

    fun onShopClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonBuyCards())
        analyticsEventHandler.send(Shop.ScreenOpened())

        Firebase.analytics.appInstanceId
            .addOnSuccessListener { urlOpener.openUrl(url = "$NEW_BUY_WALLET_URL&app_instance_id=$it") }
            .addOnFailureListener { urlOpener.openUrl(url = NEW_BUY_WALLET_URL) }
    }

    fun onSearchClick() {
        analyticsEventHandler.send(IntroductionProcess.ButtonTokensList())

        store.dispatch(TokensAction.SetArgs.ReadAccess)
        store.dispatchNavigationAction { push(AppRoute.ManageTokens(Source.STORIES)) }
    }

    private fun scanCard() {
        modelScope.launch {
            cardSdkConfigRepository.isBiometricsRequestPolicy = settingsRepository.shouldSaveAccessCodes()

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
                    tangemErrorHandler.onErrorReceived(error = it)
                    delay(HIDE_PROGRESS_DELAY)
                    store.dispatch(HomeAction.ScanInProgress(scanInProgress = false))
                },
                onSuccess = ::proceedWithScanResponse,
            )
        }
    }

    private suspend fun proceedWithScanResponse(scanResponse: ScanResponse) {
        val userWallet = userWalletBuilderFactory.create(scanResponse = scanResponse).build()

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

        store.dispatchWithMain(HomeAction.ScanInProgress(scanInProgress = false))
        delay(HIDE_PROGRESS_DELAY)

        store.dispatchNavigationAction { replaceAll(AppRoute.Wallet) }
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