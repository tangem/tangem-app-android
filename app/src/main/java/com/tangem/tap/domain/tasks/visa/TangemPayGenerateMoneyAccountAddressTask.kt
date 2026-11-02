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
import com.tangem.operations.sign.SignHashesCommand
import com.tangem.operations.sign.SignHashesResponse
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
        val authorization = when (
            val result = authorizeAndSignDepositAddress(
                session = session,
                walletPublicKey = publicKey,
                customerWalletAddress = customerWalletAddress,
                customerWalletKey = customerWalletKey,
                depositAddress = address,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<Authorization> -> result.data
        }

        val derivedKeys = mapOf(
            publicKey.toMapKey() to ExtendedPublicKeysMap(
                mapOf(VisaUtilities.moneyAccountDerivationPath to depositKey),
            ),
        )

        return CompletionResult.Success(
            data = MoneyAccountActivationData(
                address = address,
                signature = authorization.depositAddressSignature,
                derivedKeys = derivedKeys,
                initialCredentials = TangemPayInitialCredentials(
                    customerWalletAddress = customerWalletAddress,
                    authTokens = authorization.authTokens,
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

    private suspend fun authorizeAndSignDepositAddress(
        session: CardSession,
        walletPublicKey: ByteArray,
        customerWalletAddress: String,
        customerWalletKey: ExtendedPublicKey,
        depositAddress: String,
    ): CompletionResult<Authorization> {
        val challenge = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getCustomerWalletAuthChallenge(
                customerWalletAddress = customerWalletAddress,
                customerWalletId = UserWalletIdBuilder.walletPublicKey(walletPublicKey).stringValue,
            )
        }.getOrElse { return CompletionResult.Failure(it.tangemError) }

        val signatures = when (
            val result = signPersonalMessages(
                session = session,
                walletPublicKey = walletPublicKey,
                contents = listOf(
                    VisaUtilities.signWithNonceMessage(challenge.challenge),
                    VisaUtilities.moneyAccountDepositAddressMessage(depositAddress = depositAddress),
                ),
                customerWalletKey = customerWalletKey,
            )
        ) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(result.error)
            is CompletionResult.Success<List<String>> -> result.data
        }
        val (authSignature, depositAddressSignature) = signatures

        val authTokens = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getTokenWithCustomerWallet(
                sessionId = challenge.session.sessionId,
                signature = authSignature,
                nonce = challenge.challenge,
            )
        }.getOrNull() ?: return CompletionResult.Failure(VisaActivationError.FailedRemoteState.tangemError)

        return CompletionResult.Success(
            Authorization(authTokens = authTokens, depositAddressSignature = depositAddressSignature),
        )
    }

    private suspend fun signPersonalMessages(
        session: CardSession,
        walletPublicKey: ByteArray,
        contents: List<String>,
        customerWalletKey: ExtendedPublicKey,
    ): CompletionResult<List<String>> {
        val hashes = contents.map { VisaUtilities.hashPersonalMessage(it.toByteArray(Charsets.UTF_8)) }

        val deferred = CompletableDeferred<CompletionResult<SignHashesResponse>>()
        val signCommand = SignHashesCommand(
            hashes = hashes.toTypedArray(),
            walletPublicKey = walletPublicKey,
            derivationPath = VisaUtilities.customDerivationPath,
        )

        signCommand.run(session = session, callback = deferred::complete)

        return when (val result = deferred.await()) {
            is CompletionResult.Failure<*> -> CompletionResult.Failure(result.error)
            is CompletionResult.Success<SignHashesResponse> -> CompletionResult.Success(
                data = hashes.zip(result.data.signatures) { hash, signature ->
                    VisaUtilities.unmarshallSignature(
                        signature = signature,
                        hash = hash,
                        extendedPublicKey = customerWalletKey,
                    )
                },
            )
        }
    }

    private class Authorization(
        val authTokens: TangemPayAuthTokens,
        val depositAddressSignature: String,
    )
}