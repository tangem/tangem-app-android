package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extentions.calculateSha256
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError
import java.util.*

/**
 * Determines which type of data is required for signing.
 */
enum class SigningMethod(val code: Int) {
    SignHash(0),
    SignRaw(1),
    SignHashValidatedByIssuer(2),
    SignRawValidatedByIssuer(3),
    SignHashValidatedByIssuerAndWriteIssuerData(4),
    SignRawValidatedByIssuerAndWriteIssuerData(5),
    SignPos(6);

    companion object {
        private val values = values()
        fun byCode(code: Int): SigningMethod? = values.find { it.code == code }
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

enum class ProductMask(val code: Byte) {
    Note(0x01),
    Tag(0x02),
    Card(0x04);

    companion object {
        private val values = values()
        fun byCode(code: Byte): ProductMask? = values.find { it.code == code }
    }
}

/**
 * Stores and maps Tangem card settings.
 *
 * @property rawValue are card settings in a form of flags,
 * while flags definitions and values are in [SettingsMask.Companion] as constants.
 */
data class SettingsMask(val rawValue: Int) {

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
        val batchId: String?,
        val manufactureDateTime: Date?,
        val issuerName: String?,
        val blockchainName: String?,
        val manufacturerSignature: ByteArray?,
        val productMask: ProductMask?,

        val tokenSymbol: String?,
        val tokenContractAddress: String?,
        val tokenDecimal: Int?
)

/**
 * Response for [ReadCommand]. Contains detailed card information.
 */
class Card(
        val cardId: String,
        val manufacturerName: String,
        val status: CardStatus,

        val firmwareVersion: String?,
        val cardPublicKey: ByteArray?,
        val settingsMask: SettingsMask?,
        val issuerPublicKey: ByteArray?,
        val curve: EllipticCurve?,
        val maxSignatures: Int?,
        val signingMethod: SigningMethod?,
        val pauseBeforePin2: Int?,
        val walletPublicKey: ByteArray?,
        val walletRemainingSignatures: Int?,
        val walletSignedHashes: Int?,
        val health: Int?,
        val isActivated: Boolean,
        val activationSeed: ByteArray?,
        val paymentFlowVersion: ByteArray?,
        val userCounter: Int?,
        val terminalIsLinked: Boolean,
        val cardData: CardData?
) : CommandResponse

/**
 * This command receives from the Tangem Card all the data about the card and the wallet,
 * including unique card number (CID or cardId) that has to be submitted while calling all other commands.
 */
class ReadCommand : CommandSerializer<Card>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        /**
         *  [CardEnvironment] stores the pin1 value. If no pin1 value was set, it will contain
         *  default value of ‘000000’.
         *  In order to obtain card’s data, [ReadCommand] should use the correct pin 1 value.
         *  The card will not respond if wrong pin 1 has been submitted.
         */
        val tlvData = mutableListOf(Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()))

        cardEnvironment.terminalKeys?.let { terminalKeys ->
            Tlv(TlvTag.TerminalPublicKey, terminalKeys.publicKey)
        }

        return CommandApdu(Instruction.Read, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): Card? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val tlvMapper = TlvMapper(tlvData)

            Card(
                    cardId = tlvMapper.map(TlvTag.CardId),
                    manufacturerName = tlvMapper.map(TlvTag.ManufactureId),
                    status = tlvMapper.map(TlvTag.Status),

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
            Tlv.tlvListFromBytes(it.value)
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