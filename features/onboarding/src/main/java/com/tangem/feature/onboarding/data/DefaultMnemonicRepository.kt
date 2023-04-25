package com.tangem.feature.onboarding.data

import com.tangem.crypto.bip39.*
import com.tangem.datasource.asset.AssetReader

/**
* [REDACTED_AUTHOR]
 */
internal class DefaultMnemonicRepository(
    private val assetReader: AssetReader,
) : MnemonicRepository {
// [REDACTED_TODO_COMMENT]
    private val bip39Wordlist: Wordlist by lazy {
        assetReader.openFile(MNEMONIC_FILE_NAME)
            .use { BIP39Wordlist(it) }
    }

    private val bip39Words: Set<String> by lazy { bip39Wordlist.words.toHashSet() }

    override fun getWordsDictionary(): Set<String> = bip39Words

    override fun generateDefaultMnemonic(): Mnemonic {
        return DefaultMnemonic(EntropyLength.Bits128Length, bip39Wordlist)
    }

    override fun createMnemonic(mnemonicString: String): Mnemonic = DefaultMnemonic(mnemonicString, bip39Wordlist)

    companion object {
        private const val MNEMONIC_FILE_NAME = "mnemonic/mnemonic_dictionary_en.txt"
    }
}
