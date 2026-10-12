package com.tangem.tap.domain.tasks.visa

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.MoneyAccountActivationData
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Derives the Money Account key ([VisaUtilities.moneyAccountDerivationPath]) on the card and
 * generates its deposit address. The derived key is returned (keyed by the seed wallet public key)
 * so the caller can persist it via `DerivationsRepository.storeDerivedKeys` — no second tap needed.
 */
class TangemPayGenerateMoneyAccountAddressTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
) : CardSessionRunnable<MoneyAccountActivationData> {

    override fun run(session: CardSession, callback: CompletionCallback<MoneyAccountActivationData>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<MoneyAccountActivationData> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.curve }
            ?: return CompletionResult.Failure(VisaActivationError.MissingWallet.tangemError)
        val publicKey =
            wallet.publicKey ?: return CompletionResult.Failure(VisaActivationError.PublicKeyIsEmpty.tangemError)

        val depositKey = when (
            val result = runDerivationTask(
                session = session,
                walletPublicKey = publicKey,
                derivationPath = VisaUtilities.moneyAccountDerivationPath,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<ExtendedPublicKey> -> result.data
        }

        val address = VisaUtilities.generateAddressFromExtendedKey(extendedPublicKey = depositKey)

        val customerWalletKey = when (
            val result = runDerivationTask(
                session = session,
                walletPublicKey = publicKey,
                derivationPath = VisaUtilities.customDerivationPath,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<ExtendedPublicKey> -> result.data
        }

        val signature = when (
            val result = signDepositAddress(
                session = session,
                walletPublicKey = publicKey,
                address = address,
                customerWalletKey = customerWalletKey,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<String> -> result.data
        }

        val derivedKeys = mapOf(
            publicKey.toMapKey() to ExtendedPublicKeysMap(
                mapOf(VisaUtilities.moneyAccountDerivationPath to depositKey),
            ),
        )

        return CompletionResult.Success(
            data = MoneyAccountActivationData(
                address = address,
                signature = signature,
                derivedKeys = derivedKeys,
            ),
        )
    }

    private suspend fun runDerivationTask(
        session: CardSession,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        val derivationTask = DeriveWalletPublicKeyTask(
            walletPublicKey = walletPublicKey,
            derivationPath = derivationPath,
        )

        derivationTask.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private suspend fun signDepositAddress(
        session: CardSession,
        walletPublicKey: ByteArray,
        address: String,
        customerWalletKey: ExtendedPublicKey,
    ): CompletionResult<String> {
        val content = VisaUtilities.moneyAccountDepositAddressMessage(depositAddress = address)
        val hash = VisaUtilities.hashPersonalMessage(content.toByteArray(Charsets.UTF_8))

        val deferred = CompletableDeferred<CompletionResult<SignHashResponse>>()
        val signCommand = SignHashCommand(
            hash = hash,
            walletPublicKey = walletPublicKey,
            derivationPath = VisaUtilities.customDerivationPath,
        )

        signCommand.run(session = session, callback = deferred::complete)

        return when (val result = deferred.await()) {
            is CompletionResult.Failure<*> -> CompletionResult.Failure(result.error)
            is CompletionResult.Success<SignHashResponse> -> CompletionResult.Success(
                data = VisaUtilities.unmarshallSignature(
                    signature = result.data.signature,
                    hash = hash,
                    extendedPublicKey = customerWalletKey,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): TangemPayGenerateMoneyAccountAddressTask
    }
}