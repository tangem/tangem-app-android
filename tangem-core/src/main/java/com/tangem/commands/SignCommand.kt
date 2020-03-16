package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign
import com.tangem.tasks.TaskError

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
    : CommandSerializer<SignResponse>() {

    private val hashSizes = if (hashes.isNotEmpty()) hashes.first().size else 0
    private val dataToSign = flattenHashes()

    private fun flattenHashes(): ByteArray {
        checkForErrors()
        return hashes.reduce { arr1, arr2 -> arr1 + arr2 }
    }

    private fun checkForErrors() {
        if (hashes.isEmpty()) throw TaskError.EmptyHashes()
        if (hashes.size > 10) throw TaskError.TooMuchHashesInOneTransaction()
        if (hashes.any { it.size != hashSizes }) throw TaskError.HashSizeMustBeEqual()
    }

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.Pin2, cardEnvironment.pin2)
        tlvBuilder.append(TlvTag.CardId, cardEnvironment.cardId)
        tlvBuilder.append(TlvTag.TransactionOutHashSize, byteArrayOf(hashSizes.toByte()))
        tlvBuilder.append(TlvTag.TransactionOutHash, dataToSign)
        tlvBuilder.append(TlvTag.Cvc, cardEnvironment.cvc)

        addTerminalSignature(cardEnvironment, tlvBuilder)
        return CommandApdu(
                Instruction.Sign, tlvBuilder.serialize(),
                cardEnvironment.encryptionMode, cardEnvironment.encryptionKey
        )
    }

    /**
     * Application can optionally submit a public key Terminal_PublicKey in [SignCommand].
     * Submitted key is stored by the Tangem card if it differs from a previous submitted Terminal_PublicKey.
     * The Tangem card will not enforce security delay if [SignCommand] will be called with
     * TerminalTransactionSignature parameter containing a correct signature of raw data to be signed made with TerminalPrivateKey
     * (this key should be generated and securily stored by the application).
     */
    private fun addTerminalSignature(cardEnvironment: CardEnvironment, tlvBuilder: TlvBuilder) {
        cardEnvironment.terminalKeys?.let { terminalKeyPair ->
            val signedData = dataToSign.sign(terminalKeyPair.privateKey)
            tlvBuilder.append(TlvTag.TerminalTransactionSignature, signedData)
            tlvBuilder.append(TlvTag.TerminalPublicKey, terminalKeyPair.publicKey)
        }
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): SignResponse? {
        val tlvData = responseApdu.getTlvData(cardEnvironment.encryptionKey) ?: return null

        val tlvMapper = TlvMapper(tlvData)
        return SignResponse(
                cardId = tlvMapper.map(TlvTag.CardId),
                signature = tlvMapper.map(TlvTag.Signature),
                walletRemainingSignatures = tlvMapper.map(TlvTag.RemainingSignatures),
                walletSignedHashes = tlvMapper.map(TlvTag.SignedHashes)
        )
    }
}