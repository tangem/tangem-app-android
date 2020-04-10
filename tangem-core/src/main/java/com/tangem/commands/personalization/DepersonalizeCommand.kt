package com.tangem.commands.personalization

import com.tangem.CardEnvironment
import com.tangem.commands.Command
import com.tangem.commands.CommandResponse
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu

data class DepersonalizeResponse(val success: Boolean) : CommandResponse

/**
 * Command available on SDK cards only
 *
 * This command resets card to initial state,
 * erasing all data written during personalization and usage.
 */
class DepersonalizeCommand : Command<DepersonalizeResponse>() {

    override fun serialize(environment: CardEnvironment): CommandApdu {
        return CommandApdu(
                Instruction.Depersonalize, byteArrayOf()
        )
    }

    override fun deserialize(environment: CardEnvironment, apdu: ResponseApdu): DepersonalizeResponse {
        return DepersonalizeResponse(true)
    }
}