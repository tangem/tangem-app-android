package com.tangem.feature.onboarding.presentation.wallet2.viewmodel

import com.tangem.common.CompletionResult
import com.tangem.feature.onboarding.data.model.CreateWalletResponse
import com.tangem.feature.onboarding.presentation.wallet2.analytics.SeedPhraseSource

/**
[REDACTED_AUTHOR]
 */
interface SeedPhraseMediator {
    fun createWallet(callback: (CompletionResult<CreateWalletResponse>) -> Unit)
    fun importWallet(
        mnemonicComponents: List<String>,
        passphrase: String?,
        seedPhraseSource: SeedPhraseSource,
        callback: (CompletionResult<CreateWalletResponse>) -> Unit,
    )

    fun onWalletCreated(result: CompletionResult<CreateWalletResponse>)

    fun allowScreenshots(allow: Boolean)
}