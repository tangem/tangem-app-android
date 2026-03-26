package com.tangem.tap.domain.tasks.visa

import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.payment.models.auth.PaymentAuthConfig
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.VisaDataToSignByCustomerWallet
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.domain.visa.model.sign
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.sdk.api.visa.PaymentGenerateChallengeHelper
import com.tangem.sdk.api.visa.PaymentSignChallengeResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PaymentGenerateAddressAndSignChallengeTask @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val config: PaymentAuthConfig,
    @Assisted private val userWalletId: String,
    @Assisted private val generateChallengeHelper: PaymentGenerateChallengeHelper,
) : CardSessionRunnable<PaymentSignChallengeResult> {

    override fun run(session: CardSession, callback: CompletionCallback<PaymentSignChallengeResult>) {
        coroutineScope.launch {
            callback(runSuspend(session = session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<PaymentSignChallengeResult> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())
        val wallet = card.wallets.firstOrNull { it.curve == config.curve }
            ?: return CompletionResult.Failure(VisaActivationError.MissingWallet.tangemError)

        val extendedPublicKey = when (val derivationResult = runDerivationTask(session, wallet)) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(derivationResult.error)
            is CompletionResult.Success<ExtendedPublicKey> -> derivationResult.data
        }

        val address = generateAddressFromExtendedKey(
            extendedPublicKey = extendedPublicKey,
            blockchain = Blockchain.fromId(config.blockchainId),
        )

        val challenge = generateChallengeHelper.generateChallenge(address, userWalletId)
            .getOrElse { return CompletionResult.Failure(it) }

        val dataToSign = VisaDataToSignByCustomerWallet(hashToSign = challenge.challenge)
        val approveResult = runVisaCustomerWalletApproveTask(
            session = session,
            targetAddress = address,
            dataToSign = dataToSign,
            challenge = challenge.challenge,
        )
        val signedData = when (approveResult) {
            is CompletionResult.Failure<*> -> return CompletionResult.Failure(approveResult.error)
            is CompletionResult.Success<VisaSignedDataByCustomerWallet> -> approveResult.data
        }

        return CompletionResult.Success(
            data = PaymentSignChallengeResult(
                address = address,
                challenge = challenge,
                signature = signedData.signature,

            ),
        )
    }

    private suspend fun runVisaCustomerWalletApproveTask(
        session: CardSession,
        targetAddress: String,
        dataToSign: VisaDataToSignByCustomerWallet,
        challenge: String,
    ): CompletionResult<VisaSignedDataByCustomerWallet> {
        val deferred = CompletableDeferred<CompletionResult<VisaSignedDataByCustomerWallet>>()
        val task = VisaCustomerWalletApproveTask(
            visaDataForApprove = VisaCustomerWalletApproveTask.Input(
                cardId = null,
                targetAddress = targetAddress,
                hashToSign = VisaDataToSignByCustomerWallet(hashToSign = challenge).hashToSign,
                sign = dataToSign::sign,
            ),
        )
        task.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private suspend fun runDerivationTask(
        session: CardSession,
        wallet: CardWallet,
    ): CompletionResult<ExtendedPublicKey> {
        val deferred = CompletableDeferred<CompletionResult<ExtendedPublicKey>>()
        val derivationTask = DeriveWalletPublicKeyTask(
            walletPublicKey = wallet.publicKey,
            derivationPath = config.customDerivationPath,
        )

        derivationTask.run(session = session, callback = deferred::complete)
        return deferred.await()
    }

    private fun generateAddressFromExtendedKey(extendedPublicKey: ExtendedPublicKey, blockchain: Blockchain): String {
        val derivationData = blockchain.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            cachedIndex = null,
        )
        return derivationData.address
    }

    @AssistedFactory
    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            config: PaymentAuthConfig,
            userWalletId: String,
            generateChallengeHelper: PaymentGenerateChallengeHelper,
        ): PaymentGenerateAddressAndSignChallengeTask
    }
}