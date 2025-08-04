package com.tangem.tap.domain.tasks.visa

import arrow.core.getOrElse
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.card.common.visa.VisaWalletPublicKeyUtility
import com.tangem.domain.card.common.visa.VisaWalletPublicKeyUtility.findKeyWithoutDerivation
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.domain.visa.model.sign
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.sign.SignHashCommand

class VisaCustomerWalletApproveTask(
    private val visaDataForApprove: VisaDataForApprove,
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

        if (visaDataForApprove.customerWalletCardId != null && card.cardId != visaDataForApprove.customerWalletCardId) {
            callback(CompletionResult.Failure(VisaActivationError.CardIdNotMatched.tangemError))
            return
        }

        if (card.settings.isHDWalletAllowed) {
            proceedApprove(card, session, callback)
        } else {
            proceedApproveWithLegacyCard(card, session, callback)
        }
    }

    private fun proceedApprove(
        card: Card,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val cardDTO = CardDTO(card)

        val derivationStyle = cardDTO.derivationStyleProvider.getDerivationStyle() ?: run {
            proceedApproveWithLegacyCard(
                card = card,
                session = session,
                callback = callback,
            )
            return
        }

        val derivationPath = VisaUtilities.visaDefaultDerivationPath(derivationStyle) ?: run {
            callback(CompletionResult.Failure(VisaActivationError.FailedToCreateAddress.tangemError))
            return
        }

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

        validationResult.onLeft {
            callback(CompletionResult.Failure(it.tangemError))
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

    private fun proceedApproveWithLegacyCard(
        card: Card,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val publicKey = findKeyWithoutDerivation(
            targetAddress = visaDataForApprove.targetAddress,
            card = CardDTO(card),
        ).getOrElse {
            callback(CompletionResult.Failure(it.tangemError))
            return
        }

        signApproveData(
            targetWalletPublicKey = publicKey,
            derivationPath = null,
            extendedPublicKey = null,
            session = session,
            callback = callback,
        )
    }

    private fun signApproveData(
        targetWalletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        extendedPublicKey: ExtendedPublicKey?,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val hashToSign = visaDataForApprove.dataToSign.hashToSign.hexToBytes()

        val signTask = SignHashCommand(
            hash = hashToSign,
            walletPublicKey = targetWalletPublicKey,
            derivationPath = derivationPath,
        )

        signTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    val rsvSignature = UnmarshalHelper.unmarshalSignatureExtended(
                        signature = result.data.signature,
                        hash = hashToSign,
                        publicKey = extendedPublicKey?.publicKey?.toDecompressedPublicKey()
                            ?: targetWalletPublicKey.toDecompressedPublicKey(),
                    ).asRSVLegacyEVM().toHexString().lowercase()

                    scanCard(
                        session = session,
                        callback = callback,
                        signedData = visaDataForApprove.dataToSign.sign(
                            signature = rsvSignature,
                            customerWalletAddress = visaDataForApprove.targetAddress,
                        ),
                    )
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun scanCard(
        signedData: VisaSignedDataByCustomerWallet,
        session: CardSession,
        callback: CompletionCallback<VisaSignedDataByCustomerWallet>,
    ) {
        val scanTask = ScanTask()
        scanTask.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(signedData))
                }
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }
}