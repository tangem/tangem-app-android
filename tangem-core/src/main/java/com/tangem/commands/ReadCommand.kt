package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError
import java.util.*

/**
 * Determines which type of data is required for signing.
 */
data class SigningMethod(val rawValue: Int) {

    fun contains(value: Int): Boolean {
        return if (rawValue and 0x80 == 0) {
            value == rawValue
        } else {
            rawValue and (0x01 shl value) != 0
        }
    }

    companion object {
        const val signHash = 0
        const val signRaw = 1
        const val signHashValidatedByIssuer = 2
        const val signRawValidatedByIssuer = 3
        const val signHashValidatedByIssuerAndWriteIssuerData = 4
        const val signRawValidatedByIssuerAndWriteIssuerData = 5
        const val signPos = 6
    }
}

/**
 * Elliptic curve used for wallet key operations.
 */
enum class EllipticCurve(val curve: String) {
    Secp256k1("secp256k1"),
    Ed25519("ed25519");

    companion object {
        private val values = values()
        fun byName(curve: String): EllipticCurve? = values.find { it.curve == curve }
    }
}

/**
 * Status of the card and its wallet.
 */
enum class CardStatus(val code: Int) {
    NotPersonalized(0),
    Empty(1),
    Loaded(2),
    Purged(3);

    companion object {
        private val values = values()
        fun byCode(code: Int): CardStatus? = values.find { it.code == code }
    }
}

/**
 * Mask of products enabled on card
 * @property rawValue Products mask values,
 * while flags definitions and values are in [ProductMask.Companion] as constants.
 */
data class ProductMask(val rawValue: Int) {

    fun contains(value: Int): Boolean = (rawValue and value) != 0

    companion object {
        const val note = 0x01
        const val tag = 0x02
        const val idCard = 0x04
    }
}

/**
 * Stores and maps Tangem card settings.
 *
 * @property rawValue Card settings in a form of flags,
 * while flags definitions and values are in [SettingsMask.Companion] as constants.
 */
data class SettingsMask(val rawValue: Int) {

    fun contains(value: Int): Boolean = (rawValue and value) != 0

    companion object {
        const val isReusable = 0x0001
        const val useActivation = 0x0002
        const val forbidPurgeWallet = 0x0004
        const val useBlock = 0x0008

        const val allowSwapPIN = 0x0010
        const val allowSwapPIN2 = 0x0020
        const val useCVC = 0x0040
        const val forbidDefaultPIN = 0x0080

        const val useOneCommandAtTime = 0x0100
        const val useNdef = 0x0200
        const val useDynamicNdef = 0x0400
        const val smartSecurityDelay = 0x0800

        const val protocolAllowUnencrypted = 0x1000
        const val protocolAllowStaticEncryption = 0x2000

        const val protectIssuerDataAgainstReplay = 0x4000

        const val allowSelectBlockchain = 0x8000

        const val disablePrecomputedNdef = 0x00010000

        const val skipSecurityDelayIfValidatedByLinkedTerminal = 0x00080000
    }
}

/**
 * Detailed information about card contents.
 */
class CardData(

        /**
         * Tangem internal manufacturing batch ID.
         */
        val batchId: String?,

        /**
         * Timestamp of manufacturing.
         */
        val manufactureDateTime: Date?,

        /**
         * Name of the issuer.
         */
        val issuerName: String?,

        /**
         * Name of the blockchain.
         */
        val blockchainName: String?,

        /**
         * Signature of CardId with manufacturer’s private key.
         */
        val manufacturerSignature: ByteArray?,

        /**
         * Mask of products enabled on card.
         */
        val productMask: ProductMask?,

        /**
         * Name of the token.
         */
        val tokenSymbol: String?,

        /**
         * Smart contract address.
         */
        val tokenContractAddress: String?,

        /**
         * Number of decimals in token value.
         */
        val tokenDecimal: Int?
)

/**
 * Response for [ReadCommand]. Contains detailed card information.
 */
class Card(

        /**
         * Unique Tangem card ID number.
         */
        val cardId: String,

        /**
         * Name of Tangem card manufacturer.
         */
        val manufacturerName: String,

        /**
         * Current status of the card.
         */
        val status: CardStatus?,

        /**
         * Version of Tangem COS.
         */
        val firmwareVersion: String?,

        /**
         * Public key that is used to authenticate the card against manufacturer’s database.
         * It is generated one time during card manufacturing.
         */
        val cardPublicKey: ByteArray?,

        /**
         * Card settings defined by personalization (bit mask: 0 – Enabled, 1 – Disabled).
         */
        val settingsMask: SettingsMask?,

        /**
         * Public key that is used by the card issuer to sign IssuerData field.
         */
        val issuerPublicKey: ByteArray?,

        /**
         * Explicit text name of the elliptic curve used for all wallet key operations.
         * Supported curves: ‘secp256k1’ and ‘ed25519’.
         */
        val curve: EllipticCurve?,

        /**
         * Total number of signatures allowed for the wallet when the card was personalized.
         */
        val maxSignatures: Int?,

        /**
         * Defines what data should be submitted to SIGN command.
         */
        val signingMethod: SigningMethod?,

        /**
         * Delay in seconds before COS executes commands protected by PIN2.
         */
        val pauseBeforePin2: Int?,

        /**
         * Public key of the blockchain wallet.
         */
        val walletPublicKey: ByteArray?,

        /**
         * Remaining number of [SignCommand] operations before the wallet will stop signing transactions.
         */
        val walletRemainingSignatures: Int?,

        /**
         * Total number of signed single hashes returned by the card in
         * [SignCommand] responses since card personalization.
         * Sums up array elements within all [SignCommand].
         */
        val walletSignedHashes: Int?,

        /**
         * Any non-zero value indicates that the card experiences some hardware problems.
         * User should withdraw the value to other blockchain wallet as soon as possible.
         * Non-zero Health tag will also appear in responses of all other commands.
         */
        val health: Int?,

        /**
         * Whether the card requires issuer’s confirmation of activation.
         */
        val isActivated: Boolean,

        /**
         * A random challenge generated by personalisation that should be signed and returned
         * to COS by the issuer to confirm the card has been activated.
         * This field will not be returned if the card is activated.
         */
        val activationSeed: ByteArray?,

        /**
         * Returned only if [SigningMethod.SignPos] enabling POS transactions is supported by card.
         */
        val paymentFlowVersion: ByteArray?,

        /**
         * This value can be initialized by terminal and will be increased by COS on execution of every [SignCommand].
         * For example, this field can store blockchain “nonce” for quick one-touch transaction on POS terminals.
         * Returned only if [SigningMethod.SignPos]  enabling POS transactions is supported by card.
         */
        val userCounter: Int?,

        /**
         * When this value is true, it means that the application is linked to the card,
         * and COS will not enforce security delay if [SignCommand] will be called
         * with [TlvTag.TerminalTransactionSignature] parameter containing a correct signature of raw data
         * to be signed made with [TlvTag.TerminalPublicKey].
         */
        val terminalIsLinked: Boolean,

        /**
         * Detailed information about card contents. Format is defined by the card issuer.
         * Cards complaint with Tangem Wallet application should have TLV format.
         */
        val cardData: CardData?
) : CommandResponse

