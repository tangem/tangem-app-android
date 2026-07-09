package com.tangem.tap.domain.tasks.visa

import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.VirtualAccountActivationData
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Derives the Virtual Account key ([VisaUtilities.virtualAccountDerivationPath]) on the card and
 * generates its deposit address. The derived key is returned (keyed by the seed wallet public key)
 * so the caller can persist it via `DerivationsRepository.storeDerivedKeys` — no second tap needed.
 */
class TangemPayGenerateVirtualAccountAddressTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
) : CardSessionRunnable<VirtualAccountActivationData> {

    override fun run(session: CardSession, callback: CompletionCallback<VirtualAccountActivationData>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<VirtualAccountActivationData> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.curve }
            ?: return CompletionResult.Failure(VisaActivationError.MissingWallet.tangemError)

        val extendedPublicKey = when (val derivationResult = runDerivationTask(session, wallet)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(derivationResult.error)
            is CompletionResult.Success<ExtendedPublicKey> -> derivationResult.data
        }

        val address = VisaUtilities.generateAddressFromExtendedKey(extendedPublicKey = extendedPublicKey)

        val derivedKeys = mapOf(
            wallet.publicKey.toMapKey() to ExtendedPublicKeysMap(
                mapOf(VisaUtilities.virtualAccountDerivationPath to extendedPublicKey),
            ),
        )

        return CompletionResult.Success(
            data = VirtualAccountActivationData(address = address, derivedKeys = derivedKeys),
        )
    }

    private suspend fun runDerivationTask(
        session: CardSession,
        wallet: CardWallet,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        val derivationTask = DeriveWalletPublicKeyTask(
            walletPublicKey = wallet.publicKey,
            derivationPath = VisaUtilities.virtualAccountDerivationPath,
        )

        derivationTask.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): TangemPayGenerateVirtualAccountAddressTask
    }
}