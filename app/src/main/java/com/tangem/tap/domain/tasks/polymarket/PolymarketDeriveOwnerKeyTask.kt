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
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.domain.polymarket.derivation.OWNER_DERIVATION_PATH
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.polymarket.PolymarketOwnerKeyData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Cold-card session for APP-7a: derives the secp256k1 owner key on [OWNER_DERIVATION_PATH] and returns
 * the ERC-55 address + derived key (for the caller to persist). Mirrors Pay's
 * TangemPayGenerateVirtualAccountAddressTask; imports nothing from Pay/Visa.
 */
class PolymarketDeriveOwnerKeyTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val addressFactory: PolymarketAddressFactory,
) : CardSessionRunnable<PolymarketOwnerKeyData> {

    override fun run(session: CardSession, callback: CompletionCallback<PolymarketOwnerKeyData>) {
        coroutineScope.launch { callback(runSuspend(session = session)) }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<PolymarketOwnerKeyData> {
        val card = session.environment.card
            ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?: return CompletionResult.Failure(TangemSdkError.WalletNotFound())

        val path = DerivationPath(OWNER_DERIVATION_PATH)
        val extendedPublicKey = when (val result = runDerivationTask(session, wallet, path)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<ExtendedPublicKey> -> result.data
        }

        val address = addressFactory.createAddress(extendedPublicKey)
        val derivedKeys = mapOf(
            wallet.publicKey.toMapKey() to ExtendedPublicKeysMap(mapOf(path to extendedPublicKey)),
        )
        return CompletionResult.Success(
            PolymarketOwnerKeyData(address = address, derivedKeys = derivedKeys),
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

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): PolymarketDeriveOwnerKeyTask
    }
}