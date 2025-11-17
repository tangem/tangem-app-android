package com.tangem.features.hotwallet.upgradewallet

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnResult
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
import com.tangem.domain.card.BackupValidator
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.hotwallet.UpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.entity.UpgradeWalletUM
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
) : Model() {
    private val params = paramsContainer.require<UpgradeWalletComponent.Params>()

    val resetCardsComponentCallbacks = ResetCardsModelCallbacks()
    val startResetCardsFlow = MutableSharedFlow<UserWallet.Cold>()

    internal val uiState: StateFlow<UpgradeWalletUM>
        field = MutableStateFlow(
            UpgradeWalletUM(
                onBackClick = { router.pop() },
                onBuyTangemWalletClick = ::onBuyTangemWalletClick,
                onContinueClick = ::onContinueClick,
            ),
        )

    override fun onDestroy() {
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }

    private fun onBuyTangemWalletClick() {
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onContinueClick() {
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
                    // Check if user attempted to upgrade before but something went wrong and a full reset is required
                    val userWallet = coldUserWalletBuilderFactory.create(it).build()
                    val sameWalletButNotFinishedBackup by lazy {
                        userWallet?.walletId == params.userWalletId &&
                            BackupValidator.isValidFull(it.card).not()
                    }
                    val otherWalletAndAlreadyCreated by lazy {
                        userWallet?.walletId != params.userWalletId &&
                            it.card.wallets.map { it.curve }.toSet().isNotEmpty()
                    }

                    if (userWallet != null && (sameWalletButNotFinishedBackup || otherWalletAndAlreadyCreated)) {
                        startResetCardsFlow.emit(userWallet)
                        return@doOnSuccess
                    }

                    delay(DELAY_SDK_DIALOG_CLOSE)
                    tangemSdkManager.changeDisplayedCardIdNumbersCount(it)
                    navigateToUpgradeFlow(it)
                }
                .doOnFailure {
                    showCardVerificationFailedDialog(it)
                }
                .doOnResult {
                    setLoading(false)
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { it.copy(isLoading = isLoading) }
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

    inner class ResetCardsModelCallbacks : ResetCardsComponent.ModelCallbacks {
        override fun onCancel() {
            setLoading(false)
        }

        override fun onComplete() {
            setLoading(false)
        }
    }
}