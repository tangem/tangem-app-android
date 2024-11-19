package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ComponentScoped
class MultiWalletSeedPhraseModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val state = MutableStateFlow(SeedPhraseState())

    private val _uiState = MutableStateFlow<MultiWalletSeedPhraseUM>(
        MultiWalletSeedPhraseUM.Start(
            onImportSeedPhraseClicked = {},
            onGenerateSeedPhraseClicked = {
                openGeneratedSeedPhrase()
            },
            onLearnMoreClicked = {},
        ),
    )

    val uiState = _uiState.asStateFlow()

    fun onBack() {
        TODO("Not yet implemented")
    }

    private fun openGeneratedSeedPhrase() {
        // TODO state.value.generatedWords
        val words = listOf("word, word")

        _uiState.value = MultiWalletSeedPhraseUM.GenerateSeedPhrase(
            words24OptionSelected = false,
            words = words.mapIndexed { index, s ->
                MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem(index, s)
            }.toImmutableList(),
            onWords24OptionSwitch = {
                _uiState.update {
                    if (it !is MultiWalletSeedPhraseUM.GenerateSeedPhrase) return@update it
                    it.copy(words24OptionSelected = it.words24OptionSelected.not())
                }
                state.update { it.copy(words24Option = it.words24Option.not()) }
            },
            onContinueClick = {
            },
        )
    }
}