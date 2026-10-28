package com.tangem.domain.wallets.signing

import arrow.core.Either
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWallet

interface DeriveAndSignSession {

    suspend fun <T> run(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        prepare: suspend (derivedKey: ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>>
}

class SignRequest<T>(val value: T, val hashes: List<ByteArray>)

class Signed<T>(val value: T, val signatures: List<ByteArray>)