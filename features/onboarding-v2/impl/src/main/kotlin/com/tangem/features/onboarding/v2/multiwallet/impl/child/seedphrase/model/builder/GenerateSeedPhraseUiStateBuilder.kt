package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.builder

import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.GeneratedWordsType
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.ui.state.MultiWalletSeedPhraseUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class GenerateSeedPhraseUiStateBuilder(
    private val changeGeneratedWordsType: (GeneratedWordsType) -> Unit,
    private val updateUiState: (
        (MultiWalletSeedPhraseUM.GenerateSeedPhrase) -> MultiWalletSeedPhraseUM.GenerateSeedPhrase,
    ) -> Unit,
    private val onContinue: () -> Unit,
) {

    fun getState(
        generatedWords12: Mnemonic,
        generatedWords24: Mnemonic,
        option: GeneratedWordsType,
    ): MultiWalletSeedPhraseUM.GenerateSeedPhrase {
        val words12 = generatedWords12.mnemonicComponents.mapIndexed { index, s ->
            EnumeratedTwoColumnGridItem(index + 1, s)
        }.toImmutableList()
        val words24 = generatedWords24.mnemonicComponents.mapIndexed { index, s ->
            EnumeratedTwoColumnGridItem(index + 1, s)
        }.toImmutableList()

        return MultiWalletSeedPhraseUM.GenerateSeedPhrase(
            option = option,
            words = when (option) {
                GeneratedWordsType.Words12 -> words12
                GeneratedWordsType.Words24 -> words24
            },
            onOptionChange = {
                switchType(
                    newType = it,
                    generatedWords12 = words12,
                    generatedWords24 = words24,
                )
            },
            onContinueClick = onContinue,
        )
    }

    private fun switchType(
        newType: GeneratedWordsType,
        generatedWords12: ImmutableList<EnumeratedTwoColumnGridItem>,
        generatedWords24: ImmutableList<EnumeratedTwoColumnGridItem>,
    ) {
        updateUiState { uiSt ->
            changeGeneratedWordsType(newType)

            uiSt.copy(
                option = newType,
                words = when (newType) {
                    GeneratedWordsType.Words12 -> generatedWords12
                    GeneratedWordsType.Words24 -> generatedWords24
                },
            )
        }
    }
}