package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.CompletionResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.domain.SeedPhraseInteractor
import com.tangem.feature.onboarding.presentation.wallet2.analytics.OnboardingSeedButtonOtherOptions
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseEvents
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.feature.onboarding.presentation.wallet2.model.*
import com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.extensions.isEven
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
@HiltViewModel
class SeedPhraseViewModel @Inject constructor(
    private val interactor: SeedPhraseInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    private var uiBuilder = StateBuilder(uiActions = createUiActions())

    var uiState: OnboardingSeedPhraseState by mutableStateOf(uiBuilder.init())
        private set

    private lateinit var router: SeedPhraseRouter
    private lateinit var mediator: SeedPhraseMediator

    val currentScreen: StateFlow<SeedPhraseScreen>
        get() = router.currentScreen

    val progress: Flow<Int>
        get() = currentScreen.map { progressByScreen[it] ?: 0 }

    val maxProgress: Int by lazy { progressByScreen.maxOfOrNull { it.value } ?: 0 }

    var isFinished: Boolean = false
        private set

    private val progressByScreen = mapOf(
        SeedPhraseScreen.Intro to 1,
        SeedPhraseScreen.AboutSeedPhrase to 2,
        SeedPhraseScreen.YourSeedPhrase to 3,
        SeedPhraseScreen.CheckSeedPhrase to 3,
        SeedPhraseScreen.ImportSeedPhrase to 3,
    )

    private val textFieldsDebouncers = mutableMapOf<String, Debouncer>()

    private var generatedMnemonicComponents: List<String>? = null
    private var importedMnemonicComponents: List<String>? = null

    override fun onCleared() {
        textFieldsDebouncers.forEach { entry -> entry.value.release() }
        textFieldsDebouncers.clear()
        super.onCleared()
    }

    fun setRouter(router: SeedPhraseRouter) {
        this.router = router
        subscribeToScreenChanges()
    }

    private fun subscribeToScreenChanges() {
        router.currentScreen.onEach { screen ->
            when (screen) {
                SeedPhraseScreen.Intro -> {
                    analyticsEventHandler.send(SeedPhraseEvents.IntroScreenOpened)
                }

                SeedPhraseScreen.AboutSeedPhrase -> {
                    mediator.allowScreenshots(true)
                }

                SeedPhraseScreen.YourSeedPhrase -> {
                    analyticsEventHandler.send(SeedPhraseEvents.GenerationScreenOpened)
                    mediator.allowScreenshots(false)
                }

                SeedPhraseScreen.CheckSeedPhrase -> {
                    analyticsEventHandler.send(SeedPhraseEvents.CheckingScreenOpened)
                    mediator.allowScreenshots(true)
                }

                SeedPhraseScreen.ImportSeedPhrase -> {
                    analyticsEventHandler.send(SeedPhraseEvents.ImportScreenOpened)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun setMediator(mediator: SeedPhraseMediator) {
        this.mediator = mediator
    }

    private fun createUiActions(): UiActions = UiActions(
        introActions = IntroUiAction(
            buttonCreateWalletClick = ::buttonCreateWalletClick,
            buttonOtherOptionsClick = ::buttonOtherOptionsClick,
        ),
        aboutActions = AboutUiAction(
            buttonReadMoreAboutSeedPhraseClick = ::buttonReadMoreAboutSeedPhraseClick,
            buttonGenerateSeedPhraseClick = ::buttonGenerateSeedPhraseClick,
            buttonImportSeedPhraseClick = ::buttonImportSeedPhraseClick,
        ),
        yourSeedPhraseActions = YourSeedPhraseUiAction(
            buttonContinueClick = ::buttonContinueClick,
        ),
        checkSeedPhraseActions = CheckSeedPhraseUiAction(
            buttonCreateWalletClick = {
                buttonImportWalletClick(generatedMnemonicComponents, SeedPhraseSource.GENERATED)
            },
            secondTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Second, value) },
            ),
            seventhTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Seventh, value) },
            ),
            eleventhTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Eleventh, value) },
            ),
        ),
        importSeedPhraseActions = ImportSeedPhraseUiAction(
            phraseTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onSeedPhraseTextFieldChanged(value) },
            ),
            suggestedPhraseClick = ::buttonSuggestedPhraseClick,
            buttonCreateWalletClick = {
                buttonImportWalletClick(importedMnemonicComponents, SeedPhraseSource.IMPORTED)
            },
        ),
        menuChatClick = ::menuChatClick,
        menuNavigateBackClick = ::menuNavigateBackClick,
    )

    // region CheckSeedPhrase
    private fun onTextFieldChanged(field: SeedPhraseField, textFieldValue: TextFieldValue) {
        viewModelScope.launchSingle {
            updateUi { uiBuilder.checkSeedPhrase.updateTextField(uiState, field, textFieldValue) }

            val fieldState = field.getState(uiState)
            if (fieldState.textFieldValue.text.isEmpty()) {
                updateUi { uiBuilder.checkSeedPhrase.updateTextFieldError(uiState, field, hasError = false) }
                return@launchSingle
            }

            createOrGetDebouncer(field.name).debounce(viewModelScope, context = dispatchers.io) {
                val hasError = !interactor.isWordMatch(textFieldValue.text)
                if (fieldState.isError != hasError) {
                    updateUi { uiBuilder.checkSeedPhrase.updateTextFieldError(uiState, field, hasError) }
                }
                val isCreateWalletButtonEnabled = SeedPhraseField.values()
                    .map { field -> field.getState(uiState) }
                    .all { fieldState -> fieldState.textFieldValue.text.isNotEmpty() && !fieldState.isError }

                if (uiState.checkSeedPhraseState.buttonCreateWallet.enabled != isCreateWalletButtonEnabled) {
                    updateUi {
                        uiBuilder.checkSeedPhrase.updateCreateWalletButton(
                            uiState = uiState,
                            enabled = isCreateWalletButtonEnabled,
                        )
                    }
                }
            }
        }
    }
    // endregion CheckSeedPhrase

    // region ImportSeedPhrase
    private fun onSeedPhraseTextFieldChanged(textFieldValue: TextFieldValue) {
        val oldTextFieldValue = uiState.importSeedPhraseState.tvSeedPhrase.textFieldValue
        val isSameText = textFieldValue.text == oldTextFieldValue.text
        val isCursorMoved = textFieldValue.selection != oldTextFieldValue.selection

        uiState = uiBuilder.importSeedPhrase.updateTextField(uiState, textFieldValue)

        val fieldState = uiState.importSeedPhraseState.tvSeedPhrase
        val inputMnemonic = fieldState.textFieldValue.text

        val debouncer = createOrGetDebouncer(MNEMONIC_DEBOUNCER)
        when {
            isSameText && isCursorMoved -> {
                debouncer.debounce(viewModelScope, MNEMONIC_DEBOUNCE_DELAY, dispatchers.single) {
                    updateSuggestions(fieldState)
                }
            }
            isSameText && !isCursorMoved -> {
                // do nothing
            }
            else -> {
                debouncer.debounce(viewModelScope, MNEMONIC_DEBOUNCE_DELAY, dispatchers.single) {
                    updateSuggestions(fieldState)
                    validateMnemonic(inputMnemonic)
                }
            }
        }
    }

    private suspend fun validateMnemonic(inputMnemonic: String) {
        if (inputMnemonic.isEmpty() && uiState.importSeedPhraseState.error != null) {
            val mediateState = uiBuilder.importSeedPhrase.updateCreateWalletButton(uiState, enabled = false)
            updateUi {
                uiBuilder.importSeedPhrase.updateError(mediateState, null)
            }
            return
        }

        interactor.validateMnemonicString(inputMnemonic)
            .onSuccess { mnemonicComponents ->
                importedMnemonicComponents = mnemonicComponents
                val mediateState = uiBuilder.importSeedPhrase.updateCreateWalletButton(uiState, enabled = true)
                updateUi { uiBuilder.importSeedPhrase.updateError(mediateState, null) }
            }
            .onFailure {
                uiState = uiBuilder.importSeedPhrase.updateCreateWalletButton(uiState, enabled = false)
                val error = it as? SeedPhraseError ?: return

                importedMnemonicComponents = null
                val mediateState = when (error) {
                    is SeedPhraseError.InvalidWords -> {
                        uiBuilder.importSeedPhrase.updateInvalidWords(uiState, error.words)
                    }
                    else -> uiState
                }
                updateUi { uiBuilder.importSeedPhrase.updateError(mediateState, error) }
            }
    }

    private suspend fun updateSuggestions(fieldState: TextFieldState) {
        val suggestions = interactor.getSuggestions(
            text = fieldState.textFieldValue.text,
            hasSelection = !fieldState.textFieldValue.selection.collapsed,
            cursorPosition = fieldState.textFieldValue.selection.end,
        )
        updateUi { uiBuilder.importSeedPhrase.updateSuggestions(uiState, suggestions) }
    }
    // endregion ImportSeedPhrase

    // region ButtonClickHandlers
    private fun buttonCreateWalletClick() {
        viewModelScope.launch(dispatchers.io) {
            mediator.createWallet(::handleWalletCreationResult)
        }
    }

    private fun buttonImportWalletClick(mnemonicComponents: List<String>?, seedPhraseSource: SeedPhraseSource) {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonImport)
        mnemonicComponents ?: return

        viewModelScope.launch(dispatchers.io) {
            mediator.importWallet(mnemonicComponents, seedPhraseSource, ::handleWalletCreationResult)
        }
    }

    private fun handleWalletCreationResult(result: CompletionResult<CreateWalletResponse>) {
        when (result) {
            is CompletionResult.Success -> {
                isFinished = true
            }

            is CompletionResult.Failure -> {
                // errors shows on the TangemSdk bottom sheet dialog
            }
        }
        mediator.onWalletCreated(result)
    }

    private fun menuNavigateBackClick() {
        router.navigateBack()
    }

    private fun menuChatClick() {
        router.openChat()
    }

    private fun buttonOtherOptionsClick() {
        analyticsEventHandler.send(OnboardingSeedButtonOtherOptions)
        router.openScreen(SeedPhraseScreen.AboutSeedPhrase)
    }

    private fun buttonReadMoreAboutSeedPhraseClick() {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonReadMore)
        router.openUri(URI_ABOUT_SEED_PHRASE)
    }

    private fun buttonGenerateSeedPhraseClick() {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonGenerateSeedPhrase)
        viewModelScope.launchSingle {
            updateUi { uiBuilder.generateMnemonicComponents(uiState) }
            delay(DELAY_GENERATE_SEED_PHRASE)
            interactor.generateMnemonic()
                .onSuccess { mnemonic ->
                    generatedMnemonicComponents = mnemonic.mnemonicComponents
                    val mnemonicGridItems = generateMnemonicGridList(mnemonic.mnemonicComponents)
                    updateUi {
                        router.openScreen(SeedPhraseScreen.YourSeedPhrase)
                        uiBuilder.mnemonicGenerated(uiState, mnemonicGridItems.toImmutableList())
                    }
                }
                .onFailure {
                    generatedMnemonicComponents = null
// [REDACTED_TODO_COMMENT]
                }
        }
    }

    private suspend fun generateMnemonicGridList(mnemonicComponents: List<String>): ImmutableList<MnemonicGridItem> {
        val size = mnemonicComponents.size
        val splitIndex = if (size.isEven()) size / 2 else size / 2 + 1
        val leftColumn = mnemonicComponents.subList(0, splitIndex)
        val rightColumn = mnemonicComponents.subList(splitIndex, size)

        val mnemonicGridItems = mutableListOf<MnemonicGridItem>()
        for (index in 0 until splitIndex) {
            if (index <= leftColumn.size) {
                val item = MnemonicGridItem(
                    index = index + 1,
                    mnemonic = leftColumn[index],
                )
                mnemonicGridItems.add(item)
            }
            if (index < rightColumn.size) {
                val item = MnemonicGridItem(
                    index = index + splitIndex + 1,
                    mnemonic = rightColumn[index],
                )
                mnemonicGridItems.add(item)
            }
        }
        return mnemonicGridItems.toImmutableList()
    }

    private fun buttonImportSeedPhraseClick() {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonImportWallet)
        router.openScreen(SeedPhraseScreen.ImportSeedPhrase)
    }

    private fun buttonContinueClick() {
        analyticsEventHandler.send(SeedPhraseEvents.CheckingScreenOpened)
        router.openScreen(SeedPhraseScreen.CheckSeedPhrase)
    }

    private fun buttonSuggestedPhraseClick(suggestionIndex: Int) {
        viewModelScope.launchSingle {
            val textFieldValue = uiState.importSeedPhraseState.tvSeedPhrase.textFieldValue
            val word = uiState.importSeedPhraseState.suggestionsList[suggestionIndex]
            val cursorPosition = textFieldValue.selection.end

            val insertResult = interactor.insertSuggestionWord(
                text = textFieldValue.text,
                suggestion = word,
                cursorPosition = cursorPosition,
            )
            updateUi {
                val mediateState = uiBuilder.importSeedPhrase.insertSuggestionWord(uiState, insertResult)
                uiBuilder.importSeedPhrase.updateSuggestions(mediateState, persistentListOf())
            }
        }
    }
    // endregion ButtonClickHandlers

    // region Utils
    /**
     * Updating the UI with a contract where all copying of objects are called in the Single thread context and
     * updating the UiState in the main context.
     */
    private suspend fun updateUi(updateBlock: suspend () -> OnboardingSeedPhraseState) {
        withSingleContext {
            val newState = updateBlock.invoke()
            withMainContext { uiState = newState }
        }
    }

    private fun createOrGetDebouncer(name: String): Debouncer {
        return textFieldsDebouncers[name] ?: Debouncer().apply { textFieldsDebouncers[name] = this }
    }

    private fun SeedPhraseField.getState(uiState: OnboardingSeedPhraseState): TextFieldState = when (this) {
        SeedPhraseField.Second -> uiState.checkSeedPhraseState.tvSecondPhrase
        SeedPhraseField.Seventh -> uiState.checkSeedPhraseState.tvSeventhPhrase
        SeedPhraseField.Eleventh -> uiState.checkSeedPhraseState.tvEleventhPhrase
    }

    private fun CoroutineScope.launchSingle(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(dispatchers.single, block = block)
    }

    private suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.main, block)
    }

    private suspend fun <T> withSingleContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.single, block)
    }
    // endregion Utils

    companion object {
        private val URI_ABOUT_SEED_PHRASE =
            Uri.parse("https://tangem.com/ru/blog/post/seed-phrase-a-risky-solution/")
        private const val MNEMONIC_DEBOUNCER = "MnemonicDebouncer"
        private const val MNEMONIC_DEBOUNCE_DELAY = 700L
        private const val DELAY_GENERATE_SEED_PHRASE = 300L
    }
}
