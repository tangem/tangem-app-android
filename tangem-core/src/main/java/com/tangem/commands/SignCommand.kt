package com.tangem.commands

import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
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
class SignCommand(private val hashes: Array<ByteArray>) : Command<SignResponse>() {

    override val requiresPin2 = true

    //TODO: Allow signing more than 10 hashes

    private val hashSizes = if (hashes.isNotEmpty()) hashes.first().size else 0

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.isActivated) {
            return TangemSdkError.NotActivated()
        }
        if (card.walletRemainingSignatures == 0) {
            return TangemSdkError.NoRemainingSignatures()
        }
        if (card.signingMethods?.contains(SigningMethod.SignHash) != true) {
            return TangemSdkError.SignHashesNotAvailable()
        }
        if (hashSizes == 0) {
            return TangemSdkError.EmptyHashes()
        }
        if (hashes.any { it.size != hashSizes }) {
            return TangemSdkError.HashSizeMustBeEqual()
        }

        return when (card.status) {
            CardStatus.Loaded -> null
            CardStatus.Empty -> TangemSdkError.
            CardIsEmpty()
            CardStatus.NotPersonalized -> TangemSdkError.NotPersonalized()
            CardStatus.Purged -> TangemSdkError.CardIsPurged()
            null -> TangemSdkError.CardError()
        }
    }

    override fun mapError(card: Card?, error: TangemSdkError): TangemSdkError {
        if (error is TangemSdkError.InvalidParams) {
            return TangemSdkError.Pin2OrCvcRequired()
        }
        return error
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
        return CommandApdu(Instruction.Sign, tlvBuilder.serialize())
    }

    private fun flattenHashes(): ByteArray {
        return hashes.reduce { arr1, arr2 -> arr1 + arr2 }
    }

    /**
     * Application can optionally submit a public key Terminal_PublicKey in [SignCommand].
     * Submitted key is stored by the Tangem card if it differs from a previous submitted Terminal_PublicKey.
     * The Tangem card will not enforce security delay if [SignCommand] will be called with
     * TerminalTransactionSignature parameter containing a correct signature of raw data to be signed made with TerminalPrivateKey
     * (this key should be generated and securily stored by the application).
     */
    private fun addTerminalSignature(
        environment: SessionEnvironment, dataToSign: ByteArray, tlvBuilder: TlvBuilder
    ) {
        environment.terminalKeys?.let { terminalKeyPair ->
            val signedData = dataToSign.sign(terminalKeyPair.privateKey)
            tlvBuilder.append(TlvTag.TerminalTransactionSignature, signedData)
            tlvBuilder.append(TlvTag.TerminalPublicKey, terminalKeyPair.publicKey)
        }
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SignResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return SignResponse(
            cardId = decoder.decode(TlvTag.CardId),
            signature = decoder.decode(TlvTag.Signature),
            walletRemainingSignatures = decoder.decode(TlvTag.RemainingSignatures),
            walletSignedHashes = decoder.decode(TlvTag.SignedHashes)
        )
    }
}