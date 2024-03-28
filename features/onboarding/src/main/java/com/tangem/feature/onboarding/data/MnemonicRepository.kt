package com.tangem.feature.onboarding.data

import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.feature.onboarding.domain.models.MnemonicType

/**
[REDACTED_AUTHOR]
 */
interface MnemonicRepository {

    fun getWordsDictionary(): Set<String>

    @Throws(TangemSdkError.MnemonicException::class)
    fun generateDefaultMnemonic(): Mnemonic

    @Throws(TangemSdkError.MnemonicException::class)
    fun generateMnemonic(mnemonicType: MnemonicType): Mnemonic

    @Throws(TangemSdkError.MnemonicException::class)
    fun createMnemonic(mnemonicString: String): Mnemonic
}