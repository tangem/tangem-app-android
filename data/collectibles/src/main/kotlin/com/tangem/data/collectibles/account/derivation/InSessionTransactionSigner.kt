package com.tangem.data.collectibles.account.derivation

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.run
import com.tangem.common.map
import com.tangem.operations.sign.MultipleSignCommand
import com.tangem.operations.sign.SignData
import com.tangem.operations.sign.SignHashesCommand

/**
 * Signs on an already-open session, unlike `TangemSigner`, which starts one of its own: onboarding derives the
 * owner key and signs `init_vault` under the same tap.
 *
 * Lives and dies with [session] — build it at the call site inside a `CardSessionRunnable`, never inject or cache it.
 */
internal class InSessionTransactionSigner(private val session: CardSession) : TransactionSigner {

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey) = sign(
        hashes = listOf(hash),
        publicKey = publicKey,
    ).map(List<ByteArray>::first)

    override suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey) = SignHashesCommand(
        hashes = hashes.toTypedArray(),
        walletPublicKey = publicKey.seedKey,
        derivationPath = publicKey.derivationPath,
    )
        .run(session = session)
        .map { response -> response.signatures }

    override suspend fun multiSign(dataToSign: List<SignData>, publicKey: Wallet.PublicKey) =
        MultipleSignCommand(dataToSign = dataToSign, walletPublicKey = publicKey.seedKey)
            .run(session = session)
            .map { responses -> responses.associate { it.walletPublicKey to it.signature } }
}