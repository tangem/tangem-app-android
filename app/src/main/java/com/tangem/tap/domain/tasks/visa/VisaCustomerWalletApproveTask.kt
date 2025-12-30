package com.tangem.tap.domain.tasks.visa

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.card.common.visa.VisaWalletPublicKeyUtility
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.sign.SignHashCommand

class VisaCustomerWalletApproveTask(
    private val visaDataForApprove: Input,
) : CardSessionRunnable<VisaSignedDataByCustomerWallet> {

    override fun run(session: CardSession, callback: CompletionCallback<VisaSignedDataByCustomerWallet>) {
        val card = session.environment.card ?: run {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (VisaUtilities.isVisaCard(card.firmwareVersion.doubleValue, card.batchId)) {
            callback(CompletionResult.Failure(VisaActivationError.VisaCardForApproval.tangemError))
            return
        }

        if (visaDataForApprove.cardId != null && card.cardId != visaDataForApprove.cardId) {
            callback(CompletionResult.Failure(VisaActivationError.CardIdNotMatched.tangemError))
            return
        }

        proceedApprove(card, session, callback)
    }

    private fun proceedApprove(
        card: Card,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val derivationPath = VisaUtilities.customDerivationPath

        val wallet = card.wallets.firstOrNull { it.curve == VisaUtilities.curve } ?: run {
            callback(CompletionResult.Failure(VisaActivationError.MissingWallet.tangemError))
            return
        }

        val derivationTask = DeriveWalletPublicKeyTask(
            walletPublicKey = wallet.publicKey,
            derivationPath = derivationPath,
        )

        derivationTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    processDerivedKey(
                        wallet = wallet,
                        extendedPublicKey = result.data,
                        derivationPath = derivationPath,
                        session = session,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun processDerivedKey(
        wallet: CardWallet,
        extendedPublicKey: ExtendedPublicKey,
        derivationPath: DerivationPath,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val validationResult = VisaWalletPublicKeyUtility.validateExtendedPublicKey(
            targetAddress = visaDataForApprove.targetAddress,
            extendedPublicKey = extendedPublicKey,
        )

        validationResult.onLeft { error ->
            callback(CompletionResult.Failure(error.tangemError))
            return
        }

        signApproveData(
            targetWalletPublicKey = wallet.publicKey,
            derivationPath = derivationPath,
            session = session,
            extendedPublicKey = extendedPublicKey,
            callback = callback,
        )
    }

    private fun signApproveData(
        targetWalletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        extendedPublicKey: ExtendedPublicKey,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val content = VisaUtilities.signWithNonceMessage(visaDataForApprove.hashToSign)
        val hash = VisaUtilities.hashPersonalMessage(content.toByteArray(Charsets.UTF_8))

        val signTask = SignHashCommand(
            hash = hash,
            walletPublicKey = targetWalletPublicKey,
            derivationPath = derivationPath,
        )

        signTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val rsvSignature = VisaUtilities.unmarshallSignature(
                        signature = result.data.signature,
                        hash = hash,
                        extendedPublicKey = extendedPublicKey,
                    )

                    val signedData = visaDataForApprove.sign(rsvSignature, visaDataForApprove.targetAddress)
                    callback(CompletionResult.Success(signedData))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    data class Input(
        val cardId: String? = null,
        val targetAddress: String,
        val hashToSign: String,
        val sign: (signature: String, customerWalletAddress: String) -> VisaSignedDataByCustomerWallet,
    )
}