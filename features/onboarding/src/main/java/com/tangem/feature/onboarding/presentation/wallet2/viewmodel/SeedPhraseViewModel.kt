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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
@HiltViewModel
class SeedPhraseViewModel @Inject constructor(
    private val seedPhraseInteractor: SeedPhraseInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    var aboutSeedPhraseUriProvider: AboutSeedPhraseOpener? = null
    var chatLauncher: ChatSupportOpener? = null

    private var uiBuilder = StateBuilder(createUiActions())

    var uiState: OnboardingSeedPhraseState by mutableStateOf(uiBuilder.init())
        private set

    val currentStep: OnboardingSeedPhraseStep
        get() = uiState.step

    private val textFieldsDebouncers = mutableMapOf<SeedPhraseField, Debouncer>()

    init {
        prepareCheckYourSeedPhraseTest()
    }

    // TODO: delete
    private fun prepareCheckYourSeedPhraseTest() {
        uiState.copy(step = OnboardingSeedPhraseStep.AboutSeedPhrase)
        viewModelScope.launchIo {
            delay(300)
            buttonGenerateSeedPhraseClick()
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
        ),
        checkSeedPhraseActions = CheckSeedPhraseUiAction(
            buttonCreateWalletClick = ::buttonCreateWalletWithSeedPhraseClick,
            secondTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Second, value) },
                onFocusChanged = { isFocused -> onFocusChanged(SeedPhraseField.Second, isFocused) },
            ),
            seventhTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Seventh, value) },
                onFocusChanged = { isFocused -> onFocusChanged(SeedPhraseField.Seventh, isFocused) },
            ),
            eleventhTextFieldAction = TextFieldUiAction(
                onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Eleventh, value) },
                onFocusChanged = { isFocused -> onFocusChanged(SeedPhraseField.Eleventh, isFocused) },
            ),
        ),
        importSeedPhraseActions = ImportSeedPhraseUiAction(
            phraseTextFieldAction = TextFieldUiAction(
                // onFocusChanged = { isFocused -> onFocusChanged(SeedPhraseField.Eleventh, isFocused) },
                // onTextFieldChanged = { value -> onTextFieldChanged(SeedPhraseField.Eleventh, value) },
            ),
            suggestedPhraseClick = ::buttonSuggestedPhraseClick,
            buttonCreateWalletClick = ::buttonCreateWalletWithSeedPhraseClick,
        ),
        menuChatClick = ::menuChatClick,
    )

    override fun onCleared() {
        textFieldsDebouncers.forEach { entry -> entry.value.release() }
        textFieldsDebouncers.clear()
        super.onCleared()
    }

    // region CheckSeedPhrase
    private fun onFocusChanged(field: SeedPhraseField, isFocused: Boolean) {
        when (field) {
            SeedPhraseField.Second -> {}
            SeedPhraseField.Seventh -> {}
            SeedPhraseField.Eleventh -> {}
        }
    }

    private fun onTextFieldChanged(field: SeedPhraseField, textFieldValue: TextFieldValue) {
        uiState = uiBuilder.checkSeedPhrase.updateTextField(uiState, field, textFieldValue)

        val fieldState = field.getState(uiState)
        if (fieldState.textFieldValue.text.isEmpty()) {
            uiState = uiBuilder.checkSeedPhrase.updateTextFieldError(uiState, field, hasError = false)
            return
        }

        viewModelScope.launchIo {
            val textFieldDebouncer = textFieldsDebouncers[field] ?: Debouncer().apply {
                textFieldsDebouncers[field] = this
            }
            textFieldDebouncer.debounce(viewModelScope, context = dispatchers.io) {
                val hasError = !seedPhraseInteractor.isPhraseMatch(
                    phrase = textFieldValue.text,
                    source = uiState.yourSeedPhraseState.phraseList,
                )
                if (fieldState.isError != hasError) {
                    withMainContext {
                        uiState = uiBuilder.checkSeedPhrase.updateTextFieldError(
                            uiState = uiState,
                            field = field,
                            hasError = hasError,
                        )
                    }
                }

                val allFieldsWithoutError = SeedPhraseField.values()
                    .map { it.getState(uiState) }
                    .all { !it.isError }

                if (uiState.checkSeedPhraseState.buttonCreateWallet.enabled != allFieldsWithoutError) {
                    withMainContext {
                        uiState = uiBuilder.checkSeedPhrase.updateCreateWalletButton(
                            uiState = uiState,
                            enabled = allFieldsWithoutError,
                        )
                    }
                }
            }
        }
    }
    // endregion CheckSeedPhrase

    // region ButtonClickHandlers
    private fun buttonCreateWalletClick() {
    }

    private fun buttonCreateWalletWithSeedPhraseClick() {
        buttonGenerateSeedPhraseClick()
    }

    private fun buttonOtherOptionsClick() {
        uiState = uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.AboutSeedPhrase)
    }

    private fun buttonReadMoreAboutSeedPhraseClick() {
        aboutSeedPhraseUriProvider?.open()
    }

    private fun buttonGenerateSeedPhraseClick() {
        viewModelScope.launchIo {
            withMainContext {
                uiState = uiBuilder.generateSeedPhrase(uiState)
            }

            delay(1000)
            val seedPhraseList = seedPhraseInteractor.generateSeedPhrase()
            withMainContext {
                uiState = uiBuilder.seedPhraseGenerated(uiState, seedPhraseList)
            }
        }
    }

    private fun buttonImportSeedPhraseClick() {
        uiState = uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.ImportSeedPhrase)
    }

    private fun buttonContinueClick() {
        uiState = uiBuilder.changeStep(uiState, OnboardingSeedPhraseStep.CheckSeedPhrase)
    }

    private fun buttonSuggestedPhraseClick(suggestionIndex: Int) {
    }

    private fun menuChatClick() {
        chatLauncher?.open()
    }
    // endregion ButtonClickHandlers

    // region Utils
    private fun SeedPhraseField.getState(uiState: OnboardingSeedPhraseState): TextFieldState = when (this) {
        SeedPhraseField.Second -> uiState.checkSeedPhraseState.tvSecondPhrase
        SeedPhraseField.Seventh -> uiState.checkSeedPhraseState.tvSeventhPhrase
        SeedPhraseField.Eleventh -> uiState.checkSeedPhraseState.tvEleventhPhrase
    }

    private fun CoroutineScope.launchMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(dispatchers.main, block = block)
    }

    private fun CoroutineScope.launchIo(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(dispatchers.main, block = block)
    }

    private suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.main, block)
    }

    private suspend fun <T> withIoContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(dispatchers.io, block)
    }
    // endregion Utils
}

interface AboutSeedPhraseOpener {
    fun open() {}
}

interface ChatSupportOpener {
    fun open() {}
}