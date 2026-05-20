package com.tangem.features.tangempay.hotwallet

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.onboarding.api.R
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class TangemPayHotWalletOnboardingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
) : Model() {

    val uiState: StateFlow<TangemPayHotWalletOnboardingUM>
        field = MutableStateFlow(
            TangemPayHotWalletOnboardingUM(
                isLoading = false,
                onGetCardClick = ::onGetCardClick,
                onTermsClick = ::onTermsClick,
            ),
        )

    private fun onTermsClick() {
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    private fun onGetCardClick() {
        uiState.update { it.copy(isLoading = true) }

        if (!isHotWalletCreationSupported()) {
            uiMessageSender.send(
                Dialogs.hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            modelScope.launch { clearAppsFlyerDeeplinkUseCase(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding) }
            router.replaceCurrent(AppRoute.Home())
            return
        }

        modelScope.launch {
            runSuspendCatching {
                val userWallet = createHotWalletUseCase.invoke(
                    auth = HotAuth.NoAuth,
                    mnemonicType = MnemonicType.Words12,
                ).getOrElse { throw it }

                clearAppsFlyerDeeplinkUseCase(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding)

                router.replaceCurrent(
                    AppRoute.CreateWalletBackup(
                        userWalletId = userWallet.walletId,
                        analyticsSource = AnalyticsParam.ScreensSources.TangemPayHotWalletOnboarding.value,
                        analyticsAction = WalletSettingsAnalyticEvents.RecoveryPhraseScreenAction.Backup.value,
                        nextScreen = AppRoute.UpdateAccessCode(
                            userWalletId = userWallet.walletId,
                            source = AnalyticsParam.ScreensSources.TangemPayHotWalletOnboarding.value,
                            nextScreen = AppRoute.TangemPayOnboarding(
                                mode = AppRoute.TangemPayOnboarding.Mode.FirstSetup(userWallet.walletId),
                            ),
                        ),
                    ),
                )
            }.onFailure {
                uiState.update { state -> state.copy(isLoading = false) }
                uiMessageSender.send(
                    DialogMessage(
                        title = TextReference.Res(R.string.common_something_went_wrong),
                        message = TextReference.Res(R.string.common_unknown_error),
                    ),
                )
            }
        }
    }
}