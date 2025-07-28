package com.tangem.features.hotwallet.common.repository

import android.content.Context
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.MnemonicRepository.MnemonicType
import com.tangem.sdk.extensions.getWordlist
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DefaultMnemonicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : MnemonicRepository {
    private val wordlist = Wordlist.getWordlist(context)

    override val words: Set<String> = wordlist.words.toHashSet()

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