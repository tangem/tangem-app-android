package com.tangem.tap.domain.tasks.visa

import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.visa.model.VisaDataToSignByCustomerWallet
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.domain.visa.model.sign
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TangemPayGenerateAddressAndSignChallengeTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val dispatchersProvider: CoroutineDispatcherProvider,
    private val tangemPayRemoteDataSource: TangemPayRemoteDataSource,
) : CardSessionRunnable<TangemPayInitialCredentials> {

    override fun run(session: CardSession, callback: CompletionCallback<TangemPayInitialCredentials>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<TangemPayInitialCredentials> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.curve }
            ?: return CompletionResult.Failure(VisaActivationError.MissingWallet.tangemError)

        val address = when (val derivationResult = runDerivationTask(session, wallet)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(derivationResult.error)
            is CompletionResult.Success<ExtendedPublicKey> -> VisaUtilities.generateAddressFromExtendedKey(
                extendedPublicKey = derivationResult.data,
            )
        }

        val userWalletId = UserWalletIdBuilder.walletPublicKey(wallet.publicKey)
        val challenge = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getCustomerWalletAuthChallenge(
                customerWalletAddress = address,
                customerWalletId = userWalletId.stringValue,
            )
        }.getOrElse { return CompletionResult.Failure(it.tangemError) }

        val dataToSign = VisaDataToSignByCustomerWallet(hashToSign = challenge.challenge)
        val approveResult = runVisaCustomerWalletApproveTask(
            session = session,
            targetAddress = address,
            dataToSign = dataToSign,
        )
        val signedData = when (approveResult) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(approveResult.error)
            is CompletionResult.Success<VisaSignedDataByCustomerWallet> -> approveResult.data
        }

        val authTokens = withContext(dispatchersProvider.io) {
            tangemPayRemoteDataSource.getTokenWithCustomerWallet(
                sessionId = challenge.session.sessionId,
                signature = signedData.signature,
                nonce = signedData.dataToSign.hashToSign,
            )
        }.getOrNull() ?: return CompletionResult.Failure(VisaActivationError.FailedRemoteState.tangemError)

        return CompletionResult.Success(
            data = TangemPayInitialCredentials(
                customerWalletAddress = address,
                authTokens = authTokens,
            ),
        )
    }

    private suspend fun runDerivationTask(
        session: CardSession,
        wallet: CardWallet,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        val derivationTask = DeriveWalletPublicKeyTask(
            walletPublicKey = wallet.publicKey,
            derivationPath = VisaUtilities.customDerivationPath,
        )

        derivationTask.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private suspend fun runVisaCustomerWalletApproveTask(
        session: CardSession,
        targetAddress: String,
        dataToSign: VisaDataToSignByCustomerWallet,
    ): CompletionResult<VisaSignedDataByCustomerWallet> {
        val deferred = CompletableDeferred<CompletionResult<VisaSignedDataByCustomerWallet>>()
        val task = VisaCustomerWalletApproveTask(
            visaDataForApprove = VisaCustomerWalletApproveTask.Input(
                cardId = null,
                targetAddress = targetAddress,
                hashToSign = dataToSign.hashToSign,
                sign = dataToSign::sign,
            ),
        )
        task.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): TangemPayGenerateAddressAndSignChallengeTask
    }
}