package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extentions.calculateSha256
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.data.SettingsMask
import com.tangem.enums.Instruction
import com.tangem.tasks.TaskError
import java.util.*

enum class SigningMethod(val code: Int) {
    SignHash(0),
    SignRaw(1),
    SignHashValidatedByIssuer(2),
    SignRawValidatedByIssuer(3),
    SignHashValidatedByIssuerAndWriteIssuerData(4),
    SignRawValidatedByIssuerAndWriteIssuerData(5),
    SignPos(6);

    companion object {
        fun byCode(code: Int): SigningMethod? = values().find { it.code == code }
    }
}

enum class EllipticCurve(val curve: String) {
    Secp256k1("secp256k1"),
    Ed25519("ed25519");

    companion object {
        fun byName(curve: String): EllipticCurve? = values().find { it.curve == curve }
    }
}

enum class CardStatus(val code: Int) {
    NotPersonalized(0),
    Empty(1),
    Loaded(2),
    Purged(3);

    companion object {
        fun byCode(code: Int): CardStatus? = values().find { it.code == code }
    }
}

enum class ProductMask(val code: Byte) {
    Note(0x01),
    Tag(0x02),
    Card(0x04);

    companion object {
        fun byCode(code: Byte): ProductMask? = values().find { it.code == code }
    }
}

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

        //Card Data
        val batchId: String?,
        val manufactureDateTime: Date?,
        val issuerName: String?,
        val blockchainName: String?,
        val manufacturerSignature: ByteArray?,
        val productMask: ProductMask?,

        val tokenSymbol: String?,
        val tokenContractAddress: String?,
        val tokenDecimal: Int?
) : CommandResponse


class ReadCardCommand : CommandSerializer<Card>() {

    override val instruction = Instruction.Read
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {

        val tlvData = mutableListOf(Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()))

        cardEnvironment.terminalKeys?.let {
            Tlv(TlvTag.TerminalPublicKey, it.publicKey)
        }

        return CommandApdu(instructionCode, tlvData)
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
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }


}