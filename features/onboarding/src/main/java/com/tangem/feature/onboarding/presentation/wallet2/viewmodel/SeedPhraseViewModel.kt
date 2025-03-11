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
import com.tangem.data.common.locale.LocaleProvider
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.domain.SeedPhraseError
import com.tangem.feature.onboarding.domain.SeedPhraseInteractor
import com.tangem.feature.onboarding.domain.models.MnemonicType
import com.tangem.feature.onboarding.presentation.wallet2.analytics.CreateWalletEvents
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseEvents
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource
import com.tangem.feature.onboarding.presentation.wallet2.model.*
import com.tangem.feature.onboarding.presentation.wallet2.model.SegmentSeedType.Companion.fromMnemonicType
import com.tangem.feature.onboarding.presentation.wallet2.ui.stateBuiders.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
@HiltViewModel
class SeedPhraseViewModel @Inject constructor(
    private val interactor: SeedPhraseInteractor,
    private val localeProvider: LocaleProvider,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    private var uiBuilder = StateBuilder(uiActions = createUiActions())

    private var selectedMnemonicType = MnemonicType.Mnemonic12
    var uiState: OnboardingSeedPhraseState by mutableStateOf(uiBuilder.init(fromMnemonicType(selectedMnemonicType)))
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

    private var generatedMnemonicComponents: Map<MnemonicType, List<String>> = emptyMap()
    private var importedMnemonicComponents: List<String>? = null
    private var importedPassphrase: String? = null

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
                    /* no-op */
                }

                SeedPhraseScreen.AboutSeedPhrase -> {
                    analyticsEventHandler.send(SeedPhraseEvents.IntroScreenOpened)
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

    fun setCardArtworkUri(cardArtworkUri: String) {
        if (uiState.introState.cardImageUrl != cardArtworkUri) {
            uiState = uiBuilder.setCardArtwork(uiState, cardArtworkUri)
        }
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
            onSelectType = ::onSelectType,
        ),
        checkSeedPhraseActions = CheckSeedPhraseUiAction(
            buttonCreateWalletClick = {
                val mnemonicComponents =
                    generatedMnemonicComponents[selectedMnemonicType] ?: return@CheckSeedPhraseUiAction
                buttonImportWalletClick(mnemonicComponents, null, SeedPhraseSource.GENERATED)
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
            passTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onPassPhraseTextFieldChanged(value) },
            ),
            suggestedPhraseClick = ::buttonSuggestedPhraseClick,
            onPassphraseInfoClick = ::buttonPassphraseInfoClick,
            buttonCreateWalletClick = {
                analyticsEventHandler.send(SeedPhraseEvents.ButtonImport)
                buttonImportWalletClick(importedMnemonicComponents, importedPassphrase, SeedPhraseSource.IMPORTED)
            },
        ),
        menuChatClick = ::menuChatClick,
        menuNavigateBackClick = ::menuNavigateBackClick,
    )

    // region CheckSeedPhrase
    private fun onTextFieldChanged(field: SeedPhraseField, textFieldValue: TextFieldValue) {
        launchSingle {
            updateUi { uiBuilder.checkSeedPhrase.updateTextField(uiState, field, textFieldValue) }

            val fieldState = field.getState(uiState)
            if (fieldState.textFieldValue.text.isEmpty()) {
                updateUi {
                    val mediate = uiBuilder.checkSeedPhrase.updateTextFieldError(uiState, field, hasError = false)
                    uiBuilder.checkSeedPhrase.updateCreateWalletButton(mediate, enabled = false)
                }
                return@launchSingle
            }

            createOrGetDebouncer(field.name).debounce(viewModelScope, context = dispatchers.io) {
                val mnemonicComponents = generatedMnemonicComponents[selectedMnemonicType] ?: return@debounce
                val hasError = !interactor.isWordMatch(mnemonicComponents, field, textFieldValue.text)
                if (fieldState.isError != hasError) {
                    updateUi { uiBuilder.checkSeedPhrase.updateTextFieldError(uiState, field, hasError) }
                }
                val isCreateWalletButtonEnabled = SeedPhraseField.entries
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
        val oldTextFieldValue = uiState.importSeedPhraseState.fieldSeedPhrase.textFieldValue
        val isSameText = textFieldValue.text == oldTextFieldValue.text
        val isCursorMoved = textFieldValue.selection != oldTextFieldValue.selection

        uiState = uiBuilder.importSeedPhrase.updateSeedPhraseTextField(uiState, textFieldValue)

        val fieldState = uiState.importSeedPhraseState.fieldSeedPhrase
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

    private fun onPassPhraseTextFieldChanged(textFieldValue: TextFieldValue) {
        importedPassphrase = textFieldValue.text
        uiState = uiBuilder.importSeedPhrase.updatePassPhraseTextField(uiState, textFieldValue)
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
            analyticsEventHandler.send(CreateWalletEvents.OnboardingSeedButtonCreateWallet)
            mediator.createWallet(::handleWalletCreationResult)
        }
    }

    private fun buttonImportWalletClick(
        mnemonicComponents: List<String>?,
        passphrase: String?,
        seedPhraseSource: SeedPhraseSource,
    ) {
        mnemonicComponents ?: return

        viewModelScope.launch(dispatchers.io) {
            mediator.importWallet(mnemonicComponents, passphrase, seedPhraseSource, ::handleWalletCreationResult)
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
        analyticsEventHandler.send(CreateWalletEvents.OnboardingSeedButtonOtherOptions)
        router.openScreen(SeedPhraseScreen.AboutSeedPhrase)
    }

    private fun buttonReadMoreAboutSeedPhraseClick() {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonReadMore)
        val webUri = Uri.Builder()
            .scheme("https")
            .authority("tangem.com")
            .appendPath(localeProvider.getWebUriLocaleLanguage())
            .appendPath("blog/post/seed-phrase-a-risky-solution")
            .build()

        router.openUri(webUri)
    }

    private fun buttonGenerateSeedPhraseClick() {
        val mnemonicTypes = listOf(MnemonicType.Mnemonic12, MnemonicType.Mnemonic24)
        analyticsEventHandler.send(SeedPhraseEvents.ButtonGenerateSeedPhrase)
        launchSingle {
            updateUi { uiBuilder.generateMnemonicComponents(uiState) }
            delay(DELAY_GENERATE_SEED_PHRASE)
            interactor.generateMnemonics(mnemonicTypes)
                .onSuccess { mnemonics ->
                    generatedMnemonicComponents = mnemonics.mapValues { it.value.mnemonicComponents }
                    val defaultMnemonic = mnemonics[selectedMnemonicType]?.mnemonicComponents ?: return@launchSingle
                    val mnemonicGridItems = toMnemonicGridList(defaultMnemonic)
                    updateUi {
                        router.openScreen(SeedPhraseScreen.YourSeedPhrase)
                        uiBuilder.mnemonicGenerated(uiState, mnemonicGridItems.toPersistentList())
                    }
                }
                .onFailure {
                    generatedMnemonicComponents = emptyMap()
                    // TODO: show error
                }
        }
    }

    private fun toMnemonicGridList(mnemonicComponents: List<String>): PersistentList<MnemonicGridItem> {
        return mnemonicComponents.mapIndexed { index, word ->
            MnemonicGridItem(
                index = index.inc(),
                mnemonic = word,
            )
        }.toPersistentList()
    }

    private fun buttonImportSeedPhraseClick() {
        analyticsEventHandler.send(SeedPhraseEvents.ButtonImportWallet)
        router.openScreen(SeedPhraseScreen.ImportSeedPhrase)
    }

    private fun buttonContinueClick() {
        launchSingle {
            updateUi {
                uiBuilder.clearCheckSeedPhraseState(uiState)
            }
            router.openScreen(SeedPhraseScreen.CheckSeedPhrase)
        }
    }

    private fun onSelectType(selectedSeedType: SegmentSeedType) {
        launchSingle {
            selectedMnemonicType = selectedSeedType.toMnemonicType()
            val selectedMnemonic = generatedMnemonicComponents[selectedMnemonicType] ?: return@launchSingle
            updateUi {
                uiBuilder.selectSeedType(
                    uiState = uiState,
                    mnemonicGridItems = toMnemonicGridList(selectedMnemonic),
                    selectedSeedType = selectedSeedType,
                )
            }
        }
    }

    private fun buttonSuggestedPhraseClick(suggestionIndex: Int) {
        launchSingle {
            val textFieldValue = uiState.importSeedPhraseState.fieldSeedPhrase.textFieldValue
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
                // copy of TextFieldValue has no effect for textChange callback and seed validation doesn't work
                // call validation force
            }
            validateMnemonic(uiState.importSeedPhraseState.fieldSeedPhrase.textFieldValue.text)
        }
    }

    private fun buttonPassphraseInfoClick() {
        launchSingle {
            updateUi {
                uiBuilder.importSeedPhrase.showPassphraseInfoBottomSheet(uiState) {
                    dismissPassphraseBottomSheet()
                }
            }
        }
    }

    private fun dismissPassphraseBottomSheet() {
        launchSingle {
            updateUi { uiBuilder.importSeedPhrase.dismissPassphraseBottomSheet(uiState) }
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

    private fun launchSingle(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(dispatchers.single, block = block)
    }

    private suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.main, block)
    }

    private suspend fun <T> withSingleContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.single, block)
    }
    // endregion Utils

    private companion object {
        const val MNEMONIC_DEBOUNCER = "MnemonicDebouncer"
        const val MNEMONIC_DEBOUNCE_DELAY = 700L
        const val DELAY_GENERATE_SEED_PHRASE = 300L
    }
}