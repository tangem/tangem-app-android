package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder

import com.tangem.crypto.bip39.Mnemonic
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.SeedPhraseState
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class GenerateSeedPhraseUiStateBuilder(
    private val state: MutableStateFlow<SeedPhraseState>,
    private val updateUiState: (
        (MultiWalletSeedPhraseUM.GenerateSeedPhrase) -> MultiWalletSeedPhraseUM.GenerateSeedPhrase,
    ) -> Unit,
    private val onContinue: () -> Unit,
) {

    fun getState(generatedWords12: Mnemonic, generatedWords24: Mnemonic): MultiWalletSeedPhraseUM.GenerateSeedPhrase {
        val words12 = generatedWords12.mnemonicComponents.mapIndexed { index, s ->
            MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem(index + 1, s)
        }.toImmutableList()
        val words24 = generatedWords24.mnemonicComponents.mapIndexed { index, s ->
            MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem(index + 1, s)
        }.toImmutableList()

        return MultiWalletSeedPhraseUM.GenerateSeedPhrase(
            words24OptionSelected = state.value.words24Option,
            words = words12,
            onWords24OptionSwitch = { switchType(words12, words24) },
            onContinueClick = onContinue,
        )
    }

    private fun switchType(
        generatedWords12: ImmutableList<MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem>,
        generatedWords24: ImmutableList<MultiWalletSeedPhraseUM.GenerateSeedPhrase.MnemonicGridItem>,
    ) {
        updateUiState { uiSt ->
            state.update { it.copy(words24Option = uiSt.words24OptionSelected.not()) }

            uiSt.copy(
                words24OptionSelected = uiSt.words24OptionSelected.not(),
                words = if (uiSt.words24OptionSelected) {
                    generatedWords12
                } else {
                    generatedWords24
                },
            )
        }
    }
}