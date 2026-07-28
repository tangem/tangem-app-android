package com.tangem.tap.domain.tasks.polymarket

import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.polymarket.PolymarketOwnerKeyData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Cold-card session that derives the secp256k1 Polymarket owner key on [POLYMARKET_OWNER_DERIVATION_PATH] and
 * returns the derived key for the caller to persist and to compute the address from.
 *
 * It is a dedicated [CardSessionRunnable] rather than the generic derivation task on purpose: it will be extended
 * to also sign the Polymarket typed data inside the same session, so that derivation and signing cost the user a
 * single tap — the same fusion Tangem Pay does in `TangemPayGenerateAddressAndSignChallengeTask`.
 */
class PolymarketDeriveOwnerKeyTask : CardSessionRunnable<PolymarketOwnerKeyData> {

    override fun run(session: CardSession, callback: CompletionCallback<PolymarketOwnerKeyData>) {
        session.scope.launch { callback(runSuspend(session = session)) }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<PolymarketOwnerKeyData> {
        val card = session.environment.card
            ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?: return CompletionResult.Failure(TangemSdkError.WalletNotFound())

        val path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
        val extendedPublicKey = when (val result = runDerivationTask(session, wallet, path)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<ExtendedPublicKey> -> result.data
        }

        val derivedKeys = mapOf(
            wallet.publicKey.toMapKey() to ExtendedPublicKeysMap(mapOf(path to extendedPublicKey)),
        )
        return CompletionResult.Success(
            PolymarketOwnerKeyData(derivedKeys = derivedKeys),
        )
    }

    private suspend fun runDerivationTask(
        session: CardSession,
        wallet: CardWallet,
        path: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        DeriveWalletPublicKeyTask(walletPublicKey = wallet.publicKey, derivationPath = path)
            .run(session = session, callback = deferred::complete)
        return deferred.await()
    }
}