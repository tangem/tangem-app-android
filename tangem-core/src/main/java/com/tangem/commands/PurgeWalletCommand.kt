package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

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
 * the card changes state to ‘Empty’ and a new wallet can be created by CREATE_WALLET command.
 * If Is_Reusable flag is disabled, the card switches to ‘Purged’ state.
 * ‘Purged’ state is final, it makes the card useless.
 * @property cardId CID, Unique Tangem card ID number.
 * @property cvc Optional 3-digit code printed on the card. Required if Use_CVC flag is set in Settings_Mask.
 */
class PurgeWalletCommand(
        private val cardId: String
) : CommandSerializer<PurgeWalletResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvData = mutableListOf(
                Tlv(TlvTag.Pin, cardEnvironment.pin1.calculateSha256()),
                Tlv(TlvTag.CardId, cardId.hexToBytes()),
                Tlv(TlvTag.Pin2, cardEnvironment.pin2.calculateSha256())
        )
        return CommandApdu(Instruction.PurgeWallet, tlvData)
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): PurgeWalletResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            PurgeWalletResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    status = mapper.map(TlvTag.Status))
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}