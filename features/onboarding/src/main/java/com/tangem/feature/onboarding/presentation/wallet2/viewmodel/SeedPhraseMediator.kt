package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import com.tangem.common.CompletionResult
import com.tangem.feature.onboarding.data.model.CreateWalletResponse

/**
[REDACTED_AUTHOR]
 */
interface SeedPhraseMediator {
    fun createWallet(callback: (CompletionResult<CreateWalletResponse>) -> Unit)
    fun importWallet(mnemonicComponents: List<String>, callback: (CompletionResult<CreateWalletResponse>) -> Unit)

    fun onWalletCreated(result: CompletionResult<CreateWalletResponse>)
}