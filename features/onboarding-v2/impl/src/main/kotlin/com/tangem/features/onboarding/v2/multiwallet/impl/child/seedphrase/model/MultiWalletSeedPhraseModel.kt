package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.common.ui.OnboardingDialogUM
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.GenerateSeedPhraseUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.ImportSeedPhraseUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.SeedPhraseCheckUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============================================
// | DON'T FIXME  !!! MODIFY WITH CAUTION !!!  |
// =============================================
@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MultiWalletSeedPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val urlOpener: UrlOpener,
    private val tangemSdkManager: TangemSdkManager,
    private val cardRepository: CardRepository,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analyticsHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState get() = params.multiWalletState
    private val state = MutableStateFlow(getInitialSeedPhraseState())
    private val _uiState = MutableStateFlow(getInitialUIState())

    private val generateSeedPhraseUiStateBuilder = GenerateSeedPhraseUiStateBuilder(
        updateUiState = { block -> updateUiStateSpecific(block) },
        onContinue = ::openWordsCheck,

        // =============================================
        // | DON'T FIXME  !!! MODIFY WITH CAUTION !!!  |
        // =============================================
        changeGeneratedWordsType = { newType ->
            state.update { st -> st.copy(generatedWordsType = newType) }
        },
        // =============================================
    )

    private val seedPhraseCheckUiStateBuilder = SeedPhraseCheckUiStateBuilder(
        currentState = { state.value },
        currentUiState = { _uiState.value },
        updateUiState = { block -> updateUiStateSpecific(block) },
        readyToImport = { ready -> state.update { it.copy(readyToImport = ready) } },

        // =============================================
        // | DON'T FIXME  !!! MODIFY WITH CAUTION !!!  |
        // =============================================
        importWallet = importWallet@{
            importWallet(
                mnemonic = when (state.value.generatedWordsType) {
                    GeneratedWordsType.Words12 -> state.value.generatedWords12 ?: return@importWallet
                    GeneratedWordsType.Words24 -> state.value.generatedWords24 ?: return@importWallet
                },
                passphrase = null,
                generatedSeedPhrase = true,
            )
        },
        // =============================================
    )

    private val importSeedPhraseUiStateBuilder = ImportSeedPhraseUiStateBuilder(
        modelScope = modelScope,
        mnemonicRepository = mnemonicRepository,
        updateUiState = { block -> updateUiStateSpecific(block) },
        readyToImport = { ready -> state.update { it.copy(readyToImport = ready) } },
        importWallet = { mnemonic, passphrase ->
            importWallet(
                mnemonic = mnemonic,
                passphrase = passphrase,
                generatedSeedPhrase = false,
            )
        },
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
                _uiState.value = getInitialUIState()
                state.value = getInitialSeedPhraseState()
            }
            is MultiWalletSeedPhraseUM.GeneratedWordsCheck -> {
                openGeneratedSeedPhrase()
            }
        }
    }

    private fun getInitialSeedPhraseState(): SeedPhraseState {
        return SeedPhraseState(
            generatedWords12 = null,
            generatedWords24 = null,
            generatedWordsType = GeneratedWordsType.Words12,
            readyToImport = false,
        )
    }

    private fun getInitialUIState(): MultiWalletSeedPhraseUM {
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
        val words12 = state.value.generatedWords12 ?: return
        val words24 = state.value.generatedWords24 ?: return

        _uiState.value = generateSeedPhraseUiStateBuilder.getState(
            generatedWords12 = words12,
            generatedWords24 = words24,
            option = state.value.generatedWordsType,
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

    // =============================================
    // | DON'T FIXME  !!! MODIFY WITH CAUTION !!!  |
    // =============================================
    private fun importWallet(mnemonic: Mnemonic, passphrase: String?, generatedSeedPhrase: Boolean) {
        val currentState = state.value
        if (currentState.readyToImport.not()) return

        val scanResponse = params.parentParams.scanResponse

        modelScope.launch {
            val result = tangemSdkManager.importWallet(
                scanResponse = scanResponse,
                mnemonic = mnemonic.mnemonicComponents.joinToString(" "),
                passphrase = passphrase,
                shouldReset = false,
            )

            when (result) {
                is CompletionResult.Success -> {
                    analyticsHandler.send(
                        OnboardingEvent.CreateWallet.WalletCreatedSuccessfully(
                            creationType = if (generatedSeedPhrase) {
                                OnboardingEvent.CreateWallet.WalletCreationType.NewSeed
                            } else {
                                OnboardingEvent.CreateWallet.WalletCreationType.SeedImport
                            },
                            seedPhraseLength = mnemonic.mnemonicComponents.size,
                        ),
                    )

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
    // =============================================

    private fun handleActivationError() {
        updateDialog(
            resetCardDialog(
                onConfirm = ::navigateToSupportScreen,
                dismiss = { updateDialog(null) },
                onDismissButtonClick = ::resetCard,
            ),
        )
    }

    private fun resetCard() {
        val scanResponse = params.parentParams.scanResponse

        modelScope.launch {
            tangemSdkManager.resetToFactorySettings(
                cardId = scanResponse.card.cardId,
                allowsRequestAccessCodeFromRepository = true,
            )
        }
    }

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo =
                getWalletMetaInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
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