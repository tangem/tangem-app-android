package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign

/**
 * @param cardId CID, Unique Tangem card ID number
 * @param signature Signed hashes (array of resulting signatures)
 * @param walletRemainingSignatures Remaining number of sign operations before the wallet will stop signing transactions.
 * @param walletSignedHashes Total number of signed single hashes returned by the card in sign command responses.
 * Sums up array elements within all SIGN commands
 */
class SignResponse(
        val cardId: String,
        val signature: ByteArray,
        val walletRemainingSignatures: Int,
        val walletSignedHashes: Int
) : CommandResponse

/**
 * Signs transaction hashes using a wallet private key, stored on the card.
 *
 * @property hashes Array of transaction hashes.
 * @property cardId CID, Unique Tangem card ID number
 */
class SignCommand(private val hashes: Array<ByteArray>)
    : Command<SignResponse>() {

    private val hashSizes = if (hashes.isNotEmpty()) hashes.first().size else 0

    override fun performPreCheck(session: CardSession, callback: (result: CompletionResult<SignResponse>) -> Unit): Boolean {
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return true
        }
        if (session.environment.card?.isActivated == true) {
            callback(CompletionResult.Failure(TangemSdkError.NotActivated()))
            return true
        }
        if (session.environment.card?.status == CardStatus.Purged) {
            callback(CompletionResult.Failure(TangemSdkError.CardIsPurged()))
            return true
        }
        if (session.environment.card?.status == CardStatus.Empty) {
            callback(CompletionResult.Failure(TangemSdkError.CardIsEmpty()))
            return true
        }
        if (session.environment.card?.walletRemainingSignatures == 0) {
            callback(CompletionResult.Failure(TangemSdkError.NoRemainingSignatures()))
            return true
        }
        if (session.environment.card?.signingMethods?.contains(SigningMethod.SignHash) != true) {
            callback(CompletionResult.Failure(TangemSdkError.SignHashesNotAvailable()))
            return true
        }
        if (hashSizes == 0) {
            callback(CompletionResult.Failure(TangemSdkError.EmptyHashes()))
            return true
        }
        if (hashes.any { it.size != hashSizes }) {
            callback(CompletionResult.Failure(TangemSdkError.HashSizeMustBeEqual()))
            return true
        }
        return false
    }

    override fun performAfterCheck(session: CardSession,
                                   result: CompletionResult<SignResponse>,
                                   callback: (result: CompletionResult<SignResponse>) -> Unit): Boolean {
        when (result) {
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.InvalidParams) {
                    callback(CompletionResult.Failure(TangemSdkError.Pin2OrCvcRequired()))
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val dataToSign = flattenHashes()
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.TransactionOutHashSize, byteArrayOf(hashSizes.toByte()))
        tlvBuilder.append(TlvTag.TransactionOutHash, dataToSign)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)

        addTerminalSignature(environment, dataToSign, tlvBuilder)
        return CommandApdu(
                Instruction.Sign, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    private fun flattenHashes(): ByteArray {
        checkForErrors()
        return hashes.reduce { arr1, arr2 -> arr1 + arr2 }
    }

    private fun checkForErrors() {
        if (hashes.isEmpty()) throw TangemSdkError.EmptyHashes()
        if (hashes.size > 10) throw TangemSdkError.TooManyhHashesInOneTransaction()
        if (hashes.any { it.size != hashSizes }) throw TangemSdkError.HashSizeMustBeEqual()
    }

    /**
     * Application can optionally submit a public key Terminal_PublicKey in [SignCommand].
     * Submitted key is stored by the Tangem card if it differs from a previous submitted Terminal_PublicKey.
     * The Tangem card will not enforce security delay if [SignCommand] will be called with
     * TerminalTransactionSignature parameter containing a correct signature of raw data to be signed made with TerminalPrivateKey
     * (this key should be generated and securily stored by the application).
     */
    private fun addTerminalSignature(
            environment: SessionEnvironment, dataToSign: ByteArray, tlvBuilder: TlvBuilder) {
        environment.terminalKeys?.let { terminalKeyPair ->
            val signedData = dataToSign.sign(terminalKeyPair.privateKey)
            tlvBuilder.append(TlvTag.TerminalTransactionSignature, signedData)
            tlvBuilder.append(TlvTag.TerminalPublicKey, terminalKeyPair.publicKey)
        }
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SignResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SignResponse(
                cardId = decoder.decode(TlvTag.CardId),
                signature = decoder.decode(TlvTag.Signature),
                walletRemainingSignatures = decoder.decode(TlvTag.RemainingSignatures),
                walletSignedHashes = decoder.decode(TlvTag.SignedHashes)
        )
    }
}