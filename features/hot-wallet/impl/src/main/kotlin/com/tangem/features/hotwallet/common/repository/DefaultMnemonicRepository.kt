package com.tangem.features.hotwallet.common.repository

import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.MnemonicRepository.MnemonicType
import com.tangem.sdk.extensions.getWordlist
import javax.inject.Inject

internal class DefaultMnemonicRepository @Inject constructor() : MnemonicRepository {

    private val wordlist by lazy(LazyThreadSafetyMode.NONE) { Wordlist.getWordlist() }

    override val words: Set<String> by lazy(LazyThreadSafetyMode.NONE) { wordlist.words.toHashSet() }

    override fun generateMnemonic(type: MnemonicType): Mnemonic = DefaultMnemonic(
        entropy = when (type) {
            MnemonicType.Words12 -> EntropyLength.Bits128Length
            MnemonicType.Words24 -> EntropyLength.Bits256Length
        },
        wordlist = wordlist,
    )

    override fun generateMnemonic(mnemonicString: String): Mnemonic = DefaultMnemonic(
        mnemonic = mnemonicString,
        wordlist = wordlist,
    )
}