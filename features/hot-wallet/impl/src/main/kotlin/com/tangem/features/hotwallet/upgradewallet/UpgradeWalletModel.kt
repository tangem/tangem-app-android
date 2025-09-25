package com.tangem.features.hotwallet.upgradewallet

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.hotwallet.UpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.entity.UpgradeWalletUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UpgradeWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val settingsRepository: SettingsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val uiMessageSender: UiMessageSender,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : Model() {
    private val params = paramsContainer.require<UpgradeWalletComponent.Params>()

    private val _uiState = MutableStateFlow(
        UpgradeWalletUM(
            onBackClick = { router.pop() },
            onBuyTangemWalletClick = ::onBuyTangemWalletClick,
            onScanDeviceClick = ::onScanDeviceClick,
        ),
    )
    internal val uiState: StateFlow<UpgradeWalletUM> = _uiState

    override fun onDestroy() {
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }

    private fun onBuyTangemWalletClick() {
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onScanDeviceClick() {
        scanCard()
    }

    private fun scanCard() {
        modelScope.launch {
            setLoading(true)

            val shouldSaveAccessCodes = settingsRepository.shouldSaveAccessCodes()
            cardSdkConfigRepository.setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = shouldSaveAccessCodes,
            )

            tangemSdkManager
                .scanProduct()
                .doOnSuccess {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    tangemSdkManager.changeDisplayedCardIdNumbersCount(it)
                    navigateToUpgradeFlow(it)
                }
                .doOnFailure {
                    showCardVerificationFailedDialog(it)
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    private fun showCardVerificationFailedDialog(error: TangemError) {
        if (error !is TangemSdkError.CardVerificationFailed) return

        // TODO [REDACTED_TASK_KEY] track error

        val resource = error.localizedDescriptionRes()
        val resId = resource.resId ?: R.string.common_unknown_error
        val resArgs = resource.args.map { it.value }

        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(id = resId, resArgs.toWrappedList()),
                title = resourceReference(id = R.string.security_alert_title),
                isDismissable = false,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(id = R.string.alert_button_request_support),
                        onClick = {
                            modelScope.launch {
                                sendFeedbackEmailUseCase(type = FeedbackEmailType.CardAttestationFailed)
                            }
                        },
                    )
                },
                secondActionBuilder = { cancelAction(onClick = {}) },
            ),
        )
    }

    private fun navigateToUpgradeFlow(scanResponse: ScanResponse) {
        setLoading(false)
        router.push(
            AppRoute.Onboarding(
                scanResponse = scanResponse,
                mode = AppRoute.Onboarding.Mode.UpgradeHotWallet(
                    userWalletId = params.userWalletId,
                ),
            ),
        )
    }
}