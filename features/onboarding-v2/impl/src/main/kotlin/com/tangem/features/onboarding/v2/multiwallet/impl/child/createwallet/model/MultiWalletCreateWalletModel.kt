package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state.MultiWalletCreateWalletUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
internal class MultiWalletCreateWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val cardRepository: CardRepository,
    private val analyticsHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState
        get() = params.multiWalletState

    private val _uiState = MutableStateFlow(
        MultiWalletCreateWalletUM(
            title = if (params.parentParams.withSeedPhraseFlow) {
                resourceReference(R.string.onboarding_create_wallet_options_title)
            } else {
                resourceReference(R.string.onboarding_create_wallet_header)
            },
            bodyText = if (params.parentParams.withSeedPhraseFlow) {
                resourceReference(R.string.onboarding_create_wallet_options_message)
            } else {
                resourceReference(R.string.onboarding_create_wallet_body)
            },
            onCreateWalletClick = {
                analyticsHandler.send(OnboardingEvent.CreateWallet.ButtonCreateWallet)
                createWallet(false)
            },
            showOtherOptionsButton = params.parentParams.withSeedPhraseFlow,
            onOtherOptionsClick = {
                modelScope.launch {
                    onDone.emit(OnboardingMultiWalletState.Step.SeedPhrase)
                }
            },
            dialog = null,
        ),
    )

    val uiState: StateFlow<MultiWalletCreateWalletUM> = _uiState
    val onDone = MutableSharedFlow<OnboardingMultiWalletState.Step>()

    init {
        analyticsHandler.send(OnboardingEvent.CreateWallet.ScreenOpened)
    }

    private fun createWallet(shouldReset: Boolean) {
        modelScope.launch {
            val result = tangemSdkManager.createProductWallet(
                scanResponse = multiWalletState.value.currentScanResponse,
                shouldReset = shouldReset,
            )

            when (result) {
                is CompletionResult.Success -> {
                    multiWalletState.update {
                        it.copy(
                            currentScanResponse = it.currentScanResponse.copy(
                                card = result.data.card,
                                derivedKeys = result.data.derivedKeys,
                                primaryCard = result.data.primaryCard,
                            ),
                        )
                    }

                    cardRepository.startCardActivation(cardId = result.data.card.cardId)

                    if (params.parentParams.withSeedPhraseFlow) {
                        onDone.emit(OnboardingMultiWalletState.Step.AddBackupDevice)
                    } else {
                        onDone.emit(OnboardingMultiWalletState.Step.ChooseBackupOption)
                    }

                    analyticsHandler.send(OnboardingEvent.CreateWallet.WalletCreatedSuccessfully())
                }

                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.WalletAlreadyCreated) {
                        // show should reset dialog
                        handleActivationError()
                    }
                }
            }
        }
    }

    private fun handleActivationError() {
        _uiState.update { state ->
            state.copy(
                dialog = resetCardDialog(
                    onConfirm = ::navigateToSupportScreen,
                    dismiss = { _uiState.update { it.copy(dialog = null) } },
                    onDismissButtonClick = ::resetCard,
                ),
            )
        }
    }

    private fun resetCard() {
        createWallet(true)
    }

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo = getCardInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }
}