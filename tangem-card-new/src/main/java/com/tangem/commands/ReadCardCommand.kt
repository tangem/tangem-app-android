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

class ReadCardResponse(
        val cardId: String,
        val manufacturerName: String,
        val status: CardStatus,

        val firmwareVersion: String?,
        val cardPublicKey: String?,
        val settingsMask: SettingsMask?,
        val issuerPublicKey: String?,
        val curve: EllipticCurve?,
        val maxSignatures: Int?,
        val signingMethpod: SigningMethod?,
        val pauseBeforePin2: Int?,
        val walletPublicKey: ByteArray?,
        val walletRemainingSignatures: Int?,
        val walletSignedHashes: Int?,
        val health: Int?,
        val isActivated: Boolean?,
        val activationSeed: ByteArray?,
        val paymentFlowVersion: ByteArray?,
        val userCounter: Int?,

        //Card Data
        val batchId: Int?,
        val manufactureDateTime: String?,
        val issuerName: String?,
        val blockchainName: String?,
        val manufacturerSignature: ByteArray?,
        val productMask: ProductMask?,

        val tokenSymbol: String?,
        val tokenContractAddress: String?,
        val tokenDecimal: Int?
) : CommandResponse


class ReadCardCommand(private val pin1: String) : CommandSerializer<ReadCardResponse>() {

    override val instruction = Instruction.Read
    override val instructionCode = instruction.code

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {

        val tlvData = listOf(
            Tlv(TlvTag.Pin, TlvTag.Pin.code, pin1.calculateSha256())
        )

        return CommandApdu(instructionCode, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): ReadCardResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val tlvMapper = TlvMapper(tlvData)

            ReadCardResponse(
                    tlvMapper.map(TlvTag.CardId),
                    tlvMapper.map(TlvTag.ManufactureId),
                    tlvMapper.map(TlvTag.Status),

                    tlvMapper.mapOptional(TlvTag.Firmware),
                    tlvMapper.mapOptional(TlvTag.CardPublicKey),
                    tlvMapper.mapOptional(TlvTag.SettingsMask),
                    tlvMapper.mapOptional(TlvTag.IssuerDataPublicKey),
                    tlvMapper.mapOptional(TlvTag.CurveId),
                    tlvMapper.mapOptional(TlvTag.MaxSignatures),
                    tlvMapper.mapOptional(TlvTag.SigningMethod),
                    tlvMapper.mapOptional(TlvTag.PauseBeforePin2),
                    tlvMapper.mapOptional(TlvTag.WalletPublicKey),
                    tlvMapper.mapOptional(TlvTag.RemainingSignatures),
                    tlvMapper.mapOptional(TlvTag.SignedHashes),
                    tlvMapper.mapOptional(TlvTag.Health),
                    tlvMapper.mapOptional(TlvTag.IsActivated),
                    tlvMapper.mapOptional(TlvTag.ActivationSeed),
                    tlvMapper.mapOptional(TlvTag.PaymentFlowVersion),
                    tlvMapper.mapOptional(TlvTag.UserCounter),

                    tlvMapper.mapOptional(TlvTag.Batch),
                    tlvMapper.mapOptional(TlvTag.ManufactureDateTime),
                    tlvMapper.mapOptional(TlvTag.IssuerId),
                    tlvMapper.mapOptional(TlvTag.BlockchainId),
                    tlvMapper.mapOptional(TlvTag.ManufacturerSignature),
                    tlvMapper.mapOptional(TlvTag.ProductMask),

                    tlvMapper.mapOptional(TlvTag.TokenSymbol),
                    tlvMapper.mapOptional(TlvTag.TokenContractAddress),
                    tlvMapper.mapOptional(TlvTag.TokenDecimal)
            )
        } catch (exception: Exception) {
            null
        }
    }


}