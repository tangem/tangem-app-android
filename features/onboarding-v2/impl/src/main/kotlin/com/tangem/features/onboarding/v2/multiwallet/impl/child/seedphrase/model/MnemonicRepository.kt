package com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model

import android.content.Context
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.bip39.Wordlist
import com.tangem.sdk.extensions.getWordlist
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class MnemonicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wordlist = Wordlist.getWordlist(context)

    val words: Set<String> = wordlist.words.toHashSet()

    fun generateMnemonic(type: MnemonicType = MnemonicType.Words12): Mnemonic = DefaultMnemonic(
        entropy = when (type) {
            MnemonicType.Words12 -> EntropyLength.Bits128Length
            MnemonicType.Words24 -> EntropyLength.Bits256Length
        },
        wordlist = wordlist,
    )

    fun generateMnemonic(mnemonicString: String): Mnemonic = DefaultMnemonic(
        mnemonic = mnemonicString,
        wordlist = wordlist,
    )

    enum class MnemonicType {
        Words12, Words24,
    }
}