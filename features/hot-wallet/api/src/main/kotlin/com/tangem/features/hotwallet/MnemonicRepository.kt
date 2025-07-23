package com.tangem.features.hotwallet

import com.tangem.crypto.bip39.Mnemonic

interface MnemonicRepository {

    val words: Set<String>

    fun generateMnemonic(type: MnemonicType = MnemonicType.Words12): Mnemonic

    fun generateMnemonic(mnemonicString: String): Mnemonic

    enum class MnemonicType {
        Words12, Words24,
    }
}