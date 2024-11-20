package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.GenerateSeedPhraseUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.ImportSeedPhraseUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.SeedPhraseCheckUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.OnboardingDialogUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
internal class MultiWalletSeedPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val urlOpener: UrlOpener,
    private val tangemSdkManager: TangemSdkManager,
    private val cardRepository: CardRepository,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState get() = params.multiWalletState
    private val state = MutableStateFlow(SeedPhraseState())
    private val _uiState = MutableStateFlow(getStartState())

    private val generateSeedPhraseUiStateBuilder = GenerateSeedPhraseUiStateBuilder(
        state = state,
        updateUiState = { block -> updateUiStateSpecific(block) },
        onContinue = ::openWordsCheck,
    )

    private val seedPhraseCheckUiStateBuilder = SeedPhraseCheckUiStateBuilder(
        state = state,
        currentUiState = { _uiState.value },
        updateUiState = { block -> updateUiStateSpecific(block) },
        importWallet = ::importWallet,
    )

    private val importSeedPhraseUiStateBuilder = ImportSeedPhraseUiStateBuilder(
        modelScope = modelScope,
        state = state,
        mnemonicRepository = mnemonicRepository,
        updateUiState = { block -> updateUiStateSpecific(block) },
        importWallet = ::importWallet,
    )

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()
    val navigateBack = MutableSharedFlow<Unit>()

    fun onBack() {
        when (_uiState.value) {
            is MultiWalletSeedPhraseUM.Start -> {
                modelScope.launch { navigateBack.emit(Unit) }
            }
            is MultiWalletSeedPhraseUM.Import,
            is MultiWalletSeedPhraseUM.GenerateSeedPhrase,
            -> {
                _uiState.value = getStartState()
                state.value = SeedPhraseState()
            }
            is MultiWalletSeedPhraseUM.GeneratedWordsCheck -> {
                openGeneratedSeedPhrase()
            }
        }
    }

    private fun getStartState(): MultiWalletSeedPhraseUM {
        return MultiWalletSeedPhraseUM.Start(
            onImportSeedPhraseClicked = {
                openImportSeedPhrase()
            },
            onGenerateSeedPhraseClicked = {
                generateSeedPhrase()
                openGeneratedSeedPhrase()
            },
            onLearnMoreClicked = {
                urlOpener.openUrl(seedPhraseLearnMoreUrl())
            },
        )
    }

    private fun openGeneratedSeedPhrase() {
        // TODO analytics SeedPhraseEvents.ButtonGenerateSeedPhrase

        val words12 = state.value.generatedWords12 ?: return
        val words24 = state.value.generatedWords24 ?: return

        _uiState.value = generateSeedPhraseUiStateBuilder.getState(
            generatedWords12 = words12,
            generatedWords24 = words24,
        )
    }

    private fun openWordsCheck() {
        _uiState.value = seedPhraseCheckUiStateBuilder.getState()
    }

    private fun openImportSeedPhrase() {
        _uiState.value = importSeedPhraseUiStateBuilder.getState()
    }

    private fun generateSeedPhrase() {
        val words12 = mnemonicRepository.generateMnemonic(MnemonicRepository.MnemonicType.Words12)
        val words24 = mnemonicRepository.generateMnemonic(MnemonicRepository.MnemonicType.Words24)

        state.update {
            it.copy(
                generatedWords12 = words12,
                generatedWords24 = words24,
            )
        }
    }

    private fun importWallet(shouldReset: Boolean = false) {
        val currentState = state.value
        val mnemonic = currentState.importedMnemonic
            ?: (if (currentState.words24Option) currentState.generatedWords24 else currentState.generatedWords12)
            ?: return

        val scanResponse = params.parentParams.scanResponse

        modelScope.launch {
            val result = tangemSdkManager.importWallet(
                scanResponse = scanResponse,
                mnemonic = mnemonic.mnemonicComponents.joinToString(" "),
                passphrase = currentState.passphrase,
                shouldReset = shouldReset,
            )

            when (result) {
                is CompletionResult.Success -> {
                    // TODO on success send analytics event
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
        updateDialog(
            resetCardDialog(
                onConfirm = ::navigateToSupportScreen,
                dismiss = { updateDialog(null) },
                onDismissButtonClick = ::resetCard,
            ),
        )
    }

    private fun resetCard() = importWallet(true)

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo = getCardInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }

    private fun updateDialog(dialog: OnboardingDialogUM?) {
        _uiState.update { st ->
            when (st) {
                is MultiWalletSeedPhraseUM.GeneratedWordsCheck -> st.copy(dialog = dialog)
                is MultiWalletSeedPhraseUM.Import -> st.copy(dialog = dialog)
                else -> st
            }
        }
    }

    private inline fun <reified T : MultiWalletSeedPhraseUM> updateUiStateSpecific(block: (T) -> T) {
        _uiState.update {
            if (it !is T) return@update it
            block(it)
        }
    }
}
