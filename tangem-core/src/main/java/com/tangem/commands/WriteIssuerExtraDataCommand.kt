package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

enum class IssuerDataMode(val code: Byte) {
    ExtraData(1),
    WriteExtraData(2),
    FinalizeExtraData(3);

    companion object {
        private val values = CardStatus.values()
        fun byCode(code: Int): CardStatus? = values.find { it.code == code }
    }
}

/**
 * This command writes Issuer Extra Data field and its issuer’s signature.
 * Issuer Extra Data is never changed or parsed from within the Tangem COS.
 * The issuer defines purpose of use, format and payload of Issuer Data.
 * For example, this field may contain a photo or biometric information for ID card products.
 * Because of the large size of Issuer_Extra_Data, a series of these commands have to be executed
 * to write entire Issuer_Extra_Data.
 * @param issuerData Data provided by issuer.
 * @param startingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerDataCounter] (if flags Protect_Issuer_Data_Against_Replay and
 * Restrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]) and size of [issuerData].
 * @param finalizingSignature Issuer’s signature with Issuer Data Private Key of [cardId],
 * [issuerData] and [issuerDataCounter] (the latter one only if flags Protect_Issuer_Data_Against_Replay
 * andRestrict_Overwrite_Issuer_Extra_Data are set in [SettingsMask]).
 * @param issuerDataCounter An optional counter that protect issuer data against replay attack.
 */
class WriteIssuerExtraDataCommand(
        private val issuerData: ByteArray,
        private val startingSignature: ByteArray,
        private val finalizingSignature: ByteArray,
        private val issuerDataCounter: Int? = null,
        verifier: IssuerDataVerifier = DefaultIssuerDataVerifier()
) : CommandSerializer<WriteIssuerDataResponse>(), IssuerDataVerifier by verifier {

    var mode: IssuerDataMode = IssuerDataMode.ExtraData
    var offset: Int = 0

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()

        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.CardId, cardEnvironment.cardId)
        tlvBuilder.append(TlvTag.Mode, mode)

        when (mode) {
            IssuerDataMode.ExtraData -> {
                tlvBuilder.append(TlvTag.Size, issuerData.size)
                tlvBuilder.append(TlvTag.IssuerDataSignature, startingSignature)
                tlvBuilder.append(TlvTag.IssuerDataCounter, issuerDataCounter)
            }
            IssuerDataMode.WriteExtraData -> {
                tlvBuilder.append(TlvTag.IssuerData, getDataToWrite())
                tlvBuilder.append(TlvTag.Offset, offset)
            }
            IssuerDataMode.FinalizeExtraData -> {
                tlvBuilder.append(TlvTag.IssuerDataSignature, finalizingSignature)
            }
        }
        return CommandApdu(Instruction.WriteIssuerData, tlvBuilder.serialize())
    }

    private fun getDataToWrite(): ByteArray =
            issuerData.copyOfRange(offset, offset + calculatePartSize())

    private fun calculatePartSize(): Int {
        val bytesLeft = issuerData.size - offset
        return if (bytesLeft < SINGLE_WRITE_SIZE) bytesLeft else SINGLE_WRITE_SIZE
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): WriteIssuerDataResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            WriteIssuerDataResponse(
                    cardId = mapper.map(TlvTag.CardId)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }

    companion object {
        const val SINGLE_WRITE_SIZE = 1524
    }
}