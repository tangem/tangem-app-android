package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state.MultiWalletCreateWalletUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
class MultiWalletCreateWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val cardRepository: CardRepository,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState
        get() = params.multiWalletState

    private val _uiState = MutableStateFlow(
        MultiWalletCreateWalletUM(
            onCreateWalletClick = { createWallet(false) },
            showSeedPhraseOption = params.parentParams.withSeedPhraseFlow,
            onOtherOptionsClick = { /* navigate */ },
            dialog = null,
        ),
    )

    val uiState: StateFlow<MultiWalletCreateWalletUM> = _uiState
    val onDone = MutableSharedFlow<Unit>()

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
                    onDone.emit(Unit)
                    // TODO
                    // Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
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
