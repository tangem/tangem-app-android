package com.tangem.data.wallets.signing

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.run
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import javax.inject.Inject

internal interface InSessionKeyDeriver {

    suspend fun derive(
        session: CardSession,
        seedKey: ByteArray,
        path: DerivationPath,
    ): CompletionResult<ExtendedPublicKey>
}

internal class DefaultInSessionKeyDeriver @Inject constructor() : InSessionKeyDeriver {

    override suspend fun derive(
        session: CardSession,
        seedKey: ByteArray,
        path: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> =
        DeriveWalletPublicKeyTask(walletPublicKey = seedKey, derivationPath = path).run(session)
}