package com.tangem.commands.personalization

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.commands.CardStatus
import com.tangem.commands.Command
import com.tangem.commands.CommandResponse
import com.tangem.common.CompletionResult
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

    override fun performPreCheck(session: CardSession, callback: (result: CompletionResult<DepersonalizeResponse>) -> Unit): Boolean {
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return true
        }
        if (session.environment.card?.firmwareVersion?.contains("SDK") == false) {
            callback(CompletionResult.Failure(TangemSdkError.CannotBeDepersonalized()))
            return true
        }
        return false
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        return CommandApdu(
                Instruction.Depersonalize, byteArrayOf()
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): DepersonalizeResponse {
        return DepersonalizeResponse(true)
    }
}