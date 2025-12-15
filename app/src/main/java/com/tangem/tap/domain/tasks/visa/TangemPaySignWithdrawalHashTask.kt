package com.tangem.tap.domain.tasks.visa

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.sign.SignHashCommand

class TangemPaySignWithdrawalHashTask(
    private val cardId: String,
    private val hash: ByteArray,
) : CardSessionRunnable<String> {

    override fun run(session: CardSession, callback: CompletionCallback<String>) {
        val card = session.environment.card ?: run {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        if (card.cardId != cardId) {
            callback(CompletionResult.Failure(VisaActivationError.CardIdNotMatched.tangemError))
            return
        }

        proceedSign(card, session, callback)
    }

    private fun proceedSign(card: Card, session: CardSession, callback: CompletionCallback<String>) {
        val derivationPath = VisaUtilities.customDerivationPath

        val wallet = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: run {
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
                    signData(
                        targetWalletPublicKey = wallet.publicKey,
                        derivationPath = derivationPath,
                        session = session,
                        extendedPublicKey = result.data,
                        callback = callback,
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun signData(
        targetWalletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        extendedPublicKey: ExtendedPublicKey?,
        session: CardSession,
        callback: CompletionCallback<String>,
    ) {
        val signTask = SignHashCommand(
            hash = hash,
            walletPublicKey = targetWalletPublicKey,
            derivationPath = derivationPath,
        )

        signTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val rsvSignature = UnmarshalHelper.unmarshalSignatureExtended(
                        signature = result.data.signature,
                        hash = hash,
                        publicKey = extendedPublicKey?.publicKey?.toDecompressedPublicKey()
                            ?: targetWalletPublicKey.toDecompressedPublicKey(),
                    ).asRSVLegacyEVM().toHexString().lowercase()

                    callback(CompletionResult.Success(rsvSignature))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}