package com.tangem.data.wallets.signing

import arrow.core.Either
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWallet

interface DeriveAndSignSession {

    suspend fun <T> run(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        block: suspend DeriveAndSignScope.(derivedKey: ExtendedPublicKey) -> T,
    ): Either<TangemSdkError, T>
}

fun interface DeriveAndSignScope {

    suspend fun sign(hashes: List<ByteArray>): CompletionResult<List<ByteArray>>
}