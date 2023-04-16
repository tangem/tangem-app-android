package com.tangem.feature.onboarding.data

import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.bip39.BIP39Wordlist
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.datasource.asset.AssetReader
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
interface MnemonicRepository {

    fun getWordsDictionary(): Set<String>

    @Throws(TangemSdkError.MnemonicException::class)
    fun generateDefaultMnemonic(): Mnemonic

    @Throws(TangemSdkError.MnemonicException::class)
    fun createMnemonic(mnemonicString: String): Mnemonic
}

class DefaultMnemonicRepository @Inject constructor(
    private val assetReader: AssetReader,
) : MnemonicRepository {

    private val bip39Wordlist: Wordlist by lazy { BIP39Wordlist(assetReader.openFile(MNEMONIC_FILE_NAME)) }
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
