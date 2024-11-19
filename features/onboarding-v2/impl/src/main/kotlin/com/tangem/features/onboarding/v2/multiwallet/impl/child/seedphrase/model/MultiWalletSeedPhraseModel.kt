package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.GenerateSeedPhraseUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder.SeedPhraseCheckUiStateBuilder
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ComponentScoped
class MultiWalletSeedPhraseModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val mnemonicRepository: MnemonicRepository,
    private val urlOpener: UrlOpener,
) : Model() {

    private val state = MutableStateFlow(SeedPhraseState())
    private val _uiState = MutableStateFlow(getStartState())

    private val generateSeedPhraseUiStateBuilder = GenerateSeedPhraseUiStateBuilder(
        state = state,
        updateUiState = { block -> updateUiStateSpecific(block) },
        onContinue = { openWordsCheck() },
    )

    private val seedPhraseCheckUiStateBuilder = SeedPhraseCheckUiStateBuilder(
        state = state,
        currentUiState = { _uiState.value },
        updateUiState = { block -> updateUiStateSpecific(block) },
        importWallet = { },
    )

    val uiState = _uiState.asStateFlow()

    fun onBack() {
        when (_uiState.value) {
            is MultiWalletSeedPhraseUM.Start -> {
                // TODO navigate back
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
            onImportSeedPhraseClicked = {},
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

        // TODO disallow screenshots

        _uiState.value = generateSeedPhraseUiStateBuilder.getState(
            generatedWords12 = words12,
            generatedWords24 = words24,
        )
    }

    private fun openWordsCheck() {
        _uiState.value = seedPhraseCheckUiStateBuilder.getState()
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

    private inline fun <reified T : MultiWalletSeedPhraseUM> updateUiStateSpecific(block: (T) -> T) {
        _uiState.update {
            if (it !is T) return@update it
            block(it)
        }
    }
}