package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
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

    override fun handlePreRunErrors(session: CardSession, callback: (result: CompletionResult<PurgeWalletResponse>) -> Unit): Boolean {
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return true
        }
        if (session.environment.card?.isActivated == true) {
            callback(CompletionResult.Failure(TangemSdkError.NotActivated()))
            return true
        }
        if (session.environment.card?.settingsMask?.contains(Settings.ProhibitPurgeWallet) == true) {
            callback(CompletionResult.Failure(TangemSdkError.PurgeWalletProhibited()))
            return true
        }
        return false
    }

    override fun handleResponseErrors(session: CardSession,
                                      result: CompletionResult<PurgeWalletResponse>,
                                      callback: (result: CompletionResult<PurgeWalletResponse>) -> Unit): Boolean {
        when (result) {
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.InvalidParams) {
                    callback(CompletionResult.Failure(TangemSdkError.Pin2OrCvcRequired()))
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2)
        return CommandApdu(
                Instruction.PurgeWallet, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): PurgeWalletResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return PurgeWalletResponse(
                cardId = decoder.decode(TlvTag.CardId),
                status = decoder.decode(TlvTag.Status))
    }
}