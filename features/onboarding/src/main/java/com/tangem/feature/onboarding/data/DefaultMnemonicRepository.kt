package com.tangem.feature.onboarding.data

import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.feature.onboarding.domain.models.MnemonicType

/**
[REDACTED_AUTHOR]
 */
class DefaultMnemonicRepository(
    private val bip39Wordlist: Wordlist,
) : MnemonicRepository {

    private val bip39Words: Set<String> by lazy { bip39Wordlist.words.toHashSet() }

    override fun getWordsDictionary(): Set<String> = bip39Words

    override fun generateDefaultMnemonic(): Mnemonic {
        return DefaultMnemonic(EntropyLength.Bits128Length, bip39Wordlist)
    }

    override fun generateMnemonic(mnemonicType: MnemonicType): Mnemonic {
        return when (mnemonicType) {
            MnemonicType.Mnemonic12 -> DefaultMnemonic(EntropyLength.Bits128Length, bip39Wordlist)
            MnemonicType.Mnemonic24 -> DefaultMnemonic(EntropyLength.Bits256Length, bip39Wordlist)
        }
    }

    override fun createMnemonic(mnemonicString: String): Mnemonic = DefaultMnemonic(mnemonicString, bip39Wordlist)
}