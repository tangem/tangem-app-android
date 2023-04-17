package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.onboarding.domain.SeedPhraseInteractor
import com.tangem.feature.onboarding.presentation.wallet2.model.AboutUiAction
import com.tangem.feature.onboarding.presentation.wallet2.model.CheckSeedPhraseUiAction
import com.tangem.feature.onboarding.presentation.wallet2.model.ImportSeedPhraseUiAction
import com.tangem.feature.onboarding.presentation.wallet2.model.IntroUiAction
import com.tangem.feature.onboarding.presentation.wallet2.model.MnemonicGridItem
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseState
import com.tangem.feature.onboarding.presentation.wallet2.model.OnboardingSeedPhraseStep
import com.tangem.feature.onboarding.presentation.wallet2.model.SeedPhraseField
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldState
import com.tangem.feature.onboarding.presentation.wallet2.model.TextFieldUiAction
import com.tangem.feature.onboarding.presentation.wallet2.model.UiActions
import com.tangem.feature.onboarding.presentation.wallet2.model.YourSeedPhraseUiAction
import com.tangem.feature.onboarding.presentation.wallet2.ui.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.extensions.isEven
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
@HiltViewModel
class SeedPhraseViewModel @Inject constructor(
    private val interactor: SeedPhraseInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    private var uiBuilder = StateBuilder(createUiActions())

    var uiState: OnboardingSeedPhraseState by mutableStateOf(uiBuilder.init())
        private set

    val currentStep: OnboardingSeedPhraseStep
        get() = uiState.step

    private val textFieldsDebouncers = mutableMapOf<String, Debouncer>()

    override fun onCleared() {
        textFieldsDebouncers.forEach { entry -> entry.value.release() }
        textFieldsDebouncers.clear()
        super.onCleared()
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
            buttonCreateWalletClick = ::buttonCreateWalletWithSeedPhraseClick,
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
                onFocusChanged = { isFocused ->  /* TODO: handle it */ },
                onTextFieldChanged = { value -> /* TODO: handle it */ },
            ),
            suggestedPhraseClick = ::buttonSuggestedPhraseClick,
            buttonCreateWalletClick = ::buttonCreateWalletWithSeedPhraseClick,
        ),
        menuChatClick = ::menuChatClick,
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

                val allFieldsWithoutError = SeedPhraseField.values()
                    .map { field -> field.getState(uiState) }
                    .all { fieldState -> !fieldState.isError }

                if (uiState.checkSeedPhraseState.buttonCreateWallet.enabled != allFieldsWithoutError) {
                    updateUi { uiBuilder.checkSeedPhrase.updateCreateWalletButton(uiState, allFieldsWithoutError) }
                }
            }
        }
    }
    // endregion CheckSeedPhrase

    // region ButtonClickHandlers
    private fun buttonCreateWalletClick() {
    }

    private fun buttonCreateWalletWithSeedPhraseClick() {
    }

    private fun buttonOtherOptionsClick() {
        viewModelScope.launchSingle {
            updateUi { uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.AboutSeedPhrase) }
        }
    }

    private fun buttonReadMoreAboutSeedPhraseClick() {
// [REDACTED_TODO_COMMENT]
    }

    private fun buttonGenerateSeedPhraseClick() {
        viewModelScope.launchSingle {
            updateUi { uiBuilder.generateMnemonicComponents(uiState) }
            interactor.generateMnemonic()
                .onSuccess { mnemonic ->
                    val mnemonicGridItems = generateMnemonicGridList(mnemonic.mnemonicComponents)
                    updateUi { uiBuilder.mnemonicGenerated(uiState, mnemonicGridItems.toImmutableList()) }
                }
                .onFailure {
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
            if (index <= leftColumn.size) mnemonicGridItems.add(
                MnemonicGridItem(
                    index = index + 1,
                    mnemonic = leftColumn[index],
                ),
            )
            if (index < rightColumn.size) mnemonicGridItems.add(
                MnemonicGridItem(
                    index = index + splitIndex + 1,
                    mnemonic = rightColumn[index],
                ),
            )
        }
        return mnemonicGridItems.toImmutableList()
    }

    private fun buttonImportSeedPhraseClick() {
        viewModelScope.launchSingle {
            updateUi { uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.ImportSeedPhrase) }
        }
    }

    private fun buttonContinueClick() {
        viewModelScope.launchSingle {
            updateUi { uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.CheckSeedPhrase) }
        }
    }

    private fun buttonSuggestedPhraseClick(suggestionIndex: Int) {
    }

    private fun menuChatClick() {
// [REDACTED_TODO_COMMENT]
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
}
