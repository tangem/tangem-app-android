package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.SignedEthereumTransaction
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.CommandResponse
import com.tangem.operations.GenerateOTPCommand
import com.tangem.operations.GenerateOTPResponse
import com.tangem.operations.attestation.AttestWalletKeyCommand
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.operations.sign.SignHashesCommand
import com.tangem.tap.scope
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class RegistrationTaskResponse(
    val signedTransactions: List<SignedEthereumTransaction>,
    val attestResponse: AttestWalletKeyResponse,
) : CommandResponse

class SaltPayRegistrationTask(
    private var gnosisRegistrator: GnosisRegistrator,
    private var challenge: ByteArray,
    private val walletPublicKey: ByteArray,
    private val approvalValue: BigDecimal,
    private val spendLimitValue: BigDecimal,
) : CardSessionRunnable<RegistrationTaskResponse> {

    private var generateOTPResponse: GenerateOTPResponse? = null
    private var attestWalletResponse: AttestWalletKeyResponse? = null
    private var signedTransactions: List<SignedEthereumTransaction> = listOf()

    override fun run(session: CardSession, callback: CompletionCallback<RegistrationTaskResponse>) {
        generateOTP(session, callback)
    }

    private fun generateOTP(session: CardSession, callback: CompletionCallback<RegistrationTaskResponse>) {
        GenerateOTPCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    generateOTPResponse = result.data
                    attestWallet(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun attestWallet(
        session: CardSession,
        callback: CompletionCallback<RegistrationTaskResponse>,
    ) {
        val card = session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val walletPublicKey = card.wallets.firstOrNull()?.publicKey
        if (walletPublicKey == null || !walletPublicKey.contentEquals(this.walletPublicKey)) {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        val attestWalletCommand = AttestWalletKeyCommand(
            publicKey = walletPublicKey,
            challenge = challenge,
            confirmationMode = AttestWalletKeyCommand.ConfirmationMode.Dynamic,
        )
        attestWalletCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    attestWalletResponse = result.data
                    scope.launch { prepareTransactions(session, callback) }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private suspend fun prepareTransactions(
        session: CardSession,
        callback: CompletionCallback<RegistrationTaskResponse>,
    ) {
        val generateOTPResponse = this.generateOTPResponse.guard {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
            return
        }

        gnosisRegistrator.prepareBeforeMakingTxs().successOr {
            callback(CompletionResult.Failure(it.error))
            return
        }

        val approvalTx = gnosisRegistrator.makeApprovalTx(approvalValue).successOr {
            callback(CompletionResult.Failure(it.error))
            return
        }
        val setWalletTx = gnosisRegistrator.makeSetWalletTx().successOr {
            callback(CompletionResult.Failure(it.error))
            return
        }
        val initOtpTx = gnosisRegistrator.makeInitOtpTx(
            generateOTPResponse.rootOTP,
            generateOTPResponse.rootOTPCounter,
        ).successOr {
            callback(CompletionResult.Failure(it.error))
            return
        }

        val setSpendLimitTx = gnosisRegistrator.makeSetSpendLimitTx(spendLimitValue).successOr {
            callback(CompletionResult.Failure(it.error))
            return
        }

        signTransactions(
            transactions = listOf(
                approvalTx,
                setWalletTx,
                initOtpTx,
                setSpendLimitTx,
            ),
            session = session,
            callback = callback,
        )
    }

    private fun signTransactions(
        transactions: List<CompiledEthereumTransaction>,
        session: CardSession,
        callback: CompletionCallback<RegistrationTaskResponse>,
    ) {
        val walletPublicKey = session.environment.card?.wallets?.firstOrNull()?.publicKey.guard {
            callback(CompletionResult.Failure(TangemSdkError.WalletNotFound()))
            return
        }

        val hashes = transactions.map { it.hash }.toTypedArray()
        val signHashesCommand = SignHashesCommand(hashes, walletPublicKey)

        signHashesCommand.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    signedTransactions = transactions.zip(result.data.signatures).map {
                        SignedEthereumTransaction(it.first, it.second)
                    }
                    complete(callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun complete(callback: CompletionCallback<RegistrationTaskResponse>) {
        if (signedTransactions.isEmpty() || attestWalletResponse == null) {
            callback(CompletionResult.Failure(TangemSdkError.UnknownError()))
        } else {
            val result = RegistrationTaskResponse(signedTransactions, attestWalletResponse!!)
            callback(CompletionResult.Success(result))
        }
    }
}