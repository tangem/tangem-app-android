package com.tangem.commands

import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.apdu.StatusWord
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

enum class SetPinStatus {
    PinsNotChanged,
    Pin1Changed,
    Pin2Changed,
    Pin3Changed,
    Pins12Changed,
    Pins13Changed,
    Pins23Changed,
    Pins123Changed,
    ;

    companion object {
        fun fromStatusWord(statusWord: StatusWord): SetPinStatus? {
            return when (statusWord) {
                StatusWord.ProcessCompleted -> PinsNotChanged
                StatusWord.Pin1Changed -> Pin1Changed
                StatusWord.Pin2Changed -> Pin2Changed
                StatusWord.Pins12Changed -> Pins12Changed
                StatusWord.Pin3Changed -> Pin3Changed
                StatusWord.Pins13Changed -> Pins13Changed
                StatusWord.Pins23Changed -> Pins23Changed
                StatusWord.Pins123Changed -> Pins123Changed
                else -> null
            }
        }
    }
}

class SetPinResponse(
    /**
     * CID, Unique Tangem card ID number.
     */
    val cardId: String,
    /**
     *
     */
    val status: SetPinStatus
) : CommandResponse


class SetPinCommand(
    private val newPin1: ByteArray = SessionEnvironment.DEFAULT_PIN.calculateSha256(),
    private val newPin2: ByteArray = SessionEnvironment.DEFAULT_PIN2.calculateSha256(),
    private val newPin3: ByteArray? = null
) : Command<SetPinResponse>() {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)
        tlvBuilder.append(TlvTag.NewPin, newPin1)
        tlvBuilder.append(TlvTag.NewPin2, newPin2)
        tlvBuilder.append(TlvTag.NewPin3, newPin3)
        return CommandApdu(Instruction.SetPin, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): SetPinResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val status = SetPinStatus.fromStatusWord(apdu.statusWord) ?: throw TangemSdkError.DecodingFailed()

        val decoder = TlvDecoder(tlvData)
        return SetPinResponse(
            cardId = decoder.decode(TlvTag.CardId),
            status = status
        )
    }
}