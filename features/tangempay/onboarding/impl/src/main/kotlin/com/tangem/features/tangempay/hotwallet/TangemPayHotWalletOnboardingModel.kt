package com.tangem.features.tangempay.hotwallet

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.onboarding.api.R
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class TangemPayHotWalletOnboardingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
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
        router.push(AppRoute.Disclaimer(isTosAccepted = true))
    }

    private fun onGetCardClick() {
        TangemLogger.i("[TangemPay][HWO]onGetCardClick")
        uiState.update { it.copy(isLoading = true) }

        val isSupported = isHotWalletCreationSupported()
        TangemLogger.i("[TangemPay][HWO]Hot wallet creation supported=$isSupported")
        if (!isSupported) {
            uiMessageSender.send(
                Dialogs.hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            router.replaceCurrent(AppRoute.Home())
            return
        }

        modelScope.launch {
            TangemLogger.i("[TangemPay][HWO]Creating hot wallet")
            runSuspendCatching {
                val userWallet = createHotWalletUseCase.invoke(
                    auth = HotAuth.NoAuth,
                    mnemonicType = MnemonicType.Words12,
                ).getOrElse { throw it }

                TangemLogger.i("[TangemPay][HWO]Hot wallet created")
                router.replaceAll(
                    AppRoute.TangemPayOnboarding(
                        mode = AppRoute.TangemPayOnboarding.Mode.FirstSetup(userWallet.walletId),
                    ),
                )
            }.onFailure {
                TangemLogger.e("[TangemPay][HWO] Failed to create hot wallet")
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