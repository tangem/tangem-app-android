package com.tangem.commands

import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

class PurgeWalletResponse(
        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String,
        /**
         * Current status of the card [1 - Empty, 2 - Loaded, 3- Purged]
         */
        val status: CardStatus
) : CommandResponse

/**
 * This command deletes all wallet data. If Is_Reusable flag is enabled during personalization,

 * If Is_Reusable flag is disabled, the card switches to ‘Purged’ state.
 * ‘Purged’ state is final, it makes the card useless.
 * @property cardId CID, Unique Tangem card ID number.
 */
class PurgeWalletCommand : Command<PurgeWalletResponse>() {

    override val requiresPin2 = true

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.status == CardStatus.NotPersonalized) {
            return TangemSdkError.NotPersonalized()
        }
        if (card.isActivated) {
            return TangemSdkError.NotActivated()
        }
        if (card.settingsMask?.contains(Settings.ProhibitPurgeWallet) == true) {
            return TangemSdkError.PurgeWalletProhibited()
        }

        return when (card.status) {
            CardStatus.Loaded -> null
            CardStatus.NotPersonalized -> TangemSdkError.NotPersonalized()
            CardStatus.Empty -> TangemSdkError.CardIsEmpty()
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
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2)
        return CommandApdu(Instruction.PurgeWallet, tlvBuilder.serialize())
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): PurgeWalletResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return PurgeWalletResponse(
                cardId = decoder.decode(TlvTag.CardId),
                status = decoder.decode(TlvTag.Status))
    }
}