/**
 * This command receives from the Tangem Card all the data about the card and the wallet,
 * including unique card number (CID or cardId) that has to be submitted while calling all other commands.
 */
class ReadCommand : CommandSerializer<Card>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        /**
         *  [CardEnvironment] stores the pin1 value. If no pin1 value was set, it will contain
         *  default value of ‘000000’.
         *  In order to obtain card’s data, [ReadCommand] should use the correct pin 1 value.
         *  The card will not respond if wrong pin 1 has been submitted.
         */
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.TerminalPublicKey, cardEnvironment.terminalKeys?.publicKey)
        return CommandApdu(
                Instruction.Read, tlvBuilder.serialize(),
                cardEnvironment.encryptionMode, cardEnvironment.encryptionKey
        )
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): Card? {
        val tlvData = responseApdu.getTlvData(cardEnvironment.encryptionKey) ?: return null

        return try {
            val tlvMapper = TlvMapper(tlvData)

            Card(
                    cardId = tlvMapper.mapOptional(TlvTag.CardId) ?: "",
                    manufacturerName = tlvMapper.mapOptional(TlvTag.ManufactureId) ?: "",
                    status = tlvMapper.mapOptional(TlvTag.Status),

                    firmwareVersion = tlvMapper.mapOptional(TlvTag.Firmware),
                    cardPublicKey = tlvMapper.mapOptional(TlvTag.CardPublicKey),
                    settingsMask = tlvMapper.mapOptional(TlvTag.SettingsMask),
                    issuerPublicKey = tlvMapper.mapOptional(TlvTag.IssuerDataPublicKey),
                    curve = tlvMapper.mapOptional(TlvTag.CurveId),
                    maxSignatures = tlvMapper.mapOptional(TlvTag.MaxSignatures),
                    signingMethod = tlvMapper.mapOptional(TlvTag.SigningMethod),
                    pauseBeforePin2 = tlvMapper.mapOptional(TlvTag.PauseBeforePin2),
                    walletPublicKey = tlvMapper.mapOptional(TlvTag.WalletPublicKey),
                    walletRemainingSignatures = tlvMapper.mapOptional(TlvTag.RemainingSignatures),
                    walletSignedHashes = tlvMapper.mapOptional(TlvTag.SignedHashes),
                    health = tlvMapper.mapOptional(TlvTag.Health),
                    isActivated = tlvMapper.map(TlvTag.IsActivated),
                    activationSeed = tlvMapper.mapOptional(TlvTag.ActivationSeed),
                    paymentFlowVersion = tlvMapper.mapOptional(TlvTag.PaymentFlowVersion),
                    userCounter = tlvMapper.mapOptional(TlvTag.UserCounter),
                    terminalIsLinked = tlvMapper.map(TlvTag.TerminalIsLinked),

                    cardData = deserializeCardData(tlvData)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }

    private fun deserializeCardData(tlvData: List<Tlv>): CardData? {
        val cardDataTlvs = tlvData.find { it.tag == TlvTag.CardData }?.let {
            Tlv.deserialize(it.value)
        }
        if (cardDataTlvs.isNullOrEmpty()) return null

        val tlvMapper = TlvMapper(cardDataTlvs)
        return CardData(
                batchId = tlvMapper.mapOptional(TlvTag.Batch),
                manufactureDateTime = tlvMapper.mapOptional(TlvTag.ManufactureDateTime),
                issuerName = tlvMapper.mapOptional(TlvTag.IssuerId),
                blockchainName = tlvMapper.mapOptional(TlvTag.BlockchainId),
                manufacturerSignature = tlvMapper.mapOptional(TlvTag.ManufacturerSignature),
                productMask = tlvMapper.mapOptional(TlvTag.ProductMask),

                tokenSymbol = tlvMapper.mapOptional(TlvTag.TokenSymbol),
                tokenContractAddress = tlvMapper.mapOptional(TlvTag.TokenContractAddress),
                tokenDecimal = tlvMapper.mapOptional(TlvTag.TokenDecimal)
        )
    }
}