package com.tangem.commands.personalization

import com.tangem.commands.CommandResponse
import com.tangem.commands.CommandSerializer
import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu

data class DepersonalizeResponse(val success: Boolean) : CommandResponse

/**
 * Command available on SDK cards only
 *
 * This command resets card to initial state,
 * erasing all data written during personalization and usage.
 * @param cardId CID, Unique Tangem card ID number.
 */
class DepersonalizeCommand : CommandSerializer<DepersonalizeResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        return CommandApdu(
                Instruction.Depersonalize, byteArrayOf()
        )
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): DepersonalizeResponse? {
        return DepersonalizeResponse(true)
    }
}