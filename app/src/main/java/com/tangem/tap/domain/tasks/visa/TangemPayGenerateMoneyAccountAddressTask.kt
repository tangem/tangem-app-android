package com.tangem.tap.domain.tasks.visa

import arrow.core.getOrElse
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
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.MoneyAccountActivationData
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TangemPayGenerateMoneyAccountAddressTask @Inject constructor(
    private val dispatchersProvider: CoroutineDispatcherProvider,
    private val tangemPayRemoteDataSource: TangemPayRemoteDataSource,
) : CardSessionRunnable<MoneyAccountActivationData> {

    override fun run(session: CardSession, callback: CompletionCallback<MoneyAccountActivationData>) {
        session.scope.launch {
            val result = runSuspendCatching { runSuspend(session = session) }
                .getOrElse { error ->
                    CompletionResult.Failure(TangemSdkError.ExceptionError(error))
                }
            callback(result)
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

        val customerWalletAddress = VisaUtilities.generateAddressFromExtendedKey(extendedPublicKey = customerWalletKey)
        val authTokens = when (
            val result = authorizeCustomerWallet(
                session = session,
                walletPublicKey = publicKey,
                customerWalletAddress = customerWalletAddress,
                customerWalletKey = customerWalletKey,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<TangemPayAuthTokens> -> result.data
        }

        val signature = when (
            val result = signPersonalMessage(
                session = session,
                walletPublicKey = publicKey,
                content = VisaUtilities.moneyAccountDepositAddressMessage(depositAddress = address),
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
                initialCredentials = TangemPayInitialCredentials(
                    customerWalletAddress = customerWalletAddress,
                    authTokens = authTokens,
                ),
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

    private suspend fun authorizeCustomerWallet(
        session: CardSession,
        walletPublicKey: ByteArray,
        customerWalletAddress: String,
        customerWalletKey: ExtendedPublicKey,
    ): CompletionResult<TangemPayAuthTokens> {
        val challenge = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getCustomerWalletAuthChallenge(
                customerWalletAddress = customerWalletAddress,
                customerWalletId = UserWalletIdBuilder.walletPublicKey(walletPublicKey).stringValue,
            )
        }.getOrElse { return CompletionResult.Failure(it.tangemError) }

        val signature = when (
            val result = signPersonalMessage(
                session = session,
                walletPublicKey = walletPublicKey,
                content = VisaUtilities.signWithNonceMessage(challenge.challenge),
                customerWalletKey = customerWalletKey,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<String> -> result.data
        }

        val authTokens = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getTokenWithCustomerWallet(
                sessionId = challenge.session.sessionId,
                signature = signature,
                nonce = challenge.challenge,
            )
        }.getOrNull() ?: return CompletionResult.Failure(VisaActivationError.FailedRemoteState.tangemError)

        return CompletionResult.Success(authTokens)
    }

    private suspend fun signPersonalMessage(
        session: CardSession,
        walletPublicKey: ByteArray,
        content: String,
        customerWalletKey: ExtendedPublicKey,
    ): CompletionResult<String> {
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
